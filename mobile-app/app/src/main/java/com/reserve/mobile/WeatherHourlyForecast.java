package com.reserve.mobile;

public final class WeatherHourlyForecast {

    private final String hourLabel;
    private final double temperatureCelsius;
    private final String condition;


    public WeatherHourlyForecast(String hourLabel, double temperatureCelsius, String condition) {
        this.hourLabel = hourLabel;
        this.temperatureCelsius = temperatureCelsius;
        this.condition = condition;
    }

    public String getHourLabel() {
        return hourLabel;
    }

    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public String getCondition() {
        return condition;
    }
}



