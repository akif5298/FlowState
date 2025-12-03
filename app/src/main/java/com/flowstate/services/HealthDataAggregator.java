package com.flowstate.services;

import android.content.Context;
import android.util.Log;
import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.dao.HrDao;
import com.flowstate.data.local.dao.HrvDao;
import com.flowstate.data.local.dao.StepsDao;
import com.flowstate.data.local.dao.SleepDao;
import com.flowstate.data.local.dao.WorkoutDao;
import com.flowstate.data.local.dao.TypingDao;
import com.flowstate.data.local.dao.ReactionDao;
import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.data.local.entities.HrvLocal;
import com.flowstate.data.local.entities.StepsLocal;
import com.flowstate.data.local.entities.SleepLocal;
import com.flowstate.data.local.entities.WorkoutLocal;
import com.flowstate.data.local.entities.TypingLocal;
import com.flowstate.data.local.entities.ReactionLocal;
import com.flowstate.data.local.entities.ManualEnergyInputLocal;
import com.flowstate.data.local.entities.ScheduleLocal;
import com.flowstate.data.local.entities.CaffeineIntakeLocal;
import com.flowstate.data.local.entities.EmotionLocal;
import com.flowstate.data.local.entities.DeviceUsageLocal;
import com.flowstate.data.local.entities.WeatherLocal;
import com.flowstate.app.data.models.HealthDataSummary;

import com.flowstate.data.local.entities.PredictionLocal;

import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Aggregates health data from local database for energy prediction
 * Collects and formats data from Health Connect (stored locally) for API requests
 */
public class HealthDataAggregator {
    
    private static final String TAG = "HealthDataAggregator";
    
    private final AppDb database;
    private final HealthConnectManager healthConnectManager;
    
    // Default context length (48 hours) - standard time series context window
    private static final int DEFAULT_CONTEXT_HOURS = 48;
    
    public HealthDataAggregator(Context context) {
        this.database = AppDb.getInstance(context);
        this.healthConnectManager = new HealthConnectManager(context);
    }
    
    /**
     * Collects health data from the last N hours and formats it for prediction API
     * 
     * @param hours Number of hours of historical data to collect (default: 48)
     * @return Map of feature names to lists of values, ready for API request
     */
    public Map<String, List<Double>> collectHealthData(int hours) {
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        
        long startMs = start.toEpochMilli();
        long endMs = end.toEpochMilli();
        
        Map<String, List<Double>> data = new HashMap<>();
        
        try {
            // Collect heart rate data
            List<HrLocal> hrRecords = database.hrDao().getByDateRange(startMs, endMs);
            if (!hrRecords.isEmpty()) {
                List<Double> heartRates = new ArrayList<>();
                for (HrLocal hr : hrRecords) {
                    heartRates.add((double) hr.bpm);
                }
                data.put("heart_rate", heartRates);
                Log.d(TAG, "Collected " + heartRates.size() + " heart rate readings");
            }
            
            // Collect HRV data
            List<HrvLocal> hrvRecords = database.hrvDao().getByDateRange(startMs, endMs);
            if (!hrvRecords.isEmpty()) {
                List<Double> hrvValues = new ArrayList<>();
                for (HrvLocal hrv : hrvRecords) {
                    hrvValues.add(hrv.rmssd);
                }
                data.put("hrv", hrvValues);
                Log.d(TAG, "Collected " + hrvValues.size() + " HRV readings");
            }
            
            // Collect steps data (aggregate by hour)
            List<StepsLocal> stepsRecords = database.stepsDao().getByDateRange(startMs, endMs);
            if (!stepsRecords.isEmpty()) {
                // Aggregate steps by hour
                Map<Long, Integer> hourlySteps = new HashMap<>();
                for (StepsLocal steps : stepsRecords) {
                    long hourKey = (steps.start_time / (1000 * 60 * 60)) * (1000 * 60 * 60);
                    hourlySteps.put(hourKey, hourlySteps.getOrDefault(hourKey, 0) + steps.count);
                }
                
                List<Double> stepsList = new ArrayList<>();
                for (long hour = startMs; hour < endMs; hour += (1000 * 60 * 60)) {
                    stepsList.add((double) hourlySteps.getOrDefault(hour, 0));
                }
                data.put("steps", stepsList);
                Log.d(TAG, "Collected " + stepsList.size() + " hourly step counts");
            }
            
            // Collect sleep data (hours slept per day for the last 7 days)
            // Aggregated by day so the bar chart shows one bar per day
            List<SleepLocal> sleepRecords = database.sleepDao().getAll();
            Log.d(TAG, "Total sleep records in DB: " + sleepRecords.size());
            
            // Use Calendar for proper local timezone day boundaries
            Calendar cal = Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            long todayMidnight = cal.getTimeInMillis();
            long oneDayMs = 24 * 60 * 60 * 1000L;
            
            // Create array for 7 days of sleep data (index 0 = 6 days ago, index 6 = today)
            double[] dailySleep = new double[7];
            
            // Calculate the start of 7 days ago
            long sevenDaysAgoMidnight = todayMidnight - (6 * oneDayMs);
            
            // Aggregate sleep sessions into their respective days
            if (!sleepRecords.isEmpty()) {
                for (SleepLocal sleep : sleepRecords) {
                    if (sleep.duration != null && sleep.duration > 0) {
                        // Use sleep_end to determine which day this sleep belongs to
                        long sleepEndMs = sleep.sleep_end != null ? sleep.sleep_end : sleep.sleep_start + (sleep.duration * 60 * 1000L);
                        
                        // Calculate which day index this belongs to (0-6)
                        int dayIndex = (int) ((sleepEndMs - sevenDaysAgoMidnight) / oneDayMs);
                        
                        Log.d(TAG, "Sleep record: duration=" + sleep.duration + "min, sleepEnd=" + sleepEndMs + ", dayIndex=" + dayIndex);
                        
                        if (dayIndex >= 0 && dayIndex < 7) {
                            double hoursSlept = sleep.duration / 60.0;
                            dailySleep[dayIndex] += hoursSlept;
                            Log.d(TAG, "Added " + hoursSlept + " hours to day " + dayIndex);
                        }
                    }
                }
            }
            
            // Convert to list for chart
            List<Double> sleepHours = new ArrayList<>();
            for (int i = 0; i < 7; i++) {
                sleepHours.add(dailySleep[i]);
            }
            data.put("sleep_hours", sleepHours);
            Log.d(TAG, "Sleep hours for last 7 days: " + sleepHours);
            
            // Collect Typing Speed data
            List<TypingLocal> typingRecords = database.typingDao().getByDateRange(startMs, endMs);
            if (!typingRecords.isEmpty()) {
                List<Double> typingWpm = new ArrayList<>();
                for (TypingLocal typing : typingRecords) {
                    typingWpm.add((double) typing.wpm);
                }
                data.put("typing_wpm", typingWpm);
                Log.d(TAG, "Collected " + typingWpm.size() + " typing tests");
            }

            // Collect Reaction Time data
            List<ReactionLocal> reactionRecords = database.reactionDao().getByDateRange(startMs, endMs);
            if (!reactionRecords.isEmpty()) {
                List<Double> reactionTimes = new ArrayList<>();
                for (ReactionLocal reaction : reactionRecords) {
                    reactionTimes.add((double) reaction.medianMs);
                }
                data.put("reaction_time", reactionTimes);
                Log.d(TAG, "Collected " + reactionTimes.size() + " reaction tests");
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error collecting health data", e);
        }
        
        return data;
    }
    
    /**
     * Collects health data using default context length (48 hours)
     */
    public Map<String, List<Double>> collectHealthData() {
        return collectHealthData(DEFAULT_CONTEXT_HOURS);
    }
    
    /**
     * Ensures all data arrays have the same length by padding with last known values
     * This is important for the prediction model which expects aligned time series
     */
    public Map<String, List<Double>> alignTimeSeries(Map<String, List<Double>> data) {
        if (data.isEmpty()) {
            return data;
        }
        
        // Find the maximum length
        int maxLength = 0;
        for (List<Double> values : data.values()) {
            if (values.size() > maxLength) {
                maxLength = values.size();
            }
        }
        
        // Pad shorter series with their last value (or 0 if empty)
        Map<String, List<Double>> aligned = new HashMap<>();
        for (Map.Entry<String, List<Double>> entry : data.entrySet()) {
            List<Double> values = new ArrayList<>(entry.getValue());
            if (values.isEmpty()) {
                // Fill with zeros if no data
                for (int i = 0; i < maxLength; i++) {
                    values.add(0.0);
                }
            } else {
                // Pad with last known value
                double lastValue = values.get(values.size() - 1);
                while (values.size() < maxLength) {
                    values.add(lastValue);
                }
            }
            aligned.put(entry.getKey(), values);
        }
        
        return aligned;
    }
    
    /**
     * Gets the primary time series (heart rate if available, otherwise first available)
     * Used as the main history for HuggingFace API format
     * Heart rate is preferred as it's a good proxy for energy levels
     */
    public List<Double> getPrimaryTimeSeries(Map<String, List<Double>> data) {
        if (data == null || data.isEmpty()) {
            return new ArrayList<>();
        }
        
        // Prefer heart_rate as primary series
        if (data.containsKey("heart_rate")) {
            List<Double> hr = data.get("heart_rate");
            if (hr != null && !hr.isEmpty()) {
                return hr;
            }
        }
        
        // Return first non-empty series
        for (List<Double> values : data.values()) {
            if (values != null && !values.isEmpty()) {
                return values;
            }
        }
        
        // Return empty list if no data
        return new ArrayList<>();
    }
    
    /**
     * Creates a structured health data summary for Gemini API
     * Aggregates raw data into meaningful metrics with appropriate weighting
     * 
     * @param hours Number of hours of data to analyze (default: 48)
     * @return HealthDataSummary with aggregated metrics
     */
    public HealthDataSummary createHealthSummary(int hours) {
        Instant end = Instant.now();
        Instant start = end.minus(hours, ChronoUnit.HOURS);
        Instant recentStart = end.minus(6, ChronoUnit.HOURS); // Last 6 hours for "recent" data
        
        long startMs = start.toEpochMilli();
        long endMs = end.toEpochMilli();
        long recentStartMs = recentStart.toEpochMilli();
        
        HealthDataSummary summary = new HealthDataSummary();
        
        try {
            // Get current time context
            Calendar cal = Calendar.getInstance();
            int currentHour = cal.get(Calendar.HOUR_OF_DAY);
            summary.setCurrentHour(currentHour);
            
            String timeOfDay;
            if (currentHour >= 5 && currentHour < 12) {
                timeOfDay = "morning";
            } else if (currentHour >= 12 && currentHour < 17) {
                timeOfDay = "afternoon";
            } else if (currentHour >= 17 && currentHour < 21) {
                timeOfDay = "evening";
            } else {
                timeOfDay = "night";
            }
            summary.setTimeOfDay(timeOfDay);
            
            // Heart Rate Analysis (highly weighted for energy prediction)
            List<HrLocal> hrRecords = database.hrDao().getByDateRange(startMs, endMs);
            if (!hrRecords.isEmpty()) {
                List<HrLocal> recentHr = hrRecords.stream()
                    .filter(hr -> hr.timestamp >= recentStartMs)
                    .collect(Collectors.toList());
                
                // Calculate averages
                double avgHR = hrRecords.stream()
                    .mapToInt(hr -> hr.bpm)
                    .average()
                    .orElse(0.0);
                
                // Current HR (most recent)
                double currentHR = hrRecords.get(hrRecords.size() - 1).bpm;
                
                // Resting HR (lowest 10th percentile, typically during sleep hours)
                List<Integer> sortedHR = hrRecords.stream()
                    .map(hr -> hr.bpm)
                    .sorted()
                    .collect(Collectors.toList());
                double restingHR = sortedHR.size() > 10 
                    ? sortedHR.get(sortedHR.size() / 10) 
                    : sortedHR.isEmpty() ? 0 : sortedHR.get(0);
                
                summary.setAvgHeartRate(avgHR);
                summary.setCurrentHeartRate(currentHR);
                summary.setRestingHeartRate(restingHR);
                summary.setHeartRateReadings(hrRecords.size());
            }
            
            // HRV Analysis (very important for recovery/energy)
            List<HrvLocal> hrvRecords = database.hrvDao().getByDateRange(startMs, endMs);
            if (!hrvRecords.isEmpty()) {
                double avgHRV = hrvRecords.stream()
                    .mapToDouble(hrv -> hrv.rmssd)
                    .average()
                    .orElse(0.0);
                
                double currentHRV = hrvRecords.get(hrvRecords.size() - 1).rmssd;
                
                summary.setAvgHRV(avgHRV);
                summary.setCurrentHRV(currentHRV);
                summary.setHrvReadings(hrvRecords.size());
            }
            
            // Sleep Analysis (critical for energy)
            List<SleepLocal> allSleep = database.sleepDao().getAll();
            List<SleepLocal> recentSleep = allSleep.stream()
                .filter(sleep -> sleep.sleep_start >= startMs && sleep.sleep_start < endMs)
                .collect(Collectors.toList());
            
            if (!recentSleep.isEmpty()) {
                // Last night's sleep (most recent completed sleep session)
                SleepLocal lastSleep = recentSleep.stream()
                    .filter(sleep -> sleep.sleep_end < endMs)
                    .max((s1, s2) -> Long.compare(s1.sleep_end, s2.sleep_end))
                    .orElse(null);
                
                if (lastSleep != null) {
                    double lastNightHours = lastSleep.duration / 60.0;
                    summary.setLastNightSleepHours(lastNightHours);
                    if (lastSleep.sleep_end != null) {
                        summary.setLastWakeTimestamp(lastSleep.sleep_end);
                    }
                }
                
                // Average sleep
                double avgSleep = recentSleep.stream()
                    .mapToDouble(sleep -> sleep.duration / 60.0)
                    .average()
                    .orElse(0.0);
                summary.setAvgSleepHours(avgSleep);
                
                // Sleep quality estimate (7-9 hours = good, <6 or >10 = poor)
                if (lastSleep != null) {
                    double sleepHours = lastSleep.duration / 60.0;
                    double quality;
                    if (sleepHours >= 7 && sleepHours <= 9) {
                        quality = 0.8; // Good
                    } else if (sleepHours >= 6 && sleepHours < 7) {
                        quality = 0.6; // Fair
                    } else if (sleepHours > 9 && sleepHours <= 10) {
                        quality = 0.7; // Fair
                    } else {
                        quality = 0.4; // Poor
                    }
                    summary.setSleepQuality(quality);
                }
                
                summary.setSleepSessions(recentSleep.size());
            }
            
            // Steps/Activity Analysis
            List<StepsLocal> stepsRecords = database.stepsDao().getByDateRange(startMs, endMs);
            if (!stepsRecords.isEmpty()) {
                // Today's steps (last 24 hours)
                Instant todayStart = end.minus(24, ChronoUnit.HOURS);
                long todayStartMs = todayStart.toEpochMilli();
                int todaySteps = stepsRecords.stream()
                    .filter(steps -> steps.start_time >= todayStartMs)
                    .mapToInt(steps -> steps.count)
                    .sum();
                summary.setTodaySteps(todaySteps);
                
                // Average daily steps
                Map<Long, Integer> dailySteps = new HashMap<>();
                for (StepsLocal steps : stepsRecords) {
                    long dayKey = (steps.start_time / (1000 * 60 * 60 * 24)) * (1000 * 60 * 60 * 24);
                    dailySteps.put(dayKey, dailySteps.getOrDefault(dayKey, 0) + steps.count);
                }
                double avgDaily = dailySteps.values().stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
                summary.setAvgDailySteps((int) avgDaily);
                
                // Activity level (0-1 scale based on steps relative to 10k goal)
                double activityLevel = Math.min(1.0, todaySteps / 10000.0);
                summary.setActivityLevel(activityLevel);
            }
            
            // Collect additional activity metrics
            try {
                Double activeCalories = healthConnectManager.readActiveCalories(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (activeCalories > 0) summary.setActiveCaloriesBurned(activeCalories);
            } catch (Exception e) {
                Log.d(TAG, "Active calories not available", e);
            }
            
            try {
                Double totalCalories = healthConnectManager.readTotalCalories(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (totalCalories > 0) summary.setTotalCaloriesBurned(totalCalories);
            } catch (Exception e) {
                Log.d(TAG, "Total calories not available", e);
            }
            
            try {
                Double distance = healthConnectManager.readDistance(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (distance > 0) summary.setDistance(distance);
            } catch (Exception e) {
                Log.d(TAG, "Distance not available", e);
            }
            
            try {
                Double elevation = healthConnectManager.readElevationGained(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (elevation > 0) summary.setElevationGained(elevation);
            } catch (Exception e) {
                Log.d(TAG, "Elevation not available", e);
            }
            
            try {
                Integer floors = healthConnectManager.readFloorsClimbed(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (floors > 0) summary.setFloorsClimbed(floors);
            } catch (Exception e) {
                Log.d(TAG, "Floors climbed not available", e);
            }
            
            // Collect vitals
            try {
                Double restingHR = healthConnectManager.readRestingHeartRate(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (restingHR != null) {
                    summary.setRestingHeartRate(restingHR);
                }
            } catch (Exception e) {
                Log.d(TAG, "Resting heart rate not available", e);
            }
            
            try {
                java.util.concurrent.CompletableFuture<kotlin.Pair<Double, Double>> bpFuture = 
                    healthConnectManager.readBloodPressure(start, end);
                kotlin.Pair<Double, Double> bp = bpFuture.get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (bp != null && bp.getFirst() != null && bp.getSecond() != null) {
                    summary.setBloodPressureSystolic(bp.getFirst());
                    summary.setBloodPressureDiastolic(bp.getSecond());
                }
            } catch (Exception e) {
                Log.d(TAG, "Blood pressure not available", e);
            }
            
            try {
                Double oxygenSat = healthConnectManager.readOxygenSaturation(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (oxygenSat != null) summary.setOxygenSaturation(oxygenSat);
            } catch (Exception e) {
                Log.d(TAG, "Oxygen saturation not available", e);
            }
            
            try {
                Double respRate = healthConnectManager.readRespiratoryRate(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (respRate != null) summary.setRespiratoryRate(respRate);
            } catch (Exception e) {
                Log.d(TAG, "Respiratory rate not available", e);
            }
            
            // Collect body measurements
            try {
                Double weight = healthConnectManager.readWeight(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (weight != null) summary.setWeight(weight);
            } catch (Exception e) {
                Log.d(TAG, "Weight not available", e);
            }
            
            try {
                Double height = healthConnectManager.readHeight(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (height != null) summary.setHeight(height);
            } catch (Exception e) {
                Log.d(TAG, "Height not available", e);
            }
            
            // Collect nutrition & hydration
            try {
                Double hydration = healthConnectManager.readHydration(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (hydration > 0) summary.setHydration(hydration);
            } catch (Exception e) {
                Log.d(TAG, "Hydration not available", e);
            }
            
            // Collect wellness
            try {
                Double mindfulness = healthConnectManager.readMindfulness(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (mindfulness > 0) summary.setMindfulnessMinutes(mindfulness);
            } catch (Exception e) {
                Log.d(TAG, "Mindfulness not available", e);
            }
            
            // Collect blood glucose
            try {
                Double glucose = healthConnectManager.readBloodGlucose(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (glucose != null) summary.setBloodGlucose(glucose);
            } catch (Exception e) {
                Log.d(TAG, "Blood glucose not available", e);
            }
            
            // Collect VO2 max
            try {
                Double vo2Max = healthConnectManager.readVo2Max(start, end).get(10, java.util.concurrent.TimeUnit.SECONDS);
                if (vo2Max != null) summary.setVo2Max(vo2Max);
            } catch (Exception e) {
                Log.d(TAG, "VO2 max not available", e);
            }
            
            // Exercise details (from existing workout data)
            List<WorkoutLocal> workouts = database.workoutDao().getByDateRange(startMs, endMs);
            if (!workouts.isEmpty()) {
                summary.setExerciseSessions(workouts.size());
                double avgDuration = workouts.stream()
                    .mapToInt(w -> w.duration_minutes)
                    .average()
                    .orElse(0.0);
                summary.setAvgExerciseDuration(avgDuration);
                
                // Collect exercise types
                String types = workouts.stream()
                    .map(w -> w.type)
                    .distinct()
                    .collect(java.util.stream.Collectors.joining(", "));
                summary.setExerciseTypes(types);
            }
            
            // App Test Results - Typing Tests
            List<TypingLocal> typingTests = database.typingDao().getByDateRange(startMs, endMs);
            if (!typingTests.isEmpty()) {
                // Get most recent typing test
                TypingLocal recentTyping = typingTests.get(typingTests.size() - 1);
                summary.setRecentTypingWPM((double) recentTyping.wpm);
                summary.setRecentTypingAccuracy(recentTyping.accuracy);
                summary.setTypingTestsCount(typingTests.size());
            }
            
            // App Test Results - Reaction Time Tests
            List<ReactionLocal> reactionTests = database.reactionDao().getByDateRange(startMs, endMs);
            if (!reactionTests.isEmpty()) {
                // Get most recent reaction test
                ReactionLocal recentReaction = reactionTests.get(reactionTests.size() - 1);
                summary.setRecentReactionTimeMs(recentReaction.medianMs);
                summary.setReactionTestsCount(reactionTests.size());
            }
            
            
            // Manual Energy Inputs (Smart Aggregation with varying lifespans)
            // We fetch the last 10 hours of inputs to find the most relevant valid entry for each field
            // 1. General feelings (Energy, Tiredness) -> valid for ~2-3 hours
            // 2. Caffeine -> valid for ~10 hours (as requested)
            // 3. Meal -> valid for ~2 hours
            
            long validWindowStart = endMs - (10 * 60 * 60 * 1000); // 10 hours ago
            List<ManualEnergyInputLocal> recentInputs = database.manualEnergyInputDao().getByDateRange(validWindowStart, endMs);
            
            if (!recentInputs.isEmpty()) {
                long now = System.currentTimeMillis();
                
                // Iterate from newest to oldest to find first non-null value for each field
                for (ManualEnergyInputLocal input : recentInputs) {
                    double hoursAgo = (now - input.timestamp) / (1000.0 * 60 * 60.0);
                    
                    // General Feelings (2 hour lifespan)
                    if (hoursAgo <= 2.0) {
                        if (summary.getManualEnergyLevel() == null && input.energyLevel != null) 
                            summary.setManualEnergyLevel(input.energyLevel);
                        if (summary.getPhysicalTiredness() == null && input.physicalTiredness != null) 
                            summary.setPhysicalTiredness(input.physicalTiredness);
                        if (summary.getMentalTiredness() == null && input.mentalTiredness != null) 
                            summary.setMentalTiredness(input.mentalTiredness);
                        if (summary.getMealImpact() == null && input.mealImpact != null) 
                            summary.setMealImpact(input.mealImpact);
                        if (summary.getCurrentEmotion() == null && input.currentEmotion != null) 
                            summary.setCurrentEmotion(input.currentEmotion);
                        if (summary.getRecentTask() == null && input.recentTask != null) 
                            summary.setRecentTask(input.recentTask);
                        if (summary.getPredictionAccuracyRating() == null && input.predictionAccuracyRating != null)
                            summary.setPredictionAccuracyRating(input.predictionAccuracyRating);
                            
                        // Update timestamp only if we used a general feeling (closest to "now")
                        if (summary.getLastManualInputTimestamp() == null) 
                            summary.setLastManualInputTimestamp(input.timestamp);
                    }
                    
                    // Caffeine (10 hour lifespan)
                    // We sum up caffeine if multiple entries exist? Or just take the latest valid report?
                    // User said "im saying im drinking x amount... or leave not filled in".
                    // Let's take the most recent non-null entry within 10 hours.
                    if (hoursAgo <= 10.0) {
                        if (summary.getCaffeineIntakeCups() == null && input.caffeineIntake != null) {
                            summary.setCaffeineIntakeCups(input.caffeineIntake);
                            // Log the time of this specific caffeine intake if needed, but summary only has one timestamp
                        }
                    }
                }
            }
            
            // Schedule & Tasks (from database)
            Calendar calToday = Calendar.getInstance();
            calToday.set(Calendar.HOUR_OF_DAY, 0);
            calToday.set(Calendar.MINUTE, 0);
            calToday.set(Calendar.SECOND, 0);
            calToday.set(Calendar.MILLISECOND, 0);
            long todayStart = calToday.getTimeInMillis();
            
            ScheduleLocal todaySchedule = database.scheduleDao().getByDate(todayStart);
            if (todaySchedule != null) {
                if (todaySchedule.scheduleForDay != null && !todaySchedule.scheduleForDay.isEmpty()) {
                    summary.setScheduleForDay(todaySchedule.scheduleForDay);
                }
                if (todaySchedule.completedTasks != null && !todaySchedule.completedTasks.isEmpty()) {
                    summary.setCompletedTasks(todaySchedule.completedTasks);
                }
                if (todaySchedule.upcomingTasks != null && !todaySchedule.upcomingTasks.isEmpty()) {
                    summary.setUpcomingTasks(todaySchedule.upcomingTasks);
                }
                if (todaySchedule.mealTimes != null && !todaySchedule.mealTimes.isEmpty()) {
                    summary.setMealTimes(todaySchedule.mealTimes);
                }
            }
            
            // Caffeine Intake (from manual input now, deprecated table query below if needed)
            /* 
            // Previous caffeine logic from deprecated table
            long todayEnd = todayStart + (24 * 60 * 60 * 1000);
            Double todayCaffeine = database.caffeineIntakeDao().getTotalForDate(todayStart, todayEnd);
            if (todayCaffeine != null && todayCaffeine > 0) {
                summary.setCaffeineIntakeMg(todayCaffeine);
            }
            */
            
            // Current Emotion (from database - most recent)
            EmotionLocal recentEmotion = database.emotionDao().getMostRecent();
            if (recentEmotion != null && recentEmotion.timestamp >= recentStartMs) {
                if (recentEmotion.emotion != null && !recentEmotion.emotion.isEmpty()) {
                    summary.setCurrentEmotion(recentEmotion.emotion);
                }
            }
            
            // Device Usage (from database - today)
            DeviceUsageLocal todayUsage = database.deviceUsageDao().getByDate(todayStart);
            if (todayUsage != null) {
                summary.setDeviceScreenTimeMinutes(todayUsage.screenTimeMinutes);
            }
            
            // Weather (from database - most recent)
            WeatherLocal recentWeather = database.weatherDao().getMostRecent();
            if (recentWeather != null) {
                if (recentWeather.condition != null) {
                    summary.setWeatherCondition(recentWeather.condition);
                }
                if (recentWeather.temperatureCelsius != null) {
                    summary.setWeatherTemperature(recentWeather.temperatureCelsius);
                }
                if (recentWeather.season != null) {
                    summary.setSeason(recentWeather.season);
                }
            }
            
            // Data quality indicators
            summary.setHoursOfData(hours);
            summary.setHasRecentData(
                (!hrRecords.isEmpty() && hrRecords.stream().anyMatch(hr -> hr.timestamp >= recentStartMs)) ||
                (!hrvRecords.isEmpty() && hrvRecords.stream().anyMatch(hrv -> hrv.timestamp >= recentStartMs))
            );
            
            // Previous Prediction (Consistency Check)
            PredictionLocal lastPrediction = database.predictionDao().getLatest();
            if (lastPrediction != null) {
                 summary.setLastPredictedLevel(lastPrediction.predictedLevel);
                 summary.setLastPredictionTime(lastPrediction.predictionTime);
                 summary.setLastPredictionExplanation(lastPrediction.explanation);
            }
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating health summary", e);
        }
        
        return summary;
    }
    
    /**
     * Creates health summary with default 48 hours of data
     */
    public HealthDataSummary createHealthSummary() {
        return createHealthSummary(DEFAULT_CONTEXT_HOURS);
    }
}

