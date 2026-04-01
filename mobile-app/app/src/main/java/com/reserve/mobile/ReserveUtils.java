package com.reserve.mobile;

import android.location.Location;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

public final class ReserveUtils {

    private ReserveUtils() {
    }

    public static ReserveOption findReserveForLocation(List<ReserveOption> reserves, LatLng latLng) {
        for (ReserveOption reserve : reserves) {
            AreaBounds areaBounds = reserve.getAreaBounds();
            if (areaBounds != null && areaBounds.contains(latLng.latitude, latLng.longitude)) {
                return reserve;
            }
        }
        return null;
    }

    public static ReserveOption findNearestReserve(List<ReserveOption> reserves, LatLng latLng) {
        ReserveOption nearest = null;
        float nearestDistance = Float.MAX_VALUE;
        float[] results = new float[1];

        for (ReserveOption reserve : reserves) {
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

    public static int countVisibleHazards(List<PublicEvent> hazards, ReserveOption reserve) {
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
