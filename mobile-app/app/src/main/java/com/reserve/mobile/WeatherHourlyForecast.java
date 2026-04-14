package com.reserve.mobile;

public final class WeatherHourlyForecast {

    private final String hourLabel;
    private final double temperatureCelsius;
    private final String condition;

    // Stores one upcoming hourly weather point for compact UI display.
    public WeatherHourlyForecast(String hourLabel, double temperatureCelsius, String condition) {
        this.hourLabel = hourLabel;
        this.temperatureCelsius = temperatureCelsius;
        this.condition = condition;
    }

    // Returns the hour label (for example: 15:00).
    public String getHourLabel() {
        return hourLabel;
    }

    // Returns hourly temperature in Celsius.
    public double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    // Returns hourly weather condition text.
    public String getCondition() {
        return condition;
    }
}



