package com.flowstate.app.data.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

/**
 * Response model for energy level prediction API
 * Handles wrapped response format (HuggingFace may return direct array or wrapped object)
 */
public class PredictionResponse {
    
    @SerializedName("status")
    private String status;
    
    @SerializedName("mode")
    private String mode;
    
    @SerializedName("forecast")
    private List<Double> forecast;
    
    @SerializedName("forecast_horizon")
    private Integer forecastHorizon;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("error")
    private String error;
    
    // Getters and Setters
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getMode() {
        return mode;
    }
    
    public void setMode(String mode) {
        this.mode = mode;
    }
    
    public List<Double> getForecast() {
        return forecast;
    }
    
    public void setForecast(List<Double> forecast) {
        this.forecast = forecast;
    }
    
    public Integer getForecastHorizon() {
        return forecastHorizon;
    }
    
    public void setForecastHorizon(Integer forecastHorizon) {
        this.forecastHorizon = forecastHorizon;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public boolean isSuccess() {
        return "success".equals(status) && forecast != null && !forecast.isEmpty();
    }
}

