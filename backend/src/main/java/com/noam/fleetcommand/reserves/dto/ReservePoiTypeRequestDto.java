package com.noam.fleetcommand.reserves.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ReservePoiTypeRequestDto {

    @NotBlank(message = "POI type name is required")
    @Size(max = 80, message = "POI type name must not exceed 80 characters")
    private String name;
}
