package com.flowstate.services;

import android.content.Context;
import android.util.Log;
import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.ManualEnergyInputLocal;
import com.flowstate.data.local.entities.ScheduleLocal;
import com.flowstate.data.local.entities.CaffeineIntakeLocal;
import com.flowstate.data.local.entities.EmotionLocal;

import java.util.Calendar;

/**
 * Manages manual user inputs for energy prediction
 * Stores user self-reported data in SQLite database
 */
public class ManualInputManager {
    
    private static final String TAG = "ManualInputManager";
    
    private final AppDb database;
    
    public ManualInputManager(Context context) {
        this.database = AppDb.getInstance(context);
    }
    
    /**
     * Save manual energy input (all fields at once)
     */
    public long saveManualEnergyInput(Integer energyLevel, Integer physicalTiredness, 
                                     Integer mentalTiredness, String mealImpact, 
                                     Integer predictionAccuracy) {
        ManualEnergyInputLocal input = new ManualEnergyInputLocal();
        input.timestamp = System.currentTimeMillis();
        input.energyLevel = energyLevel;
        input.physicalTiredness = physicalTiredness;
        input.mentalTiredness = mentalTiredness;
        input.mealImpact = mealImpact;
        input.predictionAccuracyRating = predictionAccuracy;
        
        return database.manualEnergyInputDao().insert(input);
    }
    
    /**
     * Set manual energy level (1-10 scale) - creates new record
     */
    public long setManualEnergyLevel(int energyLevel) {
        if (energyLevel < 1 || energyLevel > 10) {
            Log.w(TAG, "Invalid energy level: " + energyLevel + ", must be 1-10");
            return -1;
        }
        ManualEnergyInputLocal input = new ManualEnergyInputLocal();
        input.timestamp = System.currentTimeMillis();
        input.energyLevel = energyLevel;
        return database.manualEnergyInputDao().insert(input);
    }
    
    /**
     * Set physical tiredness (1-10 scale) - creates new record
     */
    public long setPhysicalTiredness(int tiredness) {
        if (tiredness < 1 || tiredness > 10) {
            Log.w(TAG, "Invalid tiredness: " + tiredness + ", must be 1-10");
            return -1;
        }
        ManualEnergyInputLocal input = new ManualEnergyInputLocal();
        input.timestamp = System.currentTimeMillis();
        input.physicalTiredness = tiredness;
        return database.manualEnergyInputDao().insert(input);
    }
    
    /**
     * Set mental tiredness (1-10 scale) - creates new record
     */
    public long setMentalTiredness(int tiredness) {
        if (tiredness < 1 || tiredness > 10) {
            Log.w(TAG, "Invalid tiredness: " + tiredness + ", must be 1-10");
            return -1;
        }
        ManualEnergyInputLocal input = new ManualEnergyInputLocal();
        input.timestamp = System.currentTimeMillis();
        input.mentalTiredness = tiredness;
        return database.manualEnergyInputDao().insert(input);
    }
    
    /**
     * Set meal impact: "sluggish", "neutral", or "sharp" - creates new record
     */
    public long setMealImpact(String impact) {
        if (impact == null || (!impact.equals("sluggish") && !impact.equals("neutral") && !impact.equals("sharp"))) {
            Log.w(TAG, "Invalid meal impact: " + impact);
            return -1;
        }
        ManualEnergyInputLocal input = new ManualEnergyInputLocal();
        input.timestamp = System.currentTimeMillis();
        input.mealImpact = impact;
        return database.manualEnergyInputDao().insert(input);
    }
    
    /**
     * Set prediction accuracy feedback (0=accurate, 1=high, -1=low) - creates new record
     */
    public long setPredictionAccuracy(int rating) {
        ManualEnergyInputLocal input = new ManualEnergyInputLocal();
        input.timestamp = System.currentTimeMillis();
        input.predictionAccuracyRating = rating;
        return database.manualEnergyInputDao().insert(input);
    }
    
    /**
     * Save or update schedule for today
     */
    public long setScheduleForDay(String schedule, String completedTasks, 
                                 String upcomingTasks, String mealTimes) {
        long now = System.currentTimeMillis();
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dateStart = cal.getTimeInMillis();
        
        ScheduleLocal existing = database.scheduleDao().getByDate(dateStart);
        if (existing != null) {
            // Update existing
            existing.scheduleForDay = schedule;
            existing.completedTasks = completedTasks;
            existing.upcomingTasks = upcomingTasks;
            existing.mealTimes = mealTimes;
            existing.lastUpdated = now;
            database.scheduleDao().update(existing);
            return existing.id;
        } else {
            // Create new
            ScheduleLocal scheduleLocal = new ScheduleLocal();
            scheduleLocal.date = dateStart;
            scheduleLocal.scheduleForDay = schedule;
            scheduleLocal.completedTasks = completedTasks;
            scheduleLocal.upcomingTasks = upcomingTasks;
            scheduleLocal.mealTimes = mealTimes;
            scheduleLocal.lastUpdated = now;
            return database.scheduleDao().insert(scheduleLocal);
        }
    }
    
    /**
     * Add caffeine intake record
     */
    public long addCaffeineIntake(double caffeineMg, String source) {
        CaffeineIntakeLocal intake = new CaffeineIntakeLocal();
        intake.timestamp = System.currentTimeMillis();
        intake.caffeineMg = caffeineMg;
        intake.source = source != null ? source : "unknown";
        return database.caffeineIntakeDao().insert(intake);
    }
    
    /**
     * Set current emotion - creates new record
     */
    public long setCurrentEmotion(String emotion, String notes) {
        EmotionLocal emotionLocal = new EmotionLocal();
        emotionLocal.timestamp = System.currentTimeMillis();
        emotionLocal.emotion = emotion;
        emotionLocal.notes = notes;
        return database.emotionDao().insert(emotionLocal);
    }
    
    /**
     * Get most recent manual energy input
     */
    public ManualEnergyInputLocal getMostRecentInput() {
        return database.manualEnergyInputDao().getMostRecent();
    }
    
    /**
     * Get schedule for today
     */
    public ScheduleLocal getTodaySchedule() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dateStart = cal.getTimeInMillis();
        return database.scheduleDao().getByDate(dateStart);
    }
    
    /**
     * Get total caffeine for today
     */
    public double getTodayCaffeineMg() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        long dateStart = cal.getTimeInMillis();
        long dateEnd = dateStart + (24 * 60 * 60 * 1000);
        
        Double total = database.caffeineIntakeDao().getTotalForDate(dateStart, dateEnd);
        return total != null ? total : 0.0;
    }
    
    /**
     * Get most recent emotion
     */
    public EmotionLocal getMostRecentEmotion() {
        return database.emotionDao().getMostRecent();
    }
}

