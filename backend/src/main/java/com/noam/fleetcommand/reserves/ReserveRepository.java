package com.noam.fleetcommand.reserves;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReserveRepository extends JpaRepository<Reserve, Long> {
    List<Reserve> findByManagerId(Long managerId);
    List<Reserve> findByManagerIsNotNullOrderByNameAsc();
    Optional<Reserve> findByName(String name);
    Optional<Reserve> findByNameIgnoreCase(String name);
    Optional<Reserve> findByOsmTypeAndOsmId(String osmType, String osmId);
    List<Reserve> findAllByOrderByNameAsc();
}
