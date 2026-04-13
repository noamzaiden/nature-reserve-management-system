package com.reserve.mobile;

public final class WeatherInfo {

    private final double temperatureCelsius;
    private final String condition;

    // Stores one weather snapshot (temperature + condition text).
    public WeatherInfo(double temperatureCelsius, String condition) {
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
