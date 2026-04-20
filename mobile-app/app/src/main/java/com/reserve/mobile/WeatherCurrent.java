package com.reserve.mobile;

public final class WeatherCurrent {

    private final double temperatureCelsius;
    private final String condition;

    public WeatherCurrent(double temperatureCelsius, String condition) {
        this.temperatureCelsius = temperatureCelsius;
        this.condition = condition;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public String getCondition() {
        return condition;
    }
}

