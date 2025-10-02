package com.eventura.showservice.dto;

import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class CreateShowRequest {
    @NotNull
    private UUID movieId;

    @NotNull
    private UUID hallId;

    @NotNull
    private OffsetDateTime startTime;

    @NotNull
    private BigDecimal basePrice;

    @NotNull
    private OffsetDateTime endTime;

    private List<String> availableSeats;

    private List<String> bookedSeats;

    public List<String> getLockedSeats() {
        return lockedSeats;
    }

    public void setLockedSeats(List<String> lockedSeats) {
        this.lockedSeats = lockedSeats;
    }

    private List<String> lockedSeats;


    // Getters and setters
    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public UUID getMovieId() { return movieId; }
    public void setMovieId(UUID movieId) { this.movieId = movieId; }

    public UUID getHallId() { return hallId; }
    public void setHallId(UUID hallId) { this.hallId = hallId; }

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public List<String> getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(List<String> availableSeats) { this.availableSeats = availableSeats; }

    public List<String> getBookedSeats() {
        return bookedSeats;
    }

    public void setBookedSeats(List<String> bookedSeats) {
        this.bookedSeats = bookedSeats;
    }
}
