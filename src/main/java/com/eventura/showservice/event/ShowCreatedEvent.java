package com.eventura.showservice.event;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ShowCreatedEvent {
    private UUID id;
    private UUID movieId;
    private UUID hallId;
    private OffsetDateTime startTime;
    private BigDecimal basePrice;
    private OffsetDateTime endTime;

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public ShowCreatedEvent() {}

    public ShowCreatedEvent(UUID id, UUID movieId, UUID hallId, OffsetDateTime startTime, BigDecimal basePrice,OffsetDateTime endTime) {
        this.id = id;
        this.movieId = movieId;
        this.hallId = hallId;
        this.startTime = startTime;
        this.basePrice = basePrice;
        this.endTime = endTime;
    }

    // getters & setters
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
}
