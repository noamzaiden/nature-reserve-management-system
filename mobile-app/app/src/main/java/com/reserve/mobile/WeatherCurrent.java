package com.reserve.mobile;

public final class WeatherCurrent {

    private final double temperatureCelsius;
    private final String condition;

    // Stores one weather snapshot (temperature + condition text).
    public WeatherCurrent(double temperatureCelsius, String condition) {
        this.temperatureCelsius = temperatureCelsius;
        this.condition = condition;
    }

    // Returns temperature in Celsius.
    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    // Returns weather condition text shown in UI.
    public String getCondition() {
        return condition;
    }
}

