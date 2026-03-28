package com.noam.fleetcommand.events;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EventRepository extends JpaRepository<Event, Long> {
    @EntityGraph(attributePaths = {"reserve", "assignedUser", "media"})
    List<Event> findByReserveId(Long reserveId);

    @EntityGraph(attributePaths = {"reserve", "assignedUser", "media"})
    List<Event> findByReserveManagerId(Long managerId);

    @EntityGraph(attributePaths = {"reserve", "assignedUser", "media"})
    List<Event> findByReserveIdAndPublishedToTravelersTrueAndStatusNot(Long reserveId, EventStatus status);

    @Override
    @EntityGraph(attributePaths = {"reserve", "assignedUser", "media"})
    Optional<Event> findById(Long id);
}
