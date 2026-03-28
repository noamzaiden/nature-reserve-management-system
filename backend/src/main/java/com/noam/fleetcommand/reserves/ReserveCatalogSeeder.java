package com.noam.fleetcommand.reserves;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Component
public class ReserveCatalogSeeder implements ApplicationRunner {

    private final ReserveRepository reserveRepository;
    private final IsraeliNatureReserveLookupService israeliNatureReserveLookupService;

    public ReserveCatalogSeeder(ReserveRepository reserveRepository,
                                IsraeliNatureReserveLookupService israeliNatureReserveLookupService) {
        this.reserveRepository = reserveRepository;
        this.israeliNatureReserveLookupService = israeliNatureReserveLookupService;
    }

    @Transactional
    public void run(ApplicationArguments args) {
        Set<String> existingNames = new HashSet<>();
        for (Reserve reserve : reserveRepository.findAll()) {
            existingNames.add(reserve.getName().toLowerCase());
        }

        for (IsraeliNatureReserveLookupService.ResolvedNatureReserve catalogReserve : israeliNatureReserveLookupService.getCatalog()) {
            if (existingNames.contains(catalogReserve.officialName().toLowerCase())) {
                continue;
            }

            Reserve reserve = new Reserve(catalogReserve.officialName(), catalogReserve.region(), catalogReserve.area());
            reserve.setDisplayName(catalogReserve.officialName());
            reserve.setCenterLatitude((catalogReserve.area().getMinLatitude() + catalogReserve.area().getMaxLatitude()) / 2.0);
            reserve.setCenterLongitude((catalogReserve.area().getMinLongitude() + catalogReserve.area().getMaxLongitude()) / 2.0);
            reserveRepository.save(reserve);
        }
    }
}
