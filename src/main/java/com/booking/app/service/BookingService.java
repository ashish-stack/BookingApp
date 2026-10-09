package com.booking.app.service;

import com.booking.app.dto.ReservationResponse;
import com.booking.app.dto.ReserveRequest;
import com.booking.app.entity.*;
import com.booking.app.exception.BookingException;
import com.booking.app.exception.NotFoundException;
import com.booking.app.repository.AppUserRepository;
import com.booking.app.repository.ReservationRepository;
import com.booking.app.repository.SeatRepository;
import com.booking.app.repository.ShowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class BookingService {

    private final ShowRepository showRepository;
    private final SeatRepository seatRepository;
    private final ReservationRepository reservationRepository;
    private final AppUserRepository appUserRepository;
    private final MetricsService metricsService;

    public BookingService(
            ShowRepository showRepository,
            SeatRepository seatRepository,
            ReservationRepository reservationRepository,
            AppUserRepository appUserRepository,
            MetricsService metricsService
    ) {
        this.showRepository = showRepository;
        this.seatRepository = seatRepository;
        this.reservationRepository = reservationRepository;
        this.appUserRepository = appUserRepository;
        this.metricsService = metricsService;
    }

    @Transactional
    public ReservationResult reserve(
            UUID showId,
            String userId,
            String idempotencyKey,
            ReserveRequest request
    ) {

        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new BookingException(
                    HttpStatus.BAD_REQUEST,
                    "idempotency_key_required"
            );
        }

        List<String> requestedSeats = normalizeSeats(request.seats());

        if (requestedSeats.isEmpty()) {
            throw new BookingException(
                    HttpStatus.BAD_REQUEST,
                    "at_least_one_seat_required"
            );
        }

        String requestHash = hashSeats(requestedSeats);

        /*
         * STEP 1:
         *
         * Make sure the per-user lock row exists.
         */
        appUserRepository.ensureExists(userId);

        /*
         * STEP 2:
         *
         * Lock the user row.
         *
         * This makes the per-user booking limit safe when the
         * same user sends multiple concurrent requests.
         */
        appUserRepository.findLockedByUserId(userId)
                .orElseThrow(() ->
                        new BookingException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "user_lock_row_missing"
                        )
                );

        /*
         * STEP 3:
         *
         * Idempotency lookup happens while the user's lock is held.
         */
        Optional<Reservation> existing =
                reservationRepository
                        .findByShowIdAndUserIdAndIdempotencyKey(
                                showId,
                                userId,
                                idempotencyKey
                        );

        if (existing.isPresent()) {

            Reservation reservation = existing.get();

            if (!reservation.getRequestHash().equals(requestHash)) {

                throw new BookingException(
                        HttpStatus.CONFLICT,
                        "idempotency_key_reused_with_different_request"
                );
            }

            metricsService.recordIdempotentReplay();

            return new ReservationResult(
                    toResponse(reservation),
                    true
            );
        }

        /*
         * STEP 4:
         *
         * Load show.
         */
        Show show = showRepository.findById(showId)
                .orElseThrow(() ->
                        new NotFoundException("show_not_found")
                );

        /*
         * STEP 5:
         *
         * Validate requested seat count before touching seats.
         */
        if (requestedSeats.size() > show.getPerUserLimit()) {

            metricsService.recordDecline("per-user-limit");

            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "per_user_limit_exceeded"
            );
        }

        /*
         * STEP 6:
         *
         * Lock all requested seats in deterministic order.
         *
         * This is the critical concurrency operation.
         */
        List<Seat> seats =
                seatRepository
                        .findByShowIdAndSeatNumberInOrderBySeatNumberAsc(
                                showId,
                                requestedSeats
                        );

        /*
         * If the number returned is smaller than requested,
         * one or more seat numbers do not exist.
         */
        if (seats.size() != requestedSeats.size()) {

            metricsService.recordDecline("seat-not-found");

            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "one_or_more_seats_do_not_exist"
            );
        }

        /*
         * STEP 7:
         *
         * Because the seat rows are now locked, this state check
         * is race-free.
         */
        for (Seat seat : seats) {

            if (seat.getStatus() != SeatStatus.AVAILABLE) {

                metricsService.recordDecline("seat-taken");

                throw new BookingException(
                        HttpStatus.CONFLICT,
                        "seat_taken:" + seat.getSeatNumber()
                );
            }
        }

        /*
         * STEP 8:
         *
         * User row is locked, so this count cannot race with
         * another booking from the same user.
         */
        long existingSeatCount =
                reservationRepository.countConfirmedSeatsForUser(
                        showId,
                        userId,
                        ReservationStatus.CONFIRMED
                );

        if (existingSeatCount + requestedSeats.size()
                > show.getPerUserLimit()) {

            metricsService.recordDecline("per-user-limit");

            throw new BookingException(
                    HttpStatus.CONFLICT,
                    "per_user_limit_exceeded"
            );
        }

        /*
         * STEP 9:
         *
         * Create reservation.
         */
        long amount =
                Math.multiplyExact(
                        show.getPricePaise(),
                        requestedSeats.size()
                );

        Reservation reservation =
                new Reservation(
                        show,
                        userId,
                        idempotencyKey,
                        requestHash,
                        amount
                );

        reservationRepository.save(reservation);

        /*
         * STEP 10:
         *
         * Change the already locked seats.
         */
        for (Seat seat : seats) {

            seat.setStatus(SeatStatus.CONFIRMED);
            seat.setReservation(reservation);
        }

        seatRepository.saveAll(seats);

        metricsService.recordConfirmed();

        return new ReservationResult(
                toResponse(reservation, requestedSeats),
                false
        );
    }

    @Transactional
    public void cancel(
            UUID reservationId,
            String userId
    ) {

        Reservation reservation =
                reservationRepository.findById(reservationId)
                        .orElseThrow(() ->
                                new NotFoundException(
                                        "reservation_not_found"
                                )
                        );

        if (!reservation.getUserId().equals(userId)) {
            throw new BookingException(
                    HttpStatus.FORBIDDEN,
                    "reservation_does_not_belong_to_user"
            );
        }

        if (reservation.getStatus() == ReservationStatus.CANCELLED) {
            return;
        }

        /*
         * Lock user first.
         */
        appUserRepository.ensureExists(userId);

        appUserRepository.findLockedByUserId(userId)
                .orElseThrow(() ->
                        new BookingException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "user_lock_row_missing"
                        )
                );

        /*
         * Lock seats before releasing them.
         */
        List<Seat> seats =
                seatRepository
                        .findByShowIdAndSeatNumberInOrderBySeatNumberAsc(
                                reservation.getShow().getId(),
                                reservation.getShow().getSeats()
                                        .stream()
                                        .filter(s ->
                                                reservation.equals(
                                                        s.getReservation()))
                                        .map(Seat::getSeatNumber)
                                        .sorted()
                                        .toList()
                        );

        for (Seat seat : seats) {

            if (reservation.equals(seat.getReservation())) {
                seat.setReservation(null);
                seat.setStatus(SeatStatus.AVAILABLE);
            }
        }

        reservation.cancel();

        seatRepository.saveAll(seats);
        reservationRepository.save(reservation);
    }

    private List<String> normalizeSeats(List<String> seats) {

        if (seats == null) {
            return List.of();
        }

        return seats.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .map(String::toUpperCase)
                .filter(s -> !s.isBlank())
                .distinct()
                .sorted()
                .toList();
    }

    private String hashSeats(List<String> seats) {

        try {

            String input = String.join("|", seats);

            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hash =
                    digest.digest(
                            input.getBytes(StandardCharsets.UTF_8)
                    );

            StringBuilder result = new StringBuilder();

            for (byte b : hash) {
                result.append(String.format("%02x", b));
            }

            return result.toString();

        } catch (Exception e) {
            throw new IllegalStateException(
                    "unable_to_hash_request",
                    e
            );
        }
    }

    private ReservationResponse toResponse(
            Reservation reservation,
            List<String> seats
    ) {

        return new ReservationResponse(
                reservation.getId(),
                reservation.getShow().getId(),
                reservation.getUserId(),
                seats,
                reservation.getAmountPaise(),
                reservation.getStatus().name().toLowerCase()
        );
    }

    private ReservationResponse toResponse(
            Reservation reservation
    ) {

        List<String> seats =
                reservation.getShow().getSeats()
                        .stream()
                        .filter(s ->
                                reservation.equals(
                                        s.getReservation()))
                        .map(Seat::getSeatNumber)
                        .sorted()
                        .toList();

        return toResponse(reservation, seats);
    }

    public record ReservationResult(
            ReservationResponse response,
            boolean replay
    ) {
    }
}
