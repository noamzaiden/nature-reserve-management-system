package com.noam.fleetcommand.reserves;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReservePoiTypeRepository extends JpaRepository<ReservePoiType, Long> {
    List<ReservePoiType> findByReserveIdOrderBySystemDefaultDescNameAsc(Long reserveId);
    Optional<ReservePoiType> findByIdAndReserveId(Long id, Long reserveId);
    Optional<ReservePoiType> findByReserveIdAndNameIgnoreCase(Long reserveId, String name);
    boolean existsByReserveId(Long reserveId);
}
