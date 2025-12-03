package com.flowstate.app.data.models;

import java.util.Date;

/**
 * Data model for biometric data
 */
public class BiometricData {
    private Date timestamp;
    private Integer heartRate;
    private Integer sleepMinutes;
    private Double sleepQuality;
    private Double skinTemperature;

    public BiometricData() {}

    public BiometricData(Date timestamp, Integer heartRate, Integer sleepMinutes, 
                         Double sleepQuality, Double skinTemperature) {
        this.timestamp = timestamp;
        this.heartRate = heartRate;
        this.sleepMinutes = sleepMinutes;
        this.sleepQuality = sleepQuality;
        this.skinTemperature = skinTemperature;
    }

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
