package com.personaleenergy.app.data.models;

import java.util.Date;

/**
 * Represents biometric data collected from wearables (Google Fit)
 */
public class BiometricData {
    private Date timestamp;
    private Integer heartRate; // bpm
    private Integer sleepMinutes;
    private Double sleepQuality; // 0.0 to 1.0
    private Double skinTemperature; // Celsius

    public BiometricData(Date timestamp, Integer heartRate, Integer sleepMinutes, 
                        Double sleepQuality, Double skinTemperature) {
        this.timestamp = timestamp;
        this.heartRate = heartRate;
        this.sleepMinutes = sleepMinutes;
        this.sleepQuality = sleepQuality;
        this.skinTemperature = skinTemperature;
    }

    public BiometricData(Date timestamp) {
        this.timestamp = timestamp;
    }

    // Getters and Setters
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getHeartRate() {
        return heartRate;
    }

    public void setHeartRate(Integer heartRate) {
        this.heartRate = heartRate;
    }

    public Integer getSleepMinutes() {
        return sleepMinutes;
    }

    public void setSleepMinutes(Integer sleepMinutes) {
        this.sleepMinutes = sleepMinutes;
    }

    public Double getSleepQuality() {
        return sleepQuality;
    }

    public void setSleepQuality(Double sleepQuality) {
        this.sleepQuality = sleepQuality;
    }

    public Double getSkinTemperature() {
        return skinTemperature;
    }

    public void setSkinTemperature(Double skinTemperature) {
        this.skinTemperature = skinTemperature;
    }
}
