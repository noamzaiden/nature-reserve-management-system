package com.noam.fleetcommand.reserves.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class ReserveRequestDto {

    @NotBlank
    private String name;

    public ReserveRequestDto() {
    }

    public ReserveRequestDto(String name) {
        this.name = name;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ReserveRequestDto that = (ReserveRequestDto) o;
        return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name);
    }

    @Override
    public String toString() {
        return "ReserveRequestDto{" +
                "name='" + name + '\'' +
                '}';
    }
}
