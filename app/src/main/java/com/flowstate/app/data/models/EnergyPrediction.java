package com.flowstate.app.data.models;

import java.util.Date;
import java.util.Map;

/**
 * Represents predicted energy level for a specific time
 * Energy level is a numeric value from 0-100 (0 = low, 100 = high)
 */
public class EnergyPrediction {
    private Date timestamp;
    private EnergyLevel predictedLevel; // Enum for compatibility
    private double energyLevel; // Numeric value 0-100 from model
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
        // Derive energyLevel from predictedLevel if not set
        this.energyLevel = energyLevelFromEnum(predictedLevel);
    }
    
    /**
     * Constructor with numeric energy level (0-100)
     */
    public EnergyPrediction(Date timestamp, double energyLevel, double confidence,
                           Map<String, Double> biometricFactors, Map<String, Double> cognitiveFactors) {
        this.timestamp = timestamp;
        this.energyLevel = energyLevel;
        this.predictedLevel = enumFromEnergyLevel(energyLevel);
        this.confidence = confidence;
        this.biometricFactors = biometricFactors;
        this.cognitiveFactors = cognitiveFactors;
    }
    
    /**
     * Convert numeric energy level (0-100) to enum
     */
    private EnergyLevel enumFromEnergyLevel(double level) {
        if (level >= 70) return EnergyLevel.HIGH;
        if (level >= 40) return EnergyLevel.MEDIUM;
        return EnergyLevel.LOW;
    }
    
    /**
     * Convert enum to numeric energy level (0-100)
     */
    private double energyLevelFromEnum(EnergyLevel level) {
        if (level == null) return 50.0; // Default
        switch (level) {
            case HIGH: return 80.0;
            case MEDIUM: return 50.0;
            case LOW: return 20.0;
            default: return 50.0;
        }
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
        this.energyLevel = energyLevelFromEnum(predictedLevel);
    }
    
    /**
     * Get numeric energy level (0-100)
     */
    public double getEnergyLevel() {
        return energyLevel;
    }
    
    /**
     * Set numeric energy level (0-100)
     */
    public void setEnergyLevel(double energyLevel) {
        this.energyLevel = Math.max(0, Math.min(100, energyLevel)); // Clamp to 0-100
        this.predictedLevel = enumFromEnergyLevel(this.energyLevel);
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

