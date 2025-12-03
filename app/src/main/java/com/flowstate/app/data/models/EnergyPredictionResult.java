package com.flowstate.app.data.models;

import java.util.List;

/**
 * Result from Gemini energy prediction containing forecast and explanation
 */
public class EnergyPredictionResult {
    
    private List<Double> hourlyPredictions; // 12 hourly energy levels (0-100)
    private String explanation; // Technical analysis of the prediction
    private String actionableInsight; // Actionable advice for the user
    
    public EnergyPredictionResult(List<Double> hourlyPredictions, String explanation, String actionableInsight) {
        this.hourlyPredictions = hourlyPredictions;
        this.explanation = explanation;
        this.actionableInsight = actionableInsight;
    }
    
    public EnergyPredictionResult(List<Double> hourlyPredictions, String explanation) {
        this(hourlyPredictions, explanation, null);
    }
    
    public List<Double> getHourlyPredictions() {
        return hourlyPredictions;
    }
    
    public void setHourlyPredictions(List<Double> hourlyPredictions) {
        this.hourlyPredictions = hourlyPredictions;
    }
    
    public String getExplanation() {
        return explanation;
    }
    
    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }

    public String getActionableInsight() {
        return actionableInsight;
    }

    public void setActionableInsight(String actionableInsight) {
        this.actionableInsight = actionableInsight;
    }
    
    /**
     * Get current energy level (first hour prediction)
     */
    public Double getCurrentEnergyLevel() {
        if (hourlyPredictions != null && !hourlyPredictions.isEmpty()) {
            return hourlyPredictions.get(0);
        }
        return null;
    }
}

