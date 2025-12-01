package com.personaleenergy.app.data.models;

import java.util.Date;
import java.util.Map;

public class EnergyPrediction {
    private final Date timestamp;
    private final EnergyLevel level;
    private final double confidence;
    private final Map<String, Double> biometricFactors;
    private final Map<String, Double> cognitiveFactors;

    public EnergyPrediction(Date timestamp, EnergyLevel level, double confidence, Map<String, Double> biometricFactors, Map<String, Double> cognitiveFactors) {
        this.timestamp = timestamp;
        this.level = level;
        this.confidence = confidence;
        this.biometricFactors = biometricFactors;
        this.cognitiveFactors = cognitiveFactors;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public EnergyLevel getLevel() {
        return level;
    }

    public double getConfidence() {
        return confidence;
    }

    public Map<String, Double> getBiometricFactors() {
        return biometricFactors;
    }

    public Map<String, Double> getCognitiveFactors() {
        return cognitiveFactors;
    }
}
