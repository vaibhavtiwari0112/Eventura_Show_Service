package com.eventura.showservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public class ShowResponse {
    private UUID id;
    private UUID movieId;
    private UUID hallId;
    private OffsetDateTime startTime;
    private BigDecimal basePrice;
    private List<String> availableSeats;
    private List<String> bookedSeats; // ✅ added
    private OffsetDateTime endTime;

    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

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

    public List<String> getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(List<String> bookedSeats) { this.bookedSeats = bookedSeats; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }
}
