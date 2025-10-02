package com.eventura.showservice.dto;

import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ShowMiniDTO {
    private UUID id;
    private String movieTitle;
    private String hallName;
    private String location;
    private OffsetDateTime time;
}
