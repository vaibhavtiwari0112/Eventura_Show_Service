package com.eventura.showservice.dto;


import java.util.List;
import java.util.UUID;

public class SeatUpdateRequest {
    public UUID getShowId() {
        return showId;
    }

    public void setShowId(UUID showId) {
        this.showId = showId;
    }

    private UUID showId;

    public UUID getHallId() {
        return hallId;
    }

    public void setHallId(UUID hallId) {
        this.hallId = hallId;
    }

    public List<String> getSeats() {
        return seats;
    }

    public void setSeats(List<String> seats) {
        this.seats = seats;
    }

    private UUID hallId;
    private List<String> seats;
}
