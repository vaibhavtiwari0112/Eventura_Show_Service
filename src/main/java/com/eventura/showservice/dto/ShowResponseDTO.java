package com.eventura.showservice.dto;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public class ShowResponseDTO {
    private UUID showId;
    private OffsetDateTime startTime;
    private OffsetDateTime endTime;
    private BigDecimal basePrice;

    private HallDTO hall;
    private TheatreDTO theatre;
    private String movieTitle;

    public String getMovieTitle() {
        return movieTitle;
    }

    public void setMovieTitle(String movieTitle) {
        this.movieTitle = movieTitle;
    }
    // getters and setters
    public UUID getShowId() { return showId; }
    public void setShowId(UUID showId) { this.showId = showId; }

    public OffsetDateTime getStartTime() { return startTime; }
    public void setStartTime(OffsetDateTime startTime) { this.startTime = startTime; }

    public OffsetDateTime getEndTime() { return endTime; }
    public void setEndTime(OffsetDateTime endTime) { this.endTime = endTime; }

    public BigDecimal getBasePrice() { return basePrice; }
    public void setBasePrice(BigDecimal basePrice) { this.basePrice = basePrice; }

    public HallDTO getHall() { return hall; }
    public void setHall(HallDTO hall) { this.hall = hall; }

    public TheatreDTO getTheatre() { return theatre; }
    public void setTheatre(TheatreDTO theatre) { this.theatre = theatre; }
}

