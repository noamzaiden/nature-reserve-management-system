package com.noam.fleetcommand.requests;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReserveCreationRequestRepository extends JpaRepository<ReserveCreationRequest, Long> {

    @EntityGraph(attributePaths = {"requestedBy"})
    List<ReserveCreationRequest> findAllByOrderByCreatedAtDesc();

    @EntityGraph(attributePaths = {"requestedBy"})
    List<ReserveCreationRequest> findByRequestedByIdOrderByCreatedAtDesc(Long requestedById);
}
