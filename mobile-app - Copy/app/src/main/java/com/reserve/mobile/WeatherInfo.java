package com.reserve.mobile;

public class WeatherInfo {

    private final double temperatureCelsius;
    private final String condition;

    public WeatherInfo(double temperatureCelsius, String condition) {
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
