package com.personaleenergy.app.llm;

import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.data.models.ProductivitySuggestion;
import com.flowstate.app.data.models.EnergyLevel;
import com.flowstate.core.Config;
import java.util.*;

public class LLMService {
    private static final String API_KEY = Config.OPENAI_API_KEY; // Loaded from BuildConfig
    
    /**
     * Generate personalized schedule based on energy predictions
     */
    public List<ProductivitySuggestion> generateSchedule(List<EnergyPrediction> predictions) {
        List<ProductivitySuggestion> suggestions = new ArrayList<>();
        
        for (EnergyPrediction pred : predictions) {
            String timeSlot = formatTimeSlot(pred.getTimestamp());
            String activity = suggestActivity(pred.getPredictedLevel());
            String reasoning = generateReasoning(pred);
            
            suggestions.add(new ProductivitySuggestion(
                    timeSlot, activity, reasoning, pred.getPredictedLevel()
            ));
        }
        
        return suggestions;
    }
    
    private String suggestActivity(EnergyLevel energyLevel) {
        switch (energyLevel) {
            case HIGH:
                return "Intensive work: Focus on demanding tasks, problem-solving, creative work";
            case MEDIUM:
                return "Moderate work: Routine tasks, emails, meetings, data entry";
            case LOW:
                return "Restorative: Take breaks, light reading, gentle walks, meditation";
            default:
                return "Adjust schedule based on how you feel";
        }
    }
    
    private String generateReasoning(EnergyPrediction pred) {
        StringBuilder reason = new StringBuilder("Energy level is ");
        reason.append(pred.getPredictedLevel().toString().toLowerCase());
        reason.append(" (confidence: ").append(String.format("%.1f%%", pred.getConfidence() * 100));
        reason.append("). ");
        
        if (pred.getBiometricFactors() != null && !pred.getBiometricFactors().isEmpty()) {
            pred.getBiometricFactors().forEach((key, value) -> {
                switch (key) {
                    case "heartRate":
                        reason.append("Heart rate: ").append(value.intValue()).append(" bpm. ");
                        break;
                    case "sleepQuality":
                        reason.append("Recent sleep quality: ").append(String.format("%.1f", value * 100)).append("%. ");
                        break;
                }
            });
        }
        
        return reason.toString();
    }
    
    private String formatTimeSlot(Date timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp);
        int hour = cal.get(Calendar.HOUR_OF_DAY);
        int minute = cal.get(Calendar.MINUTE);
        
        String period = hour < 12 ? "AM" : "PM";
        int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
        
        return String.format("%d:%02d %s - %d:%02d %s",
                displayHour, minute, period,
                displayHour == 11 ? 12 : displayHour + 1, minute, 
                hour < 11 ? "AM" : (hour < 23 ? "PM" : "AM"));
    }
    
    /**
     * Simple rule-based approach for now
     * Can be enhanced to call OpenAI API for more sophisticated suggestions
     */
    public String generateGeneralAdvice(List<EnergyPrediction> predictions) {
        long highEnergyCount = predictions.stream()
                .filter(p -> p.getPredictedLevel() == EnergyLevel.HIGH)
                .count();
        
        long lowEnergyCount = predictions.stream()
                .filter(p -> p.getPredictedLevel() == EnergyLevel.LOW)
                .count();
        
        if (highEnergyCount > predictions.size() / 2) {
            return "You're predicted to have high energy today! This is a great day for challenging tasks. " +
                   "Schedule your most important work during peak hours.";
        } else if (lowEnergyCount > predictions.size() / 2) {
            return "Lower energy predicted today. Focus on less demanding tasks and ensure you take regular breaks. " +
                   "Consider light exercise or meditation to boost energy.";
        } else {
            return "Mixed energy levels today. Plan your schedule carefully - use high energy periods for " +
                   "important work and low energy times for routine tasks.";
        }
    }
}

