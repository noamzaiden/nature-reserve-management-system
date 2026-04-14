package com.reserve.mobile;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public final class ReserveUtils {

    // Utility class: no instances.
    private ReserveUtils() {
    }

    // Finds the reserve that contains the given location.
    public static Reserve findReserveForLocation(List<Reserve> reserves, LatLng latLng) {
        for (Reserve reserve : reserves) {
            AreaBounds areaBounds = reserve.getAreaBounds();
            if (areaBounds != null && areaBounds.contains(latLng.latitude, latLng.longitude)) {
                return reserve;
            }
        }
        return null;
    }


    // Counts hazards visible for a specific reserve or for all reserves when null.
    public static int countVisibleHazards(List<PublicEvent> hazards, Reserve reserve) {
        int count = 0;
        for (PublicEvent event : hazards) {
            if (event.hasCoordinates() && (reserve == null || event.getReserveId() == reserve.getId())) {
                count++;
            }
        }
        return count;
    }
}
