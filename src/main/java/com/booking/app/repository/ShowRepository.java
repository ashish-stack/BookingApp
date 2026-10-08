package com.booking.app.repository;

import com.booking.app.entity.Show;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShowRepository extends JpaRepository<Show, UUID> {

    @EntityGraph(attributePaths = "seats")
    Optional<Show> findWithSeatsById(UUID id);
}