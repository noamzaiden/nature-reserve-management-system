package com.reserve.mobile;

final class ReserveState {

    enum Kind {
        NO_LOCATION,
        INSIDE_RESERVE,
        OUTSIDE_RESERVE
    }

    private final Kind kind;
    private final Reserve activeReserve;
    private final int visibleHazardCount;
    private final boolean activeReserveHasFireHazard;

    private ReserveState(Kind kind, Reserve activeReserve, int visibleHazardCount,
                         boolean activeReserveHasFireHazard) {
        this.kind = kind;
        this.activeReserve = activeReserve;
        this.visibleHazardCount = visibleHazardCount;
        this.activeReserveHasFireHazard = activeReserveHasFireHazard;
    }

    static ReserveState noLocation(int visibleHazardCount) {
        return new ReserveState(Kind.NO_LOCATION, null, visibleHazardCount, false);
    }

    static ReserveState insideReserve(Reserve reserve, int visibleHazardCount,
                                      boolean activeReserveHasFireHazard) {
        return new ReserveState(Kind.INSIDE_RESERVE, reserve, visibleHazardCount, activeReserveHasFireHazard);
    }

    static ReserveState outsideReserve(int visibleHazardCount) {
        return new ReserveState(Kind.OUTSIDE_RESERVE, null, visibleHazardCount, false);
    }

    Kind getKind() {
        return kind;
    }

    Reserve getActiveReserve() {
        return activeReserve;
    }

    int getVisibleHazardCount() {
        return visibleHazardCount;
    }

    boolean hasActiveReserveFireHazard() {
        return activeReserveHasFireHazard;
    }

    boolean hasActiveReserve() {
        return activeReserve != null;
    }

    boolean isNoLocation() {
        return kind == Kind.NO_LOCATION;
    }
}
