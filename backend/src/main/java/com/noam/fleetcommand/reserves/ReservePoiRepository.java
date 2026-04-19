package com.noam.fleetcommand.reserves;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservePoiRepository extends JpaRepository<ReservePoi, Long> {
    List<ReservePoi> findByReserveIdOrderByNameAsc(Long reserveId);
    Optional<ReservePoi> findByIdAndReserveId(Long id, Long reserveId);
    boolean existsByTypeId(Long typeId);
}
