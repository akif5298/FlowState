package com.flowstate.domain.ml;

import android.util.Log;
import com.flowstate.data.local.entities.PredictionLocal;
import com.flowstate.domain.features.FeatureRow;

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
    private static final double W6_HRV = 15.0;
    private static final double W7_ACTIVITY = 10.0;
    private static final double W8_SCREEN_FATIGUE = 10.0; // Negative impact
    
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
            
            // Generate detailed explanation
            String explanation = generateDetailedExplanation(score, feat, hrBaseline);
            
            // Classify energy level
            String level = classify(score);
            
            // Compute confidence
            double confidence = computeConfidence(score);
            
            // Create prediction
            PredictionLocal prediction = new PredictionLocal(
                feat.slotStart,
                score, // predictedLevel
                explanation, // explanation
                confidence
            );
            
            predictions.add(prediction);
        }
        
        Log.d(TAG, "Generated " + predictions.size() + " predictions for " + date);
        return predictions;
    }
    
    private String generateDetailedExplanation(double score, FeatureRow feat, double hrBaseline) {
        StringBuilder sb = new StringBuilder();
        
        // Base assessment
        if (score >= HIGH_THRESHOLD) {
            sb.append("High energy predicted (").append(String.format("%.0f", score)).append("). ");
        } else if (score >= MEDIUM_THRESHOLD) {
            sb.append("Moderate energy predicted (").append(String.format("%.0f", score)).append("). ");
        } else {
            sb.append("Low energy predicted (").append(String.format("%.0f", score)).append("). ");
        }
        
        // Explain key factors
        boolean hasFactor = false;
        
        // Sleep
        if (feat.sleepQuality > 0.8) {
            sb.append("Good sleep quality boosting energy. ");
            hasFactor = true;
        } else if (feat.sleepQuality < 0.5) {
            sb.append("Poor sleep may be reducing energy. ");
            hasFactor = true;
        }
        
        // Circadian
        if (feat.cosTOD > 0.5) {
            sb.append("Mid-day peak hours alignment. ");
            hasFactor = true;
        }
        
        // Activity
        if (feat.stepsCount > 5000 || feat.workoutMinutes > 30) {
            sb.append("Activity levels contributing positively. ");
            hasFactor = true;
        }
        
        // HR
        if (feat.hrMean > hrBaseline + 15) {
            sb.append("High heart rate may indicate stress or intense activity. ");
            hasFactor = true;
        } else if (feat.hrMean < hrBaseline - 10) {
            sb.append("Low heart rate indicates resting state. ");
            hasFactor = true;
        }
        
        if (!hasFactor) {
            sb.append("Based on balanced daily metrics.");
        }
        
        return sb.toString();
    }
    
    /**
     * Compute weighted sum score
     * Score = w1*sleepQuality + w2*circadian + w3*(hrTrendZ) + w4*wpmDelta - w5*reactionDelta
     */
    private double computeScore(FeatureRow feat, double hrBaseline, double hrStdDev) {
        // w1 * sleepQuality (0-1 -> 0-30)
        double sleepComponent = W1_SLEEP_QUALITY * feat.sleepQuality;
        
        // w2 * circadian (time-of-day effect)
        // Use cosine component. We want peak energy around 2 PM (14:00) for average person, 
        // or later if wake time is late.
        // 14:00 is ~3.66 radians. cos(3.66) is negative.
        // Let's use a simpler hour-distance metric.
        // Peak at 14:00 (14.0).
        // Distance from 14.0: abs(currentHour - 14.0)
        // Max distance is 12 hours.
        // Score = 1.0 - (dist / 12.0)
        // We can approximate this with cos but shifted.
        // cos(t - peak) peaks at t=peak.
        // Peak at 14:00 -> fraction 14/24 ≈ 0.58.
        // Shift: - 0.58 * 2pi ≈ -3.66.
        // Formula: (cos(radians - 3.66) + 1) / 2
        
        // Let's use the radians from FeatureRow. 
        // Noon is PI. 2 PM is PI + (2/12)*PI = 7/6 PI ≈ 3.66.
        // We want peak at 3.66.
        // FeatureRow gives raw cosTOD (cos(t)). We can't just use that.
        // We need to re-calculate phase aligned circadian rhythm here or in FeatureService.
        // For robustness, let's calculate a custom circadian score here.
        
        // Re-calculate hour from slotStart to apply shift
        long time = feat.slotStart;
        int hour = java.time.LocalDateTime.ofInstant(java.time.Instant.ofEpochMilli(time), java.time.ZoneId.systemDefault()).getHour();
        
        // Peak energy target: 3 PM (15:00) to account for your late sleep (wake 10 AM)
        // Standard might be 1-2 PM. Let's aim for 15:00.
        double peakHour = 15.0;
        double dist = Math.abs(hour - peakHour);
        if (dist > 12) dist = 24 - dist; // Wrap around 24h
        
        // Normalize: 0 dist = 1.0, 12 dist = 0.0
        double circadianNormalized = 1.0 - (dist / 12.0);
        
        double circadianComponent = W2_CIRCADIAN * circadianNormalized;
        
        // w3 * hrTrendZ
        // Relax the standard deviation check. 72 vs 70 is noise.
        // Only penalize if HR is significantly higher (> 15 bpm above baseline)
        double hrZNormalized = 0.0; // Default neutral/good
        if (feat.hrMean > hrBaseline + 15.0) {
             // High HR penalty (stress?) -> Lower score?
             // Original logic: Higher HR -> Higher score (Activity).
             // But "Elevated heart rate activity detected" suggests it might be interpreted as stress in explanation?
             // Let's keep it as: Moderate HR elevation = Energy (Activity). Very high = Stress.
             // For 72 vs 70, it should be neutral/positive.
             hrZNormalized = 0.5; // Moderate boost
        } else if (feat.hrMean < hrBaseline - 10.0) {
             hrZNormalized = 0.2; // Low energy/sleepy
        } else {
             hrZNormalized = 0.4; // Baseline resting
        }
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
        
        // w6 * HRV (Higher HRV = better recovery/energy)
        // Normalize HRV (approx 20-100ms)
        double hrvNormalized;
        if (feat.hrvRmssd > 0) {
            hrvNormalized = Math.max(0.0, Math.min(1.0, (feat.hrvRmssd - 20.0) / 80.0));
        } else {
            hrvNormalized = 0.5; // Neutral score (50%) if missing. Better than 0 penalty.
        }
        double hrvComponent = W6_HRV * hrvNormalized;

        // w7 * Activity (Steps + Workout)
        // Normalize steps (0-10000) and workout (0-60 mins)
        double activityScore = (feat.stepsCount / 10000.0) + (feat.workoutMinutes / 60.0);
        double activityNormalized = Math.max(0.0, Math.min(1.0, activityScore));
        double activityComponent = W7_ACTIVITY * activityNormalized;

        // -w8 * Screen Fatigue (Higher screen time = lower energy)
        // Normalize screen time (0-8 hours/480 mins)
        double screenNormalized = Math.max(0.0, Math.min(1.0, feat.screenTimeMinutes / 480.0));
        double screenComponent = W8_SCREEN_FATIGUE * screenNormalized;

        // Sum all components (subtract fatigue)
        double score = sleepComponent + circadianComponent + hrComponent + wpmComponent + reactionComponent
                     + hrvComponent + activityComponent - screenComponent;
        
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

