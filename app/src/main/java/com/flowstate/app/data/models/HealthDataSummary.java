package com.flowstate.app.data.models;

import java.util.List;

/**
 * Summary of health data for energy prediction
 * Used to provide structured data to Gemini API
 */
public class HealthDataSummary {
    
    // Heart Rate metrics (most important for energy)
    private Double avgHeartRate;
    private Double currentHeartRate;
    private Double restingHeartRate;
    private Integer heartRateReadings;
    
    // HRV metrics (very important for recovery/energy)
    private Double avgHRV;
    private Double currentHRV;
    private Integer hrvReadings;
    
    // Sleep metrics (critical for energy)
    private Double lastNightSleepHours;
    private Long lastWakeTimestamp; // Epoch millis when user woke up
    private Double avgSleepHours;
    private Double sleepQuality; // 0-1 scale
    private Integer sleepSessions;
    
    // Activity metrics
    private Integer todaySteps;
    private Integer avgDailySteps;
    private Double activityLevel; // 0-1 scale
    
    // Time context
    private Integer currentHour; // 0-23
    private String timeOfDay; // "morning", "afternoon", "evening", "night"
    
    // Data quality
    private Integer hoursOfData; // How many hours of data available
    private Boolean hasRecentData; // Data from last 6 hours
    
    // Activity Metrics
    private Double activeCaloriesBurned; // kcal
    private Double totalCaloriesBurned; // kcal
    private Double activityIntensity; // 0-1 scale
    private Double distance; // meters
    private Double elevationGained; // meters
    private Integer floorsClimbed;
    private Double speed; // m/s
    private Double power; // watts
    private Double vo2Max; // ml/kg/min
    private Integer wheelchairPushes;
    private Double cyclingCadence; // rpm
    
    // Vitals
    private Double bloodPressureSystolic; // mmHg
    private Double bloodPressureDiastolic; // mmHg
    private Double oxygenSaturation; // percentage 0-100
    private Double respiratoryRate; // breaths per minute
    private Double skinTemperature; // celsius
    
    // Body Measurements
    private Double weight; // kg
    private Double height; // meters
    private Double bodyFat; // percentage
    private Double leanBodyMass; // kg
    private Double boneMass; // kg
    private Double bodyWaterMass; // kg
    private Double basalMetabolicRate; // kcal/day
    
    // Nutrition & Hydration
    private Double hydration; // liters
    private Double nutritionCalories; // kcal
    private Double nutritionProtein; // grams
    private Double nutritionCarbs; // grams
    private Double nutritionFat; // grams
    
    // Wellness
    private Double mindfulnessMinutes; // minutes
    
    // Blood & Glucose
    private Double bloodGlucose; // mmol/L
    
    // Exercise Details
    private Integer exerciseSessions;
    private Double avgExerciseDuration; // minutes
    private String exerciseTypes; // comma-separated
    
    // App Test Results (Cognitive Performance)
    private Double recentTypingWPM; // Words per minute from typing test
    private Double recentTypingAccuracy; // Accuracy percentage (0-100)
    private Integer recentReactionTimeMs; // Median reaction time in milliseconds
    private Integer typingTestsCount; // Number of typing tests in period
    private Integer reactionTestsCount; // Number of reaction tests in period
    
    // Manual Energy Inputs (User Self-Reported)
    private Integer manualEnergyLevel; // 1-10 scale, how energized user thinks they are
    private Integer physicalTiredness; // 1-10 scale, how physically tired
    private Integer mentalTiredness; // 1-10 scale, how mentally tired
    private String mealImpact; // "sluggish", "neutral", "sharp" - impact of last meal
    private Integer predictionAccuracyRating; // 0=Accurate, 1=Too High, -1=Too Low
    private Long lastManualInputTimestamp; // When user last provided manual input
    
    // Schedule & Tasks
    private String recentTask; // Recent task completed
    private String scheduleForDay; // Deprecated
    private String completedTasks; // Tasks already completed
    private String upcomingTasks; // Tasks still to be done
    private String mealTimes; // Meal times scheduled or completed
    
    // Lifestyle Factors
    private Integer caffeineIntakeCups; // Caffeine consumed today in cups
    private String currentEmotion; // "happy", "sad", "neutral", "stressed", "anxious", etc.
    
    // Device & Environment
    private Long deviceScreenTimeMinutes; // Screen time today in minutes
    private String weatherCondition; // "sunny", "cloudy", "rainy", "clear", etc.
    private Double weatherTemperature; // Temperature in celsius
    private String season; // "winter", "spring", "summer", "fall"
    
    // Getters and Setters
    public Double getAvgHeartRate() {
        return avgHeartRate;
    }
    
    public void setAvgHeartRate(Double avgHeartRate) {
        this.avgHeartRate = avgHeartRate;
    }
    
    public Double getCurrentHeartRate() {
        return currentHeartRate;
    }
    
    public void setCurrentHeartRate(Double currentHeartRate) {
        this.currentHeartRate = currentHeartRate;
    }
    
    public Double getRestingHeartRate() {
        return restingHeartRate;
    }
    
    public void setRestingHeartRate(Double restingHeartRate) {
        this.restingHeartRate = restingHeartRate;
    }
    
    public Integer getHeartRateReadings() {
        return heartRateReadings;
    }
    
    public void setHeartRateReadings(Integer heartRateReadings) {
        this.heartRateReadings = heartRateReadings;
    }
    
    public Double getAvgHRV() {
        return avgHRV;
    }
    
    public void setAvgHRV(Double avgHRV) {
        this.avgHRV = avgHRV;
    }
    
    public Double getCurrentHRV() {
        return currentHRV;
    }
    
    public void setCurrentHRV(Double currentHRV) {
        this.currentHRV = currentHRV;
    }
    
    public Integer getHrvReadings() {
        return hrvReadings;
    }
    
    public void setHrvReadings(Integer hrvReadings) {
        this.hrvReadings = hrvReadings;
    }
    
    public Double getLastNightSleepHours() {
        return lastNightSleepHours;
    }
    
    public void setLastNightSleepHours(Double lastNightSleepHours) {
        this.lastNightSleepHours = lastNightSleepHours;
    }
    
    public Long getLastWakeTimestamp() {
        return lastWakeTimestamp;
    }

    public void setLastWakeTimestamp(Long lastWakeTimestamp) {
        this.lastWakeTimestamp = lastWakeTimestamp;
    }
    
    public Double getAvgSleepHours() {
        return avgSleepHours;
    }
    
    public void setAvgSleepHours(Double avgSleepHours) {
        this.avgSleepHours = avgSleepHours;
    }
    
    public Double getSleepQuality() {
        return sleepQuality;
    }
    
    public void setSleepQuality(Double sleepQuality) {
        this.sleepQuality = sleepQuality;
    }
    
    public Integer getSleepSessions() {
        return sleepSessions;
    }
    
    public void setSleepSessions(Integer sleepSessions) {
        this.sleepSessions = sleepSessions;
    }
    
    public Integer getTodaySteps() {
        return todaySteps;
    }
    
    public void setTodaySteps(Integer todaySteps) {
        this.todaySteps = todaySteps;
    }
    
    public Integer getAvgDailySteps() {
        return avgDailySteps;
    }
    
    public void setAvgDailySteps(Integer avgDailySteps) {
        this.avgDailySteps = avgDailySteps;
    }
    
    public Double getActivityLevel() {
        return activityLevel;
    }
    
    public void setActivityLevel(Double activityLevel) {
        this.activityLevel = activityLevel;
    }
    
    public Integer getCurrentHour() {
        return currentHour;
    }
    
    public void setCurrentHour(Integer currentHour) {
        this.currentHour = currentHour;
    }
    
    public String getTimeOfDay() {
        return timeOfDay;
    }
    
    public void setTimeOfDay(String timeOfDay) {
        this.timeOfDay = timeOfDay;
    }
    
    public Integer getHoursOfData() {
        return hoursOfData;
    }
    
    public void setHoursOfData(Integer hoursOfData) {
        this.hoursOfData = hoursOfData;
    }
    
    public Boolean getHasRecentData() {
        return hasRecentData;
    }
    
    public void setHasRecentData(Boolean hasRecentData) {
        this.hasRecentData = hasRecentData;
    }
    
    // Activity Metrics Getters and Setters
    public Double getActiveCaloriesBurned() { return activeCaloriesBurned; }
    public void setActiveCaloriesBurned(Double activeCaloriesBurned) { this.activeCaloriesBurned = activeCaloriesBurned; }
    
    public Double getTotalCaloriesBurned() { return totalCaloriesBurned; }
    public void setTotalCaloriesBurned(Double totalCaloriesBurned) { this.totalCaloriesBurned = totalCaloriesBurned; }
    
    public Double getActivityIntensity() { return activityIntensity; }
    public void setActivityIntensity(Double activityIntensity) { this.activityIntensity = activityIntensity; }
    
    public Double getDistance() { return distance; }
    public void setDistance(Double distance) { this.distance = distance; }
    
    public Double getElevationGained() { return elevationGained; }
    public void setElevationGained(Double elevationGained) { this.elevationGained = elevationGained; }
    
    public Integer getFloorsClimbed() { return floorsClimbed; }
    public void setFloorsClimbed(Integer floorsClimbed) { this.floorsClimbed = floorsClimbed; }
    
    public Double getSpeed() { return speed; }
    public void setSpeed(Double speed) { this.speed = speed; }
    
    public Double getPower() { return power; }
    public void setPower(Double power) { this.power = power; }
    
    public Double getVo2Max() { return vo2Max; }
    public void setVo2Max(Double vo2Max) { this.vo2Max = vo2Max; }
    
    public Integer getWheelchairPushes() { return wheelchairPushes; }
    public void setWheelchairPushes(Integer wheelchairPushes) { this.wheelchairPushes = wheelchairPushes; }
    
    public Double getCyclingCadence() { return cyclingCadence; }
    public void setCyclingCadence(Double cyclingCadence) { this.cyclingCadence = cyclingCadence; }
    
    // Vitals Getters and Setters
    public Double getBloodPressureSystolic() { return bloodPressureSystolic; }
    public void setBloodPressureSystolic(Double bloodPressureSystolic) { this.bloodPressureSystolic = bloodPressureSystolic; }
    
    public Double getBloodPressureDiastolic() { return bloodPressureDiastolic; }
    public void setBloodPressureDiastolic(Double bloodPressureDiastolic) { this.bloodPressureDiastolic = bloodPressureDiastolic; }
    
    public Double getOxygenSaturation() { return oxygenSaturation; }
    public void setOxygenSaturation(Double oxygenSaturation) { this.oxygenSaturation = oxygenSaturation; }
    
    public Double getRespiratoryRate() { return respiratoryRate; }
    public void setRespiratoryRate(Double respiratoryRate) { this.respiratoryRate = respiratoryRate; }
    
    public Double getSkinTemperature() { return skinTemperature; }
    public void setSkinTemperature(Double skinTemperature) { this.skinTemperature = skinTemperature; }
    
    // Body Measurements Getters and Setters
    public Double getWeight() { return weight; }
    public void setWeight(Double weight) { this.weight = weight; }
    
    public Double getHeight() { return height; }
    public void setHeight(Double height) { this.height = height; }
    
    public Double getBodyFat() { return bodyFat; }
    public void setBodyFat(Double bodyFat) { this.bodyFat = bodyFat; }
    
    public Double getLeanBodyMass() { return leanBodyMass; }
    public void setLeanBodyMass(Double leanBodyMass) { this.leanBodyMass = leanBodyMass; }
    
    public Double getBoneMass() { return boneMass; }
    public void setBoneMass(Double boneMass) { this.boneMass = boneMass; }
    
    public Double getBodyWaterMass() { return bodyWaterMass; }
    public void setBodyWaterMass(Double bodyWaterMass) { this.bodyWaterMass = bodyWaterMass; }
    
    public Double getBasalMetabolicRate() { return basalMetabolicRate; }
    public void setBasalMetabolicRate(Double basalMetabolicRate) { this.basalMetabolicRate = basalMetabolicRate; }
    
    // Nutrition & Hydration Getters and Setters
    public Double getHydration() { return hydration; }
    public void setHydration(Double hydration) { this.hydration = hydration; }
    
    public Double getNutritionCalories() { return nutritionCalories; }
    public void setNutritionCalories(Double nutritionCalories) { this.nutritionCalories = nutritionCalories; }
    
    public Double getNutritionProtein() { return nutritionProtein; }
    public void setNutritionProtein(Double nutritionProtein) { this.nutritionProtein = nutritionProtein; }
    
    public Double getNutritionCarbs() { return nutritionCarbs; }
    public void setNutritionCarbs(Double nutritionCarbs) { this.nutritionCarbs = nutritionCarbs; }
    
    public Double getNutritionFat() { return nutritionFat; }
    public void setNutritionFat(Double nutritionFat) { this.nutritionFat = nutritionFat; }
    
    // Wellness Getters and Setters
    public Double getMindfulnessMinutes() { return mindfulnessMinutes; }
    public void setMindfulnessMinutes(Double mindfulnessMinutes) { this.mindfulnessMinutes = mindfulnessMinutes; }
    
    // Blood & Glucose Getters and Setters
    public Double getBloodGlucose() { return bloodGlucose; }
    public void setBloodGlucose(Double bloodGlucose) { this.bloodGlucose = bloodGlucose; }
    
    // Exercise Details Getters and Setters
    public Integer getExerciseSessions() { return exerciseSessions; }
    public void setExerciseSessions(Integer exerciseSessions) { this.exerciseSessions = exerciseSessions; }
    
    public Double getAvgExerciseDuration() { return avgExerciseDuration; }
    public void setAvgExerciseDuration(Double avgExerciseDuration) { this.avgExerciseDuration = avgExerciseDuration; }
    
    public String getExerciseTypes() { return exerciseTypes; }
    public void setExerciseTypes(String exerciseTypes) { this.exerciseTypes = exerciseTypes; }
    
    // App Test Results Getters and Setters
    public Double getRecentTypingWPM() { return recentTypingWPM; }
    public void setRecentTypingWPM(Double recentTypingWPM) { this.recentTypingWPM = recentTypingWPM; }
    
    public Double getRecentTypingAccuracy() { return recentTypingAccuracy; }
    public void setRecentTypingAccuracy(Double recentTypingAccuracy) { this.recentTypingAccuracy = recentTypingAccuracy; }
    
    public Integer getRecentReactionTimeMs() { return recentReactionTimeMs; }
    public void setRecentReactionTimeMs(Integer recentReactionTimeMs) { this.recentReactionTimeMs = recentReactionTimeMs; }
    
    public Integer getTypingTestsCount() { return typingTestsCount; }
    public void setTypingTestsCount(Integer typingTestsCount) { this.typingTestsCount = typingTestsCount; }
    
    public Integer getReactionTestsCount() { return reactionTestsCount; }
    public void setReactionTestsCount(Integer reactionTestsCount) { this.reactionTestsCount = reactionTestsCount; }
    
    // Manual Energy Inputs Getters and Setters
    public Integer getManualEnergyLevel() { return manualEnergyLevel; }
    public void setManualEnergyLevel(Integer manualEnergyLevel) { this.manualEnergyLevel = manualEnergyLevel; }
    
    public Integer getPhysicalTiredness() { return physicalTiredness; }
    public void setPhysicalTiredness(Integer physicalTiredness) { this.physicalTiredness = physicalTiredness; }
    
    public Integer getMentalTiredness() { return mentalTiredness; }
    public void setMentalTiredness(Integer mentalTiredness) { this.mentalTiredness = mentalTiredness; }
    
    public String getMealImpact() { return mealImpact; }
    public void setMealImpact(String mealImpact) { this.mealImpact = mealImpact; }
    
    public Integer getPredictionAccuracyRating() { return predictionAccuracyRating; }
    public void setPredictionAccuracyRating(Integer predictionAccuracyRating) { this.predictionAccuracyRating = predictionAccuracyRating; }
    
    public Long getLastManualInputTimestamp() { return lastManualInputTimestamp; }
    public void setLastManualInputTimestamp(Long lastManualInputTimestamp) { this.lastManualInputTimestamp = lastManualInputTimestamp; }
    
    // Schedule & Tasks Getters and Setters
    public String getRecentTask() { return recentTask; }
    public void setRecentTask(String recentTask) { this.recentTask = recentTask; }

    public String getScheduleForDay() { return scheduleForDay; }
    public void setScheduleForDay(String scheduleForDay) { this.scheduleForDay = scheduleForDay; }
    
    public String getCompletedTasks() { return completedTasks; }
    public void setCompletedTasks(String completedTasks) { this.completedTasks = completedTasks; }
    
    public String getUpcomingTasks() { return upcomingTasks; }
    public void setUpcomingTasks(String upcomingTasks) { this.upcomingTasks = upcomingTasks; }
    
    public String getMealTimes() { return mealTimes; }
    public void setMealTimes(String mealTimes) { this.mealTimes = mealTimes; }
    
    // Lifestyle Factors Getters and Setters
    public Integer getCaffeineIntakeCups() { return caffeineIntakeCups; }
    public void setCaffeineIntakeCups(Integer caffeineIntakeCups) { this.caffeineIntakeCups = caffeineIntakeCups; }
    
    public String getCurrentEmotion() { return currentEmotion; }
    public void setCurrentEmotion(String currentEmotion) { this.currentEmotion = currentEmotion; }
    
    // Device & Environment Getters and Setters
    public Long getDeviceScreenTimeMinutes() { return deviceScreenTimeMinutes; }
    public void setDeviceScreenTimeMinutes(Long deviceScreenTimeMinutes) { this.deviceScreenTimeMinutes = deviceScreenTimeMinutes; }
    
    public String getWeatherCondition() { return weatherCondition; }
    public void setWeatherCondition(String weatherCondition) { this.weatherCondition = weatherCondition; }
    
    public Double getWeatherTemperature() { return weatherTemperature; }
    public void setWeatherTemperature(Double weatherTemperature) { this.weatherTemperature = weatherTemperature; }
    
    // Previous Prediction
    private Double lastPredictedLevel;
    private Long lastPredictionTime;
    private String lastPredictionExplanation;

    public Double getLastPredictedLevel() { return lastPredictedLevel; }
    public void setLastPredictedLevel(Double lastPredictedLevel) { this.lastPredictedLevel = lastPredictedLevel; }

    public Long getLastPredictionTime() { return lastPredictionTime; }
    public void setLastPredictionTime(Long lastPredictionTime) { this.lastPredictionTime = lastPredictionTime; }

    public String getLastPredictionExplanation() { return lastPredictionExplanation; }
    public void setLastPredictionExplanation(String lastPredictionExplanation) { this.lastPredictionExplanation = lastPredictionExplanation; }

    public String getSeason() { return season; }
    public void setSeason(String season) { this.season = season; }
}


