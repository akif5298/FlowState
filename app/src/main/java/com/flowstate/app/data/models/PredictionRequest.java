package com.flowstate.app.data.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;

/**
 * Request model for energy level prediction API
 * Used internally - actual HuggingFace API uses simple {"history": [...]} format
 */
public class PredictionRequest {
    
    // Legacy format: single history array
    @SerializedName("history")
    private List<Double> history;
    
    // New multivariate format
    @SerializedName("past_values")
    private Map<String, List<Double>> pastValues;
    
    @SerializedName("timestamps")
    private List<Long> timestamps;
    
    @SerializedName("forecast_horizon")
    private Integer forecastHorizon;
    
    public PredictionRequest() {
        this.forecastHorizon = 12; // Default to 12 periods
    }
    
    // Constructor for legacy format
    public PredictionRequest(List<Double> history, Integer forecastHorizon) {
        this.history = history;
        this.forecastHorizon = forecastHorizon != null ? forecastHorizon : 12;
    }
    
    // Constructor for multivariate format
    public PredictionRequest(Map<String, List<Double>> pastValues, List<Long> timestamps, Integer forecastHorizon) {
        this.pastValues = pastValues;
        this.timestamps = timestamps;
        this.forecastHorizon = forecastHorizon != null ? forecastHorizon : 12;
    }
    
    // Getters and Setters
    public List<Double> getHistory() {
        return history;
    }
    
    public void setHistory(List<Double> history) {
        this.history = history;
    }
    
    public Map<String, List<Double>> getPastValues() {
        return pastValues;
    }
    
    public void setPastValues(Map<String, List<Double>> pastValues) {
        this.pastValues = pastValues;
    }
    
    public List<Long> getTimestamps() {
        return timestamps;
    }
    
    public void setTimestamps(List<Long> timestamps) {
        this.timestamps = timestamps;
    }
    
    public Integer getForecastHorizon() {
        return forecastHorizon;
    }
    
    public void setForecastHorizon(Integer forecastHorizon) {
        this.forecastHorizon = forecastHorizon;
    }
}

