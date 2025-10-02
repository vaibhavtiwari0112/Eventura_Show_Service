package com.eventura.showservice.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.UUID;

public class TheatreDTO {

    @JsonProperty("id")
    private UUID theatreId;

    @JsonProperty("name")
    private String theatreName;

    @JsonProperty("address")
    private String address;

    @JsonProperty("city")
    private String city;

    // getters/setters
    public UUID getTheatreId() { return theatreId; }
    public void setTheatreId(UUID theatreId) { this.theatreId = theatreId; }

    public String getTheatreName() { return theatreName; }
    public void setTheatreName(String theatreName) { this.theatreName = theatreName; }

    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }

    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
}
