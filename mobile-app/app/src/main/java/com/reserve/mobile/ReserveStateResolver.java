package com.reserve.mobile;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

final class ReserveStateResolver {

    // Computes the current reserve state from location, reserve list, and hazards.
    ReserveState resolve(LatLng currentUserLatLng, List<Reserve> reserves, List<Event> hazards) {
        if (currentUserLatLng == null) {
            return ReserveState.noLocation(countVisibleHazards(null, hazards));
        }

        Reserve activeReserve = findReserveForLocation(currentUserLatLng, reserves);
        if (activeReserve != null) {
            return ReserveState.insideReserve(activeReserve, countVisibleHazards(activeReserve, hazards));
        }

        return ReserveState.outsideReserve(countVisibleHazards(null, hazards));
    }

    private Reserve findReserveForLocation(LatLng latLng, List<Reserve> reserves) {
        for (Reserve reserve : reserves) {
            AreaBounds areaBounds = reserve.getAreaBounds();
            if (areaBounds != null && areaBounds.contains(latLng.latitude, latLng.longitude)) {
                return reserve;
            }
        }
        return null;
    }

    private int countVisibleHazards(Reserve reserve, List<Event> hazards) {
        int count = 0;
        for (Event event : hazards) {
            if (event.hasCoordinates() && (reserve == null || event.getReserveId() == reserve.getId())) {
                count++;
            }
        }
        return count;
    }
}
