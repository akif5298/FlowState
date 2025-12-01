package com.personaleenergy.domain.features;

import android.content.Context;
import android.util.Log;
import com.personaleenergy.data.local.AppDb;
import com.personaleenergy.data.local.entities.HrLocal;
import com.personaleenergy.data.local.entities.SleepLocal;
import com.personaleenergy.data.local.entities.TypingLocal;
import com.personaleenergy.data.local.entities.ReactionLocal;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Service for building feature rows for machine learning
 * 
 * Computes features from local Room data for a given date
 */
public class FeatureService {
    
    private static final String TAG = "FeatureService";
    private static final int DEFAULT_BIN_SIZE_MINUTES = 60; // Default to 60 minutes
    
    private Context context;
    private AppDb db;
    private int binSizeMinutes;
    private ExecutorService executor;
    
    public FeatureService(Context context) {
        this(context, DEFAULT_BIN_SIZE_MINUTES);
    }
    
    public FeatureService(Context context, int binSizeMinutes) {
        this.context = context.getApplicationContext();
        this.db = AppDb.getInstance(context);
        this.binSizeMinutes = binSizeMinutes; // 30 or 60 minutes
        this.executor = Executors.newSingleThreadExecutor();
    }
    
    /**
     * Build feature rows for a given date
     * 
     * @param date LocalDate to build features for
     * @return List of FeatureRow covering 00:00-24:00 with no gaps > bin size
     */
    public List<FeatureRow> buildFor(LocalDate date) {
        Log.d(TAG, "Building features for date: " + date + " with bin size: " + binSizeMinutes + " minutes");
        
        // Get date range in epoch milliseconds
        long dayStartMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long dayEndMs = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        
        // Load all data needed for feature computation
        List<HrLocal> hrData = loadHrData(dayStartMs, dayEndMs);
        SleepLocal lastNightSleep = loadLastNightSleep(date);
        double wpmBaseline = computeWpmBaseline(date);
        double reactionBaseline = computeReactionBaseline(date);
        Map<Long, Double> wpmByBin = computeWpmByBin(date);
        Map<Long, Double> reactionByBin = computeReactionByBin(date);
        
        // Create bins for the entire day (00:00 to 24:00)
        List<FeatureRow> featureRows = new ArrayList<>();
        long binSizeMs = binSizeMinutes * 60 * 1000L;
        
        // Ensure we have bins covering the entire day (00:00 to 24:00)
        // Calculate number of bins needed
        int binsPerDay = (24 * 60) / binSizeMinutes;
        
        for (long slotStart = dayStartMs; slotStart < dayEndMs; slotStart += binSizeMs) {
            FeatureRow row = new FeatureRow(slotStart);
            
            // Compute HR mean/std for this bin
            computeHrFeatures(row, hrData, slotStart, slotStart + binSizeMs);
            
            // Attach last-night sleep duration/quality to all bins
            if (lastNightSleep != null) {
                row.sleepDurationH = lastNightSleep.duration != null ? lastNightSleep.duration / 60.0 : 0.0;
                // Sleep quality: compute from duration (simple heuristic)
                // Quality = 1.0 if duration >= 7 hours, decreases linearly
                if (row.sleepDurationH >= 7.0) {
                    row.sleepQuality = 1.0;
                } else if (row.sleepDurationH >= 6.0) {
                    row.sleepQuality = 0.7 + (row.sleepDurationH - 6.0) * 0.3;
                } else if (row.sleepDurationH >= 5.0) {
                    row.sleepQuality = 0.4 + (row.sleepDurationH - 5.0) * 0.3;
                } else {
                    row.sleepQuality = Math.max(0.0, row.sleepDurationH / 5.0 * 0.4);
                }
            }
            
            // Compute time-of-day sin/cos
            computeTimeOfDayFeatures(row, slotStart);
            
            // Compute WPM/reaction deltas
            Double wpmForBin = wpmByBin.get(slotStart);
            if (wpmForBin != null) {
                row.wpmDelta = wpmForBin - wpmBaseline;
            }
            
            Double reactionForBin = reactionByBin.get(slotStart);
            if (reactionForBin != null) {
                row.reactionDelta = reactionForBin - reactionBaseline;
            }
            
            featureRows.add(row);
        }
        
        Log.d(TAG, "Built " + featureRows.size() + " feature rows for " + date);
        return featureRows;
    }
    
    /**
     * Load heart rate data for the date range
     */
    private List<HrLocal> loadHrData(long startMs, long endMs) {
        try {
            // Use DAO method to get data by date range (all data, not just pending)
            return db.hrDao().getByDateRange(startMs, endMs);
        } catch (Exception e) {
            Log.e(TAG, "Error loading HR data", e);
            return new ArrayList<>();
        }
    }
    
    /**
     * Load last night's sleep (the most recent completed sleep session before the date)
     */
    private SleepLocal loadLastNightSleep(LocalDate date) {
        try {
            // Get sleep sessions ending before the start of the date
            long dayStartMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            // Use DAO method to get last completed sleep before day start
            return db.sleepDao().getLastCompletedBefore(dayStartMs);
        } catch (Exception e) {
            Log.e(TAG, "Error loading last night sleep", e);
            return null;
        }
    }
    
    /**
     * Compute HR mean and std for a bin
     */
    private void computeHrFeatures(FeatureRow row, List<HrLocal> hrData, long binStart, long binEnd) {
        List<Integer> bpmValues = new ArrayList<>();
        
        for (HrLocal hr : hrData) {
            if (hr.timestamp >= binStart && hr.timestamp < binEnd) {
                bpmValues.add(hr.bpm);
            }
        }
        
        if (bpmValues.isEmpty()) {
            row.hrMean = 0.0;
            row.hrStd = 0.0;
            return;
        }
        
        // Compute mean
        double sum = 0.0;
        for (int bpm : bpmValues) {
            sum += bpm;
        }
        row.hrMean = sum / bpmValues.size();
        
        // Compute standard deviation
        if (bpmValues.size() == 1) {
            row.hrStd = 0.0;
        } else {
            double variance = 0.0;
            for (int bpm : bpmValues) {
                double diff = bpm - row.hrMean;
                variance += diff * diff;
            }
            row.hrStd = Math.sqrt(variance / bpmValues.size());
        }
    }
    
    /**
     * Compute time-of-day sin/cos features
     * Encodes time of day as circular features (0-24 hours -> 0-2π)
     */
    private void computeTimeOfDayFeatures(FeatureRow row, long timestampMs) {
        LocalDateTime dateTime = LocalDateTime.ofInstant(
            java.time.Instant.ofEpochMilli(timestampMs),
            ZoneId.systemDefault()
        );
        
        int hour = dateTime.getHour();
        int minute = dateTime.getMinute();
        
        // Convert to fraction of day (0.0 to 1.0)
        double fractionOfDay = (hour * 60.0 + minute) / (24.0 * 60.0);
        
        // Convert to radians (0 to 2π)
        double radians = fractionOfDay * 2.0 * Math.PI;
        
        // Compute sin and cos
        row.sinTOD = Math.sin(radians);
        row.cosTOD = Math.cos(radians);
    }
    
    /**
     * Compute 7-day baseline WPM (average over previous 7 days)
     */
    private double computeWpmBaseline(LocalDate date) {
        try {
            LocalDate startDate = date.minusDays(7);
            long startMs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            List<TypingLocal> typingData = db.typingDao().getByDateRange(startMs, endMs);
            
            if (typingData.isEmpty()) {
                return 0.0;
            }
            
            double sum = 0.0;
            for (TypingLocal typing : typingData) {
                sum += typing.wpm;
            }
            return sum / typingData.size();
        } catch (Exception e) {
            Log.e(TAG, "Error computing WPM baseline", e);
            return 0.0;
        }
    }
    
    /**
     * Compute 7-day baseline reaction time (average over previous 7 days)
     */
    private double computeReactionBaseline(LocalDate date) {
        try {
            LocalDate startDate = date.minusDays(7);
            long startMs = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long endMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            
            List<ReactionLocal> reactionData = db.reactionDao().getByDateRange(startMs, endMs);
            
            if (reactionData.isEmpty()) {
                return 0.0;
            }
            
            double sum = 0.0;
            for (ReactionLocal reaction : reactionData) {
                sum += reaction.medianMs;
            }
            return sum / reactionData.size();
        } catch (Exception e) {
            Log.e(TAG, "Error computing reaction baseline", e);
            return 0.0;
        }
    }
    
    /**
     * Compute WPM by bin for the current day
     */
    private Map<Long, Double> computeWpmByBin(LocalDate date) {
        Map<Long, Double> wpmByBin = new HashMap<>();
        
        try {
            long dayStartMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long dayEndMs = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long binSizeMs = binSizeMinutes * 60 * 1000L;
            
            List<TypingLocal> typingData = db.typingDao().getByDateRange(dayStartMs, dayEndMs);
            
            // Group typing tests by bin
            Map<Long, List<Integer>> wpmByBinList = new HashMap<>();
            
            for (TypingLocal typing : typingData) {
                // Find which bin this timestamp belongs to
                long binStart = ((typing.timestamp - dayStartMs) / binSizeMs) * binSizeMs + dayStartMs;
                
                if (!wpmByBinList.containsKey(binStart)) {
                    wpmByBinList.put(binStart, new ArrayList<>());
                }
                wpmByBinList.get(binStart).add(typing.wpm);
            }
            
            // Compute average WPM per bin
            for (Map.Entry<Long, List<Integer>> entry : wpmByBinList.entrySet()) {
                List<Integer> wpmValues = entry.getValue();
                double sum = 0.0;
                for (int wpm : wpmValues) {
                    sum += wpm;
                }
                wpmByBin.put(entry.getKey(), sum / wpmValues.size());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error computing WPM by bin", e);
        }
        
        return wpmByBin;
    }
    
    /**
     * Compute reaction time by bin for the current day
     */
    private Map<Long, Double> computeReactionByBin(LocalDate date) {
        Map<Long, Double> reactionByBin = new HashMap<>();
        
        try {
            long dayStartMs = date.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long dayEndMs = date.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
            long binSizeMs = binSizeMinutes * 60 * 1000L;
            
            List<ReactionLocal> reactionData = db.reactionDao().getByDateRange(dayStartMs, dayEndMs);
            
            // Group reaction tests by bin
            Map<Long, List<Integer>> reactionByBinList = new HashMap<>();
            
            for (ReactionLocal reaction : reactionData) {
                // Find which bin this timestamp belongs to
                long binStart = ((reaction.timestamp - dayStartMs) / binSizeMs) * binSizeMs + dayStartMs;
                
                if (!reactionByBinList.containsKey(binStart)) {
                    reactionByBinList.put(binStart, new ArrayList<>());
                }
                reactionByBinList.get(binStart).add(reaction.medianMs);
            }
            
            // Compute average reaction time per bin
            for (Map.Entry<Long, List<Integer>> entry : reactionByBinList.entrySet()) {
                List<Integer> reactionValues = entry.getValue();
                double sum = 0.0;
                for (int reaction : reactionValues) {
                    sum += reaction;
                }
                reactionByBin.put(entry.getKey(), sum / reactionValues.size());
            }
        } catch (Exception e) {
            Log.e(TAG, "Error computing reaction by bin", e);
        }
        
        return reactionByBin;
    }
    
    /**
     * Set bin size in minutes (30 or 60)
     */
    public void setBinSizeMinutes(int minutes) {
        if (minutes == 30 || minutes == 60) {
            this.binSizeMinutes = minutes;
        } else {
            Log.w(TAG, "Invalid bin size: " + minutes + ". Using default: " + DEFAULT_BIN_SIZE_MINUTES);
            this.binSizeMinutes = DEFAULT_BIN_SIZE_MINUTES;
        }
    }
    
    /**
     * Get current bin size in minutes
     */
    public int getBinSizeMinutes() {
        return binSizeMinutes;
    }
}
