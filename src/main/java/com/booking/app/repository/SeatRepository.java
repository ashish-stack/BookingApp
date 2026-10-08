package com.booking.app.repository;

import com.booking.app.entity.Seat;
import com.booking.app.entity.SeatStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface SeatRepository extends JpaRepository<Seat, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    List<Seat> findByShowIdAndSeatNumberInOrderBySeatNumberAsc(
            UUID showId,
            Collection<String> seatNumbers
    );

    long countByShowIdAndStatus(
            UUID showId,
            SeatStatus status
    );
}