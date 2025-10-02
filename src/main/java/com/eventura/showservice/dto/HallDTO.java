package com.eventura.showservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class HallDTO {
    @JsonProperty("id")
    private UUID hallId;

    @JsonProperty("name")
    private String hallName;

    @JsonProperty("capacity")
    private int capacity;

    @JsonProperty("theaterId") // assuming your Hall controller flattens Theater into theaterId
    private UUID theatreId;
    // getters/setters
    public UUID getHallId() { return hallId; }
    public void setHallId(UUID hallId) { this.hallId = hallId; }

    public String getHallName() { return hallName; }
    public void setHallName(String hallName) { this.hallName = hallName; }

    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }

    public UUID getTheatreId() {
        return theatreId;
    }

    public void setTheatreId(UUID theatreId) {
        this.theatreId = theatreId;
    }
}
