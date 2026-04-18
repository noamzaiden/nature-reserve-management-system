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

    private ReserveState(Kind kind, Reserve activeReserve, int visibleHazardCount) {
        this.kind = kind;
        this.activeReserve = activeReserve;
        this.visibleHazardCount = visibleHazardCount;
    }

    static ReserveState noLocation(int visibleHazardCount) {
        return new ReserveState(Kind.NO_LOCATION, null, visibleHazardCount);
    }

    static ReserveState insideReserve(Reserve reserve, int visibleHazardCount) {
        return new ReserveState(Kind.INSIDE_RESERVE, reserve, visibleHazardCount);
    }

    static ReserveState outsideReserve(int visibleHazardCount) {
        return new ReserveState(Kind.OUTSIDE_RESERVE, null, visibleHazardCount);
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

    boolean hasActiveReserve() {
        return activeReserve != null;
    }

    boolean isNoLocation() {
        return kind == Kind.NO_LOCATION;
    }
}
