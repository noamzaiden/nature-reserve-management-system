package com.noam.fleetcommand.events.mapper;

import com.noam.fleetcommand.events.Event;
import com.noam.fleetcommand.events.EventMedia;
import com.noam.fleetcommand.events.dto.EventRequestDto;
import com.noam.fleetcommand.events.dto.EventMediaDto;
import com.noam.fleetcommand.events.dto.EventResponseDto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface EventMapper {

    @Mapping(source = "reserve.id", target = "reserveId")
    @Mapping(source = "reserve.name", target = "reserveName")
    @Mapping(source = "assignedUser.id", target = "assignedUserId")
    EventResponseDto toDto(Event event);

    EventMediaDto toMediaDto(EventMedia media);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    @Mapping(target = "closedAt", ignore = true)
    @Mapping(target = "reserve", ignore = true)
    @Mapping(target = "assignedUser", ignore = true)
    @Mapping(target = "media", ignore = true)
    @Mapping(target = "origin", ignore = true)
    @Mapping(target = "reporterName", ignore = true)
    Event toEntity(EventRequestDto dto);
}
