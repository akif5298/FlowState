package com.personaleenergy.app.ml;

import com.personaleenergy.app.data.models.EnergyLevel;
import com.personaleenergy.app.data.models.EnergyPrediction;
import com.personaleenergy.app.data.models.BiometricData;
import java.util.*;
import java.util.stream.Collectors;

public class EnergyPredictor {
    
    /**
     * Predict energy levels based on biometric and cognitive data
     * Simple rule-based approach for now (can be replaced with TensorFlow Lite model)
     */
    public List<EnergyPrediction> predictEnergyLevels(
            List<BiometricData> biometricData,
            int hours) {
        
        List<EnergyPrediction> predictions = new ArrayList<>();
        
        // Generate predictions for next N hours
        Calendar calendar = Calendar.getInstance();
        for (int i = 0; i < hours; i++) {
            Date timestamp = calendar.getTime();
            EnergyLevel level = predictForTime(timestamp, biometricData);
            double confidence = calculateConfidence(biometricData);
            
            Map<String, Double> bioFactors = extractBiometricFactors(biometricData);
            Map<String, Double> cogFactors = new HashMap<>();
            
            predictions.add(new EnergyPrediction(timestamp, level, confidence, bioFactors, cogFactors));
            calendar.add(Calendar.HOUR_OF_DAY, 1);
        }
        
        return predictions;
    }
    
    private EnergyLevel predictForTime(Date timestamp, List<BiometricData> biometricData) {
        // Simple heuristic-based prediction
        // Find most recent data points
        List<BiometricData> recent = getRecentData(biometricData, timestamp, 24);
        
        if (recent.isEmpty()) {
            return EnergyLevel.MEDIUM;
        }
        
        double energyScore = 0.0;
        int hourOfDay = getHour(timestamp);
        
        // Heart rate analysis
        List<Integer> heartRates = recent.stream()
                .map(BiometricData::getHeartRate)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (!heartRates.isEmpty()) {
            double avgHeartRate = heartRates.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(70);
            
            // Resting heart rate ~60-100 bpm is normal
            if (avgHeartRate < 60) energyScore += 0.2; // too low
            else if (avgHeartRate < 80) energyScore += 0.7; // good
            else energyScore += 0.3; // elevated
        }
        
        // Sleep analysis
        List<BiometricData> sleepData = recent.stream()
                .filter(d -> d.getSleepQuality() != null)
                .collect(Collectors.toList());
        
        if (!sleepData.isEmpty()) {
            double avgSleepQuality = sleepData.stream()
                    .mapToDouble(BiometricData::getSleepQuality)
                    .average()
                    .orElse(0.5);
            energyScore += avgSleepQuality * 0.5;
        }
        
        // Time of day factors
        if (hourOfDay >= 6 && hourOfDay < 9) {
            energyScore += 0.4; // morning boost
        } else if (hourOfDay >= 14 && hourOfDay < 16) {
            energyScore -= 0.3; // afternoon dip
        }
        
        // Determine energy level based on score
        if (energyScore >= 0.7) return EnergyLevel.HIGH;
        else if (energyScore >= 0.4) return EnergyLevel.MEDIUM;
        else return EnergyLevel.LOW;
    }
    
    private double calculateConfidence(List<BiometricData> biometricData) {
        if (biometricData.isEmpty()) return 0.3;
        
        long recentCount = biometricData.stream()
                .filter(d -> {
                    long hoursSince = (System.currentTimeMillis() - d.getTimestamp().getTime()) / (1000 * 60 * 60);
                    return hoursSince < 24;
                })
                .count();
        
        return Math.min(0.7 + (recentCount * 0.1), 1.0);
    }
    
    private Map<String, Double> extractBiometricFactors(List<BiometricData> biometricData) {
        Map<String, Double> factors = new HashMap<>();
        
        List<Integer> heartRates = biometricData.stream()
                .map(BiometricData::getHeartRate)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (!heartRates.isEmpty()) {
            double avgHeartRate = heartRates.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(70);
            factors.put("heartRate", avgHeartRate);
        }
        
        List<Double> sleepQualities = biometricData.stream()
                .map(BiometricData::getSleepQuality)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        
        if (!sleepQualities.isEmpty()) {
            double avgSleepQuality = sleepQualities.stream()
                    .mapToDouble(Double::doubleValue)
                    .average()
                    .orElse(0.5);
            factors.put("sleepQuality", avgSleepQuality);
        }
        
        return factors;
    }
    
    private List<BiometricData> getRecentData(List<BiometricData> data, Date timestamp, int hours) {
        long cutoff = timestamp.getTime() - (hours * 60 * 60 * 1000);
        return data.stream()
                .filter(d -> d.getTimestamp().getTime() >= cutoff)
                .collect(Collectors.toList());
    }
    
    private int getHour(Date timestamp) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(timestamp);
        return cal.get(Calendar.HOUR_OF_DAY);
    }
}

