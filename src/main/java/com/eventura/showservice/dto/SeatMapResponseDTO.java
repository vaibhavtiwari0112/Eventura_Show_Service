package com.eventura.showservice.dto;

import java.util.List;
import java.util.UUID;

public class SeatMapResponseDTO {
    private UUID showId;
    private UUID hallId;
    private UUID movieId;
    private List<String> availableSeats;
    private List<String> bookedSeats;

    // getters/setters
    public UUID getShowId() { return showId; }
    public void setShowId(UUID showId) { this.showId = showId; }

    public UUID getHallId() { return hallId; }
    public void setHallId(UUID hallId) { this.hallId = hallId; }

    public UUID getMovieId() { return movieId; }
    public void setMovieId(UUID movieId) { this.movieId = movieId; }

    public List<String> getAvailableSeats() { return availableSeats; }
    public void setAvailableSeats(List<String> availableSeats) { this.availableSeats = availableSeats; }

    public List<String> getBookedSeats() { return bookedSeats; }
    public void setBookedSeats(List<String> bookedSeats) { this.bookedSeats = bookedSeats; }
}
