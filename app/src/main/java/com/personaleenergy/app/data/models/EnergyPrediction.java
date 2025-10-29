package com.personaleenergy.app.data.models;

import java.util.Date;
import java.util.Map;

/**
 * Represents predicted energy level for a specific time
 */
public class EnergyPrediction {
    private Date timestamp;
    private EnergyLevel predictedLevel;
    private double confidence; // 0.0 to 1.0
    private Map<String, Double> biometricFactors;
    private Map<String, Double> cognitiveFactors;

    public EnergyPrediction(Date timestamp, EnergyLevel predictedLevel, double confidence,
                           Map<String, Double> biometricFactors, Map<String, Double> cognitiveFactors) {
        this.timestamp = timestamp;
        this.predictedLevel = predictedLevel;
        this.confidence = confidence;
        this.biometricFactors = biometricFactors;
        this.cognitiveFactors = cognitiveFactors;
    }

    // Getters and Setters
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public EnergyLevel getPredictedLevel() {
        return predictedLevel;
    }

    public void setPredictedLevel(EnergyLevel predictedLevel) {
        this.predictedLevel = predictedLevel;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Double> getBiometricFactors() {
        return biometricFactors;
    }

    public void setBiometricFactors(Map<String, Double> biometricFactors) {
        this.biometricFactors = biometricFactors;
    }

    public Map<String, Double> getCognitiveFactors() {
        return cognitiveFactors;
    }

    public void setCognitiveFactors(Map<String, Double> cognitiveFactors) {
        this.cognitiveFactors = cognitiveFactors;
    }
}

