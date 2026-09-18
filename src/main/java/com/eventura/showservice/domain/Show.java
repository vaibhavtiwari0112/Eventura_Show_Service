package com.eventura.showservice.domain;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "shows")
public class Show {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;  // UUID set manually in service

    @Column(name = "movie_id", nullable = false)
    private UUID movieId;

    @Column(name = "hall_id", nullable = false)
    private UUID hallId;

    @Column(name = "start_time", nullable = false)
    private OffsetDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private OffsetDateTime endTime;

    @Column(name = "base_price", nullable = false)
    private BigDecimal basePrice;

    
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "show_available_seats",
            joinColumns = @JoinColumn(name = "show_id")
    )
    @Column(name = "seat_id", nullable = false)
    private List<String> availableSeats = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(
            name = "show_booked_seats",
            joinColumns = @JoinColumn(name = "show_id")
    )
    @Column(name = "seat_id", nullable = false)
    private List<String> bookedSeats = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "show_locked_seats", joinColumns = @JoinColumn(name = "show_id"))
    @Column(name = "seat_id")
    private List<String> lockedSeats = new ArrayList<>();

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version;

    public Show() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public UUID getMovieId() { return movieId; }
    public void setMovieId(UUID movieId) { this.movieId = movieId; }

    public UUID getHallId() { return hallId; }
    public void setHallId(UUID hallId) { this.hallId = hallId; }

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public List<String> getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(List<String> availableSeats) { this.availableSeats = availableSeats; }

    public List<String> getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(List<String> bookedSeats) { this.bookedSeats = bookedSeats; }

    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }

    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }

    public List<String> getLockedSeats() {
        return lockedSeats;
    }

    public void setLockedSeats(List<String> lockedSeats) {
        this.lockedSeats = lockedSeats;
    }
}
