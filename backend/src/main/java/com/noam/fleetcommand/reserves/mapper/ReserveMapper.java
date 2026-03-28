package com.noam.fleetcommand.reserves.mapper;

import com.noam.fleetcommand.reserves.Area;
import com.noam.fleetcommand.reserves.Reserve;
import com.noam.fleetcommand.reserves.dto.AreaDto;
import com.noam.fleetcommand.reserves.dto.ReserveResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface ReserveMapper {

    @Mapping(source = "manager.id", target = "managerUserId")
    ReserveResponseDto toResponseDto(Reserve reserve);

    AreaDto toAreaDto(Area area);

    Area toAreaEntity(AreaDto dto);
}
