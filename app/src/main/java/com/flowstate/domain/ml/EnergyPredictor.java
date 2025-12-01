package com.personaleenergy.domain.ml;

import android.util.Log;
import com.personaleenergy.data.local.entities.PredictionLocal;
import com.personaleenergy.domain.features.FeatureRow;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

/**
 * Energy level predictor (baseline v0)
 * 
 * Predicts energy levels based on weighted sum of features
 */
public class EnergyPredictor {
    
    private static final String TAG = "EnergyPredictor";
    
    // Weights for weighted sum formula
    private static final double W1_SLEEP_QUALITY = 30.0;
    private static final double W2_CIRCADIAN = 25.0;
    private static final double W3_HR_TREND = 20.0;
    private static final double W4_WPM_DELTA = 15.0;
    private static final double W5_REACTION_DELTA = 10.0;
    
    // Classification thresholds
    private static final int HIGH_THRESHOLD = 66;
    private static final int MEDIUM_THRESHOLD = 33;
    
    // Confidence calculation
    private static final double THRESHOLD_DISTANCE = 33.0; // Distance between thresholds
    
    /**
     * Predict energy levels for a given date based on features
     * 
     * @param date LocalDate to predict for
     * @param feats List of FeatureRow for the date
     * @return List of PredictionLocal entities
     */
    public List<PredictionLocal> predict(LocalDate date, List<FeatureRow> feats) {
        Log.d(TAG, "Predicting energy levels for " + date + " with " + feats.size() + " feature rows");
        
        List<PredictionLocal> predictions = new ArrayList<>();
        
        // Compute HR baseline for z-score calculation
        double hrBaseline = computeHrBaseline(feats);
        double hrStdDev = computeHrStdDev(feats, hrBaseline);
        
        for (FeatureRow feat : feats) {
            // Compute weighted sum score
            double score = computeScore(feat, hrBaseline, hrStdDev);
            
            // Classify energy level
            String level = classify(score);
            
            // Compute confidence
            double confidence = computeConfidence(score);
            
            // Create prediction
            PredictionLocal prediction = new PredictionLocal(
                feat.slotStart,
                level,
                confidence
            );
            
            predictions.add(prediction);
        }
        
        Log.d(TAG, "Generated " + predictions.size() + " predictions for " + date);
        return predictions;
    }
    
    /**
     * Compute weighted sum score
     * Score = w1*sleepQuality + w2*circadian + w3*(hrTrendZ) + w4*wpmDelta - w5*reactionDelta
     */
    private double computeScore(FeatureRow feat, double hrBaseline, double hrStdDev) {
        // w1 * sleepQuality (0-1 -> 0-30)
        double sleepComponent = W1_SLEEP_QUALITY * feat.sleepQuality;
        
        // w2 * circadian (time-of-day effect)
        // Use cosine component as it peaks at noon (highest energy)
        // Normalize cosTOD from [-1, 1] to [0, 1] for positive contribution
        double circadianComponent = W2_CIRCADIAN * ((feat.cosTOD + 1.0) / 2.0);
        
        // w3 * hrTrendZ (z-score of HR mean relative to day's baseline)
        // Higher HR might indicate higher energy (if in normal range)
        // Compute z-score: (hrMean - hrBaseline) / hrStdDev
        double hrZ = 0.0;
        if (hrStdDev > 0) {
            hrZ = (feat.hrMean - hrBaseline) / hrStdDev;
        }
        // Normalize z-score to [0, 1] range (assuming HR increases with energy)
        // Clamp z-score to reasonable range [-2, 2] -> [0, 1]
        double hrZNormalized = Math.max(0.0, Math.min(1.0, (hrZ + 2.0) / 4.0));
        double hrComponent = W3_HR_TREND * hrZNormalized;
        
        // w4 * wpmDelta (positive delta = higher performance = higher energy)
        // Normalize wpmDelta to [0, 1] range
        // Assuming reasonable range: -50 to +50 WPM
        double wpmDeltaNormalized = Math.max(0.0, Math.min(1.0, (feat.wpmDelta + 50.0) / 100.0));
        double wpmComponent = W4_WPM_DELTA * wpmDeltaNormalized;
        
        // -w5 * reactionDelta (negative delta = faster reaction = higher energy)
        // Normalize reactionDelta to [0, 1] range
        // Lower reaction time = higher energy, so invert: (100 - reactionDelta) / 100
        // Assuming reasonable range: -100ms to +100ms delta
        double reactionDeltaNormalized = Math.max(0.0, Math.min(1.0, (100.0 - feat.reactionDelta) / 200.0));
        double reactionComponent = W5_REACTION_DELTA * reactionDeltaNormalized;
        
        // Sum all components
        double score = sleepComponent + circadianComponent + hrComponent + wpmComponent + reactionComponent;
        
        // Clamp score to [0, 100]
        score = Math.max(0.0, Math.min(100.0, score));
        
        return score;
    }
    
    /**
     * Classify energy level based on score
     * HIGH (≥66), MEDIUM (33–65), LOW (≤32)
     */
    private String classify(double score) {
        if (score >= HIGH_THRESHOLD) {
            return "HIGH";
        } else if (score >= MEDIUM_THRESHOLD) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
    
    /**
     * Compute confidence based on distance to nearest threshold
     * Confidence = distance to nearest threshold / 33 (clamp 0..1)
     */
    private double computeConfidence(double score) {
        // Find distance to nearest threshold
        double distToHigh = Math.abs(score - HIGH_THRESHOLD);
        double distToMedium = Math.abs(score - MEDIUM_THRESHOLD);
        double minDistance = Math.min(distToHigh, distToMedium);
        
        // If score is exactly on a threshold, confidence is 0
        if (minDistance < 0.1) {
            return 0.0;
        }
        
        // Confidence = distance / 33, clamped to [0, 1]
        double confidence = minDistance / THRESHOLD_DISTANCE;
        return Math.max(0.0, Math.min(1.0, confidence));
    }
    
    /**
     * Compute HR baseline (mean) for the day
     */
    private double computeHrBaseline(List<FeatureRow> feats) {
        if (feats.isEmpty()) {
            return 70.0; // Default resting HR
        }
        
        double sum = 0.0;
        int count = 0;
        
        for (FeatureRow feat : feats) {
            if (feat.hrMean > 0) {
                sum += feat.hrMean;
                count++;
            }
        }
        
        return count > 0 ? sum / count : 70.0;
    }
    
    /**
     * Compute HR standard deviation for the day
     */
    private double computeHrStdDev(List<FeatureRow> feats, double baseline) {
        if (feats.isEmpty()) {
            return 10.0; // Default std dev
        }
        
        double sumSquaredDiff = 0.0;
        int count = 0;
        
        for (FeatureRow feat : feats) {
            if (feat.hrMean > 0) {
                double diff = feat.hrMean - baseline;
                sumSquaredDiff += diff * diff;
                count++;
            }
        }
        
        if (count == 0) {
            return 10.0;
        }
        
        double variance = sumSquaredDiff / count;
        return Math.sqrt(variance);
    }
}
