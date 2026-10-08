package com.booking.app.repository;

import com.booking.app.entity.Reservation;
import com.booking.app.entity.ReservationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Optional;
import java.util.UUID;

public interface ReservationRepository
        extends JpaRepository<Reservation, UUID> {

    Optional<Reservation> findByShowIdAndUserIdAndIdempotencyKey(
            UUID showId,
            String userId,
            String idempotencyKey
    );

    @Query("""
            select count(s)
            from Seat s
            join s.reservation r
            where r.show.id = :showId
              and r.userId = :userId
              and r.status = :status
              and s.status = 'CONFIRMED'
            """)
    long countConfirmedSeatsForUser(
            UUID showId,
            String userId,
            ReservationStatus status
    );
}