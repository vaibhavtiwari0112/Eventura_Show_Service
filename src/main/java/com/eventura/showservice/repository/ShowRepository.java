package com.eventura.showservice.repository;

import com.eventura.showservice.domain.Show;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface ShowRepository extends JpaRepository<Show, UUID> {
    List<Show> findByMovieId(UUID movieId);
    boolean existsByMovieIdAndHallIdAndStartTimeLessThanEqualAndEndTimeGreaterThanEqual(
            UUID movieId, UUID hallId, OffsetDateTime endTime, OffsetDateTime startTime);

    List<Show> findByMovieIdAndHallId(UUID movieId, UUID hallId);

    Optional<Show> findByMovieIdAndHallIdAndStartTime(UUID movieId, UUID hallId, OffsetDateTime time);

        // overlap check in same hall (true if an existing show overlaps with [startTime, endTime))
        boolean existsByHallIdAndStartTimeLessThanAndEndTimeGreaterThan(UUID hallId, OffsetDateTime endTime, OffsetDateTime startTime);

        // for convenience, fetch shows for a hall in a date range (useful for batch checks)
        List<Show> findByHallIdAndStartTimeBetween(UUID hallId, OffsetDateTime from, OffsetDateTime to);

}
