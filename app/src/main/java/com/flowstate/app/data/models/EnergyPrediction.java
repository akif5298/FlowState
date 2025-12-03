package com.flowstate.app.data.models;

import java.util.Date;
import java.util.Map;

/**
 * Data model for energy predictions
 */
public class EnergyPrediction {
    private Date timestamp;
    private EnergyLevel predictedLevel;
    private Double confidence;
    private Map<String, Object> biometricFactors;
    private Map<String, Object> cognitiveFactors;

    public EnergyPrediction() {}

    public EnergyPrediction(Date timestamp, EnergyLevel predictedLevel, Double confidence,
                           Map<String, Object> biometricFactors, Map<String, Object> cognitiveFactors) {
        this.timestamp = timestamp;
        this.predictedLevel = predictedLevel;
        this.confidence = confidence;
        this.biometricFactors = biometricFactors;
        this.cognitiveFactors = cognitiveFactors;
    }

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

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Map<String, Object> getBiometricFactors() {
        return biometricFactors;
    }

    public void setBiometricFactors(Map<String, Object> biometricFactors) {
        this.biometricFactors = biometricFactors;
    }

    public Map<String, Object> getCognitiveFactors() {
        return cognitiveFactors;
    }

    public void setCognitiveFactors(Map<String, Object> cognitiveFactors) {
        this.cognitiveFactors = cognitiveFactors;
    }
}
