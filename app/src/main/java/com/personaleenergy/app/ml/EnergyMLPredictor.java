package com.personaleenergy.app.ml;

import android.content.Context;
import android.util.Log;
import com.flowstate.app.data.models.BiometricData;
import com.flowstate.app.data.models.TypingSpeedData;
import com.flowstate.app.data.models.ReactionTimeData;
import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.data.models.EnergyLevel;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.common.FileUtil;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.util.*;

/**
 * TensorFlow Lite-based Energy Prediction Model
 * 
 * Uses on-device ML to predict energy levels based on:
 * - Sleep quality and duration
 * - Heart rate patterns
 * - Cognitive performance (typing speed, reaction time)
 * - Time of day patterns
 * - Historical energy patterns
 */
public class EnergyMLPredictor {
    
    private static final String TAG = "EnergyMLPredictor";
    private static final String MODEL_FILENAME = "energy_model.tflite";
    private static final int NUM_FEATURES = 15; // Number of input features
    private static final int NUM_OUTPUTS = 3; // HIGH, MEDIUM, LOW probabilities
    
    private Context context;
    private Interpreter tflite;
    private boolean modelLoaded = false;
    
    public EnergyMLPredictor(Context context) {
        this.context = context.getApplicationContext();
        loadModel();
    }
    
    /**
     * Load TensorFlow Lite model
     * If model file doesn't exist, creates a simple rule-based fallback
     */
    private void loadModel() {
        try {
            // Try to load model from assets
            MappedByteBuffer modelBuffer = loadModelFile();
            if (modelBuffer != null) {
                Interpreter.Options options = new Interpreter.Options();
                options.setNumThreads(4); // Use 4 threads for faster inference
                options.setUseXNNPACK(true); // Use optimized XNNPACK backend
                tflite = new Interpreter(modelBuffer, options);
                modelLoaded = true;
                Log.d(TAG, "TensorFlow Lite model loaded successfully");
            } else {
                Log.w(TAG, "Model file not found, will use rule-based fallback");
                modelLoaded = false;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading TensorFlow Lite model", e);
            modelLoaded = false;
        }
    }
    
    /**
     * Try to load model from assets, fallback to creating a simple model
     */
    private MappedByteBuffer loadModelFile() {
        try {
            // First try assets folder
            return FileUtil.loadMappedFile(context, MODEL_FILENAME);
        } catch (IOException e) {
            Log.d(TAG, "Model not in assets, will use rule-based prediction");
            return null;
        }
    }
    
    /**
     * Extract features from user data for ML model input
     */
    public float[] extractFeatures(
            List<BiometricData> biometrics,
            List<TypingSpeedData> typingData,
            List<ReactionTimeData> reactionData,
            List<EnergyPrediction> historicalPredictions,
            Date targetTime) {
        
        float[] features = new float[NUM_FEATURES];
        Arrays.fill(features, 0.0f);
        
        Calendar cal = Calendar.getInstance();
        cal.setTime(targetTime);
        int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        
        // Feature 0: Hour of day (normalized 0-1)
        features[0] = hourOfDay / 24.0f;
        
        // Feature 1: Day of week (normalized 0-1)
        features[1] = dayOfWeek / 7.0f;
        
        // Feature 2: Cosine of hour (circadian rhythm)
        features[2] = (float) Math.cos(2 * Math.PI * hourOfDay / 24.0);
        
        // Feature 3: Sine of hour (circadian rhythm)
        features[3] = (float) Math.sin(2 * Math.PI * hourOfDay / 24.0);
        
        // Sleep features (last 7 days)
        if (biometrics != null && !biometrics.isEmpty()) {
            List<BiometricData> recentSleep = getRecentData(biometrics, targetTime, 7 * 24);
            
            // Feature 4: Average sleep quality (0-1)
            double avgSleepQuality = recentSleep.stream()
                .filter(d -> d.getSleepQuality() != null)
                .mapToDouble(BiometricData::getSleepQuality)
                .average()
                .orElse(0.5);
            features[4] = (float) avgSleepQuality;
            
            // Feature 5: Average sleep hours (normalized, assuming 4-12 hour range)
            double avgSleepHours = recentSleep.stream()
                .filter(d -> d.getSleepMinutes() != null)
                .mapToInt(BiometricData::getSleepMinutes)
                .average()
                .orElse(480.0) / 60.0;
            features[5] = (float) Math.max(0, Math.min(1, (avgSleepHours - 4.0) / 8.0));
            
            // Feature 6: Sleep consistency (std dev of sleep hours, lower is better)
            List<Double> sleepHours = new ArrayList<>();
            for (BiometricData d : recentSleep) {
                if (d.getSleepMinutes() != null) {
                    sleepHours.add(d.getSleepMinutes() / 60.0);
                }
            }
            if (sleepHours.size() > 1) {
                double mean = sleepHours.stream().mapToDouble(Double::doubleValue).average().orElse(8.0);
                double variance = sleepHours.stream()
                    .mapToDouble(h -> Math.pow(h - mean, 2))
                    .average()
                    .orElse(0.0);
                double stdDev = Math.sqrt(variance);
                features[6] = (float) Math.max(0, Math.min(1, 1.0 - (stdDev / 3.0))); // Lower std dev = higher score
            } else {
                features[6] = 0.5f;
            }
        }
        
        // Heart rate features (last 24 hours)
        if (biometrics != null && !biometrics.isEmpty()) {
            List<BiometricData> recentHR = getRecentData(biometrics, targetTime, 24);
            
            // Feature 7: Average heart rate (normalized, assuming 50-120 bpm range)
            double avgHR = recentHR.stream()
                .filter(d -> d.getHeartRate() != null)
                .mapToInt(BiometricData::getHeartRate)
                .average()
                .orElse(70.0);
            features[7] = (float) Math.max(0, Math.min(1, (avgHR - 50.0) / 70.0));
            
            // Feature 8: Heart rate variability (std dev, higher is better for recovery)
            List<Integer> heartRates = new ArrayList<>();
            for (BiometricData d : recentHR) {
                if (d.getHeartRate() != null) {
                    heartRates.add(d.getHeartRate());
                }
            }
            if (heartRates.size() > 1) {
                double mean = heartRates.stream().mapToInt(Integer::intValue).average().orElse(70.0);
                double variance = heartRates.stream()
                    .mapToDouble(hr -> Math.pow(hr - mean, 2))
                    .average()
                    .orElse(0.0);
                double stdDev = Math.sqrt(variance);
                features[8] = (float) Math.max(0, Math.min(1, stdDev / 20.0)); // Normalize to 0-1
            } else {
                features[8] = 0.3f;
            }
        }
        
        // Cognitive performance features (last 7 days, same hour of day)
        if (typingData != null && !typingData.isEmpty()) {
            List<TypingSpeedData> recentTyping = getRecentDataTyping(typingData, targetTime, 7 * 24, hourOfDay);
            
            // Feature 9: Average typing speed (normalized, assuming 20-100 WPM)
            double avgWPM = recentTyping.stream()
                .mapToInt(TypingSpeedData::getWordsPerMinute)
                .average()
                .orElse(50.0);
            features[9] = (float) Math.max(0, Math.min(1, (avgWPM - 20.0) / 80.0));
            
            // Feature 10: Average typing accuracy (0-1)
            double avgAccuracy = recentTyping.stream()
                .mapToDouble(TypingSpeedData::getAccuracy)
                .average()
                .orElse(85.0) / 100.0;
            features[10] = (float) avgAccuracy;
        }
        
        if (reactionData != null && !reactionData.isEmpty()) {
            List<ReactionTimeData> recentReaction = getRecentDataReaction(reactionData, targetTime, 7 * 24, hourOfDay);
            
            // Feature 11: Average reaction time (normalized, lower is better, assuming 150-500ms)
            double avgReaction = recentReaction.stream()
                .mapToInt(ReactionTimeData::getReactionTimeMs)
                .average()
                .orElse(300.0);
            features[11] = (float) Math.max(0, Math.min(1, 1.0 - ((avgReaction - 150.0) / 350.0)));
        }
        
        // Historical energy pattern features
        if (historicalPredictions != null && !historicalPredictions.isEmpty()) {
            // Feature 12: Average energy at this hour (from historical data)
            List<EnergyPrediction> sameHour = getPredictionsAtHour(historicalPredictions, hourOfDay);
            if (!sameHour.isEmpty()) {
                double avgEnergy = sameHour.stream()
                    .mapToDouble(p -> {
                        switch (p.getPredictedLevel()) {
                            case HIGH: return 1.0;
                            case MEDIUM: return 0.5;
                            case LOW: return 0.0;
                            default: return 0.5;
                        }
                    })
                    .average()
                    .orElse(0.5);
                features[12] = (float) avgEnergy;
            }
            
            // Feature 13: Energy trend (recent vs older predictions)
            List<EnergyPrediction> recentPreds = getRecentPredictions(historicalPredictions, targetTime, 3 * 24);
            List<EnergyPrediction> olderPreds = getRecentPredictions(historicalPredictions, targetTime, 7 * 24);
            if (!recentPreds.isEmpty() && !olderPreds.isEmpty()) {
                double recentAvg = recentPreds.stream()
                    .mapToDouble(p -> {
                        switch (p.getPredictedLevel()) {
                            case HIGH: return 1.0;
                            case MEDIUM: return 0.5;
                            case LOW: return 0.0;
                            default: return 0.5;
                        }
                    })
                    .average()
                    .orElse(0.5);
                double olderAvg = olderPreds.stream()
                    .mapToDouble(p -> {
                        switch (p.getPredictedLevel()) {
                            case HIGH: return 1.0;
                            case MEDIUM: return 0.5;
                            case LOW: return 0.0;
                            default: return 0.5;
                        }
                    })
                    .average()
                    .orElse(0.5);
                features[13] = (float) (recentAvg - olderAvg); // Positive = improving, negative = declining
            }
        }
        
        // Feature 14: Time since last sleep (hours, normalized)
        if (biometrics != null && !biometrics.isEmpty()) {
            Optional<BiometricData> lastSleep = biometrics.stream()
                .filter(d -> d.getSleepMinutes() != null)
                .max(Comparator.comparing(BiometricData::getTimestamp));
            
            if (lastSleep.isPresent()) {
                long hoursSinceSleep = (targetTime.getTime() - lastSleep.get().getTimestamp().getTime()) / (1000 * 60 * 60);
                features[14] = (float) Math.max(0, Math.min(1, hoursSinceSleep / 16.0)); // Normalize to 0-1 (16 hours = 1.0)
            }
        }
        
        return features;
    }
    
    /**
     * Predict energy level using TensorFlow Lite model or rule-based fallback
     */
    public EnergyPrediction predict(
            List<BiometricData> biometrics,
            List<TypingSpeedData> typingData,
            List<ReactionTimeData> reactionData,
            List<EnergyPrediction> historicalPredictions,
            Date targetTime) {
        
        float[] features = extractFeatures(biometrics, typingData, reactionData, historicalPredictions, targetTime);
        
        if (modelLoaded && tflite != null) {
            // Use TensorFlow Lite model
            float[][] output = new float[1][NUM_OUTPUTS];
            tflite.run(features, output);
            
            // output[0] contains [HIGH_prob, MEDIUM_prob, LOW_prob]
            float highProb = output[0][0];
            float mediumProb = output[0][1];
            float lowProb = output[0][2];
            
            // Determine energy level based on highest probability
            EnergyLevel level;
            double confidence;
            if (highProb >= mediumProb && highProb >= lowProb) {
                level = EnergyLevel.HIGH;
                confidence = highProb;
            } else if (mediumProb >= lowProb) {
                level = EnergyLevel.MEDIUM;
                confidence = mediumProb;
            } else {
                level = EnergyLevel.LOW;
                confidence = lowProb;
            }
            
            Log.d(TAG, String.format("ML Prediction: %s (confidence: %.2f, probs: H=%.2f M=%.2f L=%.2f)", 
                level, confidence, highProb, mediumProb, lowProb));
            
            return new EnergyPrediction(targetTime, level, confidence, null, null);
        } else {
            // Fallback to rule-based prediction using extracted features
            return ruleBasedPrediction(features, targetTime);
        }
    }
    
    /**
     * Rule-based fallback when ML model is not available
     */
    private EnergyPrediction ruleBasedPrediction(float[] features, Date targetTime) {
        // Weighted scoring based on features
        double score = 0.0;
        double totalWeight = 0.0;
        
        // Sleep quality (weight: 0.25)
        score += features[4] * 0.25;
        totalWeight += 0.25;
        
        // Sleep hours (weight: 0.15)
        score += features[5] * 0.15;
        totalWeight += 0.15;
        
        // Sleep consistency (weight: 0.10)
        score += features[6] * 0.10;
        totalWeight += 0.10;
        
        // Heart rate (weight: 0.15, but inverted - lower is better for energy)
        score += (1.0 - features[7]) * 0.15;
        totalWeight += 0.15;
        
        // HRV (weight: 0.10)
        score += features[8] * 0.10;
        totalWeight += 0.10;
        
        // Cognitive performance (weight: 0.15)
        score += (features[9] * 0.5 + features[10] * 0.5) * 0.15;
        totalWeight += 0.15;
        
        // Historical pattern (weight: 0.10)
        score += features[12] * 0.10;
        totalWeight += 0.10;
        
        // Normalize score
        if (totalWeight > 0) {
            score = score / totalWeight;
        }
        
        // Classify
        EnergyLevel level;
        double confidence;
        if (score >= 0.65) {
            level = EnergyLevel.HIGH;
            confidence = Math.min(1.0, (score - 0.65) / 0.35);
        } else if (score >= 0.35) {
            level = EnergyLevel.MEDIUM;
            confidence = Math.min(1.0, 1.0 - Math.abs(score - 0.5) / 0.15);
        } else {
            level = EnergyLevel.LOW;
            confidence = Math.min(1.0, (0.35 - score) / 0.35);
        }
        
        Log.d(TAG, String.format("Rule-based Prediction: %s (score: %.2f, confidence: %.2f)", 
            level, score, confidence));
        
        return new EnergyPrediction(targetTime, level, confidence, null, null);
    }
    
    // Helper methods for data filtering
    private List<BiometricData> getRecentData(List<BiometricData> data, Date targetTime, int hours) {
        long cutoff = targetTime.getTime() - (hours * 60 * 60 * 1000L);
        List<BiometricData> result = new ArrayList<>();
        for (BiometricData d : data) {
            if (d.getTimestamp().getTime() >= cutoff) {
                result.add(d);
            }
        }
        return result;
    }
    
    private List<TypingSpeedData> getRecentDataTyping(List<TypingSpeedData> data, Date targetTime, int hours, int targetHour) {
        long cutoff = targetTime.getTime() - (hours * 60 * 60 * 1000L);
        List<TypingSpeedData> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (TypingSpeedData d : data) {
            cal.setTime(d.getTimestamp());
            if (d.getTimestamp().getTime() >= cutoff && cal.get(Calendar.HOUR_OF_DAY) == targetHour) {
                result.add(d);
            }
        }
        return result;
    }
    
    private List<ReactionTimeData> getRecentDataReaction(List<ReactionTimeData> data, Date targetTime, int hours, int targetHour) {
        long cutoff = targetTime.getTime() - (hours * 60 * 60 * 1000L);
        List<ReactionTimeData> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (ReactionTimeData d : data) {
            cal.setTime(d.getTimestamp());
            if (d.getTimestamp().getTime() >= cutoff && cal.get(Calendar.HOUR_OF_DAY) == targetHour) {
                result.add(d);
            }
        }
        return result;
    }
    
    private List<EnergyPrediction> getPredictionsAtHour(List<EnergyPrediction> predictions, int hour) {
        List<EnergyPrediction> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        for (EnergyPrediction p : predictions) {
            cal.setTime(p.getTimestamp());
            if (cal.get(Calendar.HOUR_OF_DAY) == hour) {
                result.add(p);
            }
        }
        return result;
    }
    
    private List<EnergyPrediction> getRecentPredictions(List<EnergyPrediction> predictions, Date targetTime, int hours) {
        long cutoff = targetTime.getTime() - (hours * 60 * 60 * 1000L);
        List<EnergyPrediction> result = new ArrayList<>();
        for (EnergyPrediction p : predictions) {
            if (p.getTimestamp().getTime() >= cutoff) {
                result.add(p);
            }
        }
        return result;
    }
    
    /**
     * Clean up resources
     */
    public void close() {
        if (tflite != null) {
            tflite.close();
            tflite = null;
        }
        modelLoaded = false;
    }
}

