package com.flowstate.services;

import android.content.Context;
import java.util.concurrent.CompletableFuture;

public class WeatherCollector {
    private final Context context;

    public WeatherCollector(Context context) {
        this.context = context;
    }

    public CompletableFuture<WeatherInfo> getCurrentWeather() {
        // TODO: Implement actual API call (e.g. OpenMeteo)
        // For now returning mock data to satisfy requirements
        return CompletableFuture.completedFuture(new WeatherInfo("Clear", 22.5f));
    }

    public static class WeatherInfo {
        public final String condition;
        public final float temperatureCelsius;
        
        public WeatherInfo(String condition, float temp) {
            this.condition = condition;
            this.temperatureCelsius = temp;
        }
    }
}

