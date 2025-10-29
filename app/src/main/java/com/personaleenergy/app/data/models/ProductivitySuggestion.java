package com.personaleenergy.app.data.models;

import java.util.Date;

/**
 * Represents LLM-generated suggestions based on energy predictions
 */
public class ProductivitySuggestion {
    private String timeSlot;
    private String suggestedActivity;
    private String reasoning;
    private EnergyLevel estimatedEnergyImpact;

    public ProductivitySuggestion(String timeSlot, String suggestedActivity, 
                                 String reasoning, EnergyLevel estimatedEnergyImpact) {
        this.timeSlot = timeSlot;
        this.suggestedActivity = suggestedActivity;
        this.reasoning = reasoning;
        this.estimatedEnergyImpact = estimatedEnergyImpact;
    }

    // Getters and Setters
    public String getTimeSlot() {
        return timeSlot;
    }

    public void setTimeSlot(String timeSlot) {
        this.timeSlot = timeSlot;
    }

    public String getSuggestedActivity() {
        return suggestedActivity;
    }

    public void setSuggestedActivity(String suggestedActivity) {
        this.suggestedActivity = suggestedActivity;
    }

    public String getReasoning() {
        return reasoning;
    }

    public void setReasoning(String reasoning) {
        this.reasoning = reasoning;
    }

    public EnergyLevel getEstimatedEnergyImpact() {
        return estimatedEnergyImpact;
    }

    public void setEstimatedEnergyImpact(EnergyLevel estimatedEnergyImpact) {
        this.estimatedEnergyImpact = estimatedEnergyImpact;
    }
}

