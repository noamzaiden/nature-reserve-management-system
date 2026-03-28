package com.noam.fleetcommand.requests.dto;

import jakarta.validation.constraints.NotBlank;

public class ReserveCreationRequestCreateDto {

    @NotBlank
    private String reserveName;

    @NotBlank
    private String message;

    public String getReserveName() {
        return reserveName;
    }

    public void setReserveName(String reserveName) {
        this.reserveName = reserveName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
