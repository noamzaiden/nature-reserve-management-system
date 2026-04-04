package com.reserve.mobile;

import android.location.Location;

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

    // Finds the nearest reserve center to the given location.
    public static Reserve findNearestReserve(List<Reserve> reserves, LatLng latLng) {
        Reserve nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        float[] results = new float[1];

        for (Reserve reserve : reserves) {
            if (!reserve.hasCenterPoint()) {
                continue;
            }

            Location.distanceBetween(
                    latLng.latitude,
                    latLng.longitude,
                    reserve.getCenterLatitude(),
                    reserve.getCenterLongitude(),
                    results
            );

            if (results[0] < nearestDistance) {
                nearestDistance = results[0];
                nearest = reserve;
            }
        }

        return nearest;
    }

    // Counts hazards visible for a specific reserve or for all reserves when null.
    public static int countVisibleHazards(List<PublicEvent> hazards, Reserve reserve) {
        int count = 0;
        for (PublicEvent event : hazards) {
            if (!event.hasCoordinates()) {
                continue;
            }
            if (reserve == null || event.getReserveId() == reserve.getId()) {
                count++;
            }
        }
        return count;
    }
}
