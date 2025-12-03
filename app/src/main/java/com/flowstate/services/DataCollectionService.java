package com.flowstate.services;

import android.content.Context;
import android.util.Log;
import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.DeviceUsageLocal;
import com.flowstate.data.local.entities.WeatherLocal;

import java.util.Calendar;

/**
 * Service to automatically collect and store device usage and weather data
 * Runs periodically to update database
 */
public class DataCollectionService {
    
    private static final String TAG = "DataCollectionService";
    
    private final AppDb database;
    private final DeviceUsageCollector deviceCollector;
    private final WeatherCollector weatherCollector;
    
    public DataCollectionService(Context context) {
        this.database = AppDb.getInstance(context);
        this.deviceCollector = new DeviceUsageCollector(context);
        this.weatherCollector = new WeatherCollector(context);
    }
    
    /**
     * Collect and store device usage for today
     */
    public void collectDeviceUsage() {
        try {
            if (!deviceCollector.hasPermission()) {
                Log.d(TAG, "Device usage permission not granted");
                return;
            }
            
            long screenTimeMs = deviceCollector.getTotalScreenTimeToday();
            long screenTimeMinutes = screenTimeMs / (1000 * 60);
            
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long dateStart = cal.getTimeInMillis();
            
            DeviceUsageLocal existing = database.deviceUsageDao().getByDate(dateStart);
            if (existing != null) {
                // Update existing
                existing.screenTimeMinutes = screenTimeMinutes;
                existing.lastUpdated = System.currentTimeMillis();
                database.deviceUsageDao().update(existing);
            } else {
                // Create new
                DeviceUsageLocal usage = new DeviceUsageLocal();
                usage.date = dateStart;
                usage.screenTimeMinutes = screenTimeMinutes;
                usage.lastUpdated = System.currentTimeMillis();
                database.deviceUsageDao().insert(usage);
            }
            
            Log.d(TAG, "Device usage collected: " + screenTimeMinutes + " minutes");
        } catch (Exception e) {
            Log.e(TAG, "Error collecting device usage", e);
        }
    }
    
    /**
     * Collect and store weather data
     */
    public void collectWeather() {
        try {
            weatherCollector.getCurrentWeather().thenAccept(weather -> {
                if (weather != null) {
                    WeatherLocal weatherLocal = new WeatherLocal();
                    weatherLocal.timestamp = System.currentTimeMillis();
                    weatherLocal.condition = weather.condition;
                    weatherLocal.temperatureCelsius = (double) weather.temperatureCelsius;
                    
                    // Determine season
                    Calendar cal = Calendar.getInstance();
                    int month = cal.get(Calendar.MONTH) + 1;
                    String season;
                    if (month >= 3 && month <= 5) {
                        season = "spring";
                    } else if (month >= 6 && month <= 8) {
                        season = "summer";
                    } else if (month >= 9 && month <= 11) {
                        season = "fall";
                    } else {
                    season = "winter";
                }
                weatherLocal.season = season;
                
                database.weatherDao().insert(weatherLocal);
                    Log.d(TAG, "Weather collected: " + weather.condition + ", " + weather.temperatureCelsius + "°C");
                }
            }).exceptionally(error -> {
                Log.e(TAG, "Error collecting weather", error);
                return null;
            });
        } catch (Exception e) {
            Log.e(TAG, "Error in weather collection", e);
        }
    }
    
    /**
     * Collect all automatic data (device usage, weather)
     */
    public void collectAll() {
        collectDeviceUsage();
        collectWeather();
    }
}

