package com.booking.app.service;

import com.booking.app.dto.CreateShowRequest;
import com.booking.app.dto.ShowResponse;
import com.booking.app.entity.Seat;
import com.booking.app.entity.SeatStatus;
import com.booking.app.entity.Show;
import com.booking.app.exception.BookingException;
import com.booking.app.exception.NotFoundException;
import com.booking.app.repository.ShowRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Service
public class ShowService {

    private final ShowRepository showRepository;

    public ShowService(ShowRepository showRepository) {
        this.showRepository = showRepository;
    }

    @Transactional
    public ShowResponse createShow(CreateShowRequest request) {

        List<String> seats = request.seats()
                .stream()
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        Set<String> uniqueSeats = new HashSet<>(seats);

        if (uniqueSeats.size() != seats.size()) {
            throw new BookingException(
                    HttpStatus.BAD_REQUEST,
                    "duplicate_seat_number"
            );
        }

        int limit = request.per_user_limit() == null
                ? 4
                : request.per_user_limit();

        if (limit > seats.size()) {
            limit = seats.size();
        }

        Show show = new Show(
                request.name(),
                request.price_paise(),
                limit
        );

        for (String seatNumber : seats) {
            show.addSeat(new Seat(seatNumber));
        }

        Show saved = showRepository.save(show);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    public ShowResponse getShow(UUID showId) {

        Show show = showRepository.findWithSeatsById(showId)
                .orElseThrow(() ->
                        new NotFoundException("show_not_found")
                );

        return toResponse(show);
    }

    private ShowResponse toResponse(Show show) {

        int available = 0;
        int held = 0;
        int confirmed = 0;

        List<ShowResponse.SeatResponse> seats =
                show.getSeats()
                        .stream()
                        .sorted((a, b) ->
                                a.getSeatNumber()
                                        .compareTo(b.getSeatNumber()))
                        .map(seat -> {
                            return new ShowResponse.SeatResponse(
                                    seat.getSeatNumber(),
                                    seat.getStatus().name().toLowerCase()
                            );
                        })
                        .toList();

        for (Seat seat : show.getSeats()) {
            if (seat.getStatus() == SeatStatus.AVAILABLE) {
                available++;
            } else if (seat.getStatus() == SeatStatus.HELD) {
                held++;
            } else if (seat.getStatus() == SeatStatus.CONFIRMED) {
                confirmed++;
            }
        }

        return new ShowResponse(
                show.getId(),
                show.getName(),
                show.getPricePaise(),
                show.getPerUserLimit(),
                show.getSeats().size(),
                available,
                held,
                confirmed,
                seats
        );
    }
}
