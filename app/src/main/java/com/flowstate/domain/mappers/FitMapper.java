package com.flowstate.domain.mappers;

import android.util.Log;
import com.google.android.gms.fitness.data.DataPoint;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.data.local.entities.SleepLocal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Mapper to convert Google Fit DataReadResponse to Room entities
 */
public class FitMapper {
    
    private static final String TAG = "FitMapper";
    
    /**
     * Map heart rate DataReadResponse to HrLocal entities
     * 
     * @param response DataReadResponse from Google Fit
     * @return List of HrLocal entities
     */
    public static List<HrLocal> mapHr(DataReadResponse response) {
        List<HrLocal> hrList = new ArrayList<>();
        
        if (response == null) {
            Log.w(TAG, "DataReadResponse is null");
            return hrList;
        }
        
        try {
            var dataSet = response.getDataSet(DataType.TYPE_HEART_RATE_BPM);
            if (dataSet == null) {
                Log.d(TAG, "No heart rate data in response");
                return hrList;
            }
            
            for (DataPoint dataPoint : dataSet.getDataPoints()) {
                try {
                    // Get heart rate value
                    var bpmValue = dataPoint.getValue(Field.FIELD_BPM);
                    if (bpmValue == null) {
                        continue;
                    }
                    
                    int bpm = bpmValue.asInt();
                    long timestamp = dataPoint.getTimestamp(TimeUnit.MILLISECONDS);
                    
                    // Validate BPM range (30-250)
                    if (bpm < 30 || bpm > 250) {
                        Log.w(TAG, "Invalid BPM value: " + bpm + ", skipping");
                        continue;
                    }
                    
                    HrLocal hrLocal = new HrLocal();
                    hrLocal.timestamp = timestamp;
                    hrLocal.bpm = bpm;
                    hrLocal.synced = false;
                    
                    hrList.add(hrLocal);
                } catch (Exception e) {
                    Log.e(TAG, "Error mapping heart rate data point", e);
                }
            }
            
            Log.d(TAG, "Mapped " + hrList.size() + " heart rate readings");
        } catch (Exception e) {
            Log.e(TAG, "Error mapping heart rate data", e);
        }
        
        return hrList;
    }
    
    /**
     * Map sleep DataReadResponse to SleepLocal entities
     * 
     * @param response DataReadResponse from Google Fit
     * @return List of SleepLocal entities
     */
    public static List<SleepLocal> mapSleep(DataReadResponse response) {
        List<SleepLocal> sleepList = new ArrayList<>();
        
        if (response == null) {
            Log.w(TAG, "DataReadResponse is null");
            return sleepList;
        }
        
        try {
            var dataSet = response.getDataSet(DataType.TYPE_SLEEP_SEGMENT);
            if (dataSet == null) {
                Log.d(TAG, "No sleep data in response");
                return sleepList;
            }
            
            for (DataPoint dataPoint : dataSet.getDataPoints()) {
                try {
                    long startMs = dataPoint.getStartTime(TimeUnit.MILLISECONDS);
                    long endMs = dataPoint.getEndTime(TimeUnit.MILLISECONDS);
                    
                    // Calculate duration in minutes
                    long durationMs = endMs - startMs;
                    int durationMinutes = (int) (durationMs / 60000);
                    
                    // Validate duration (should be positive and reasonable)
                    if (durationMinutes <= 0 || durationMinutes > 1440) { // Max 24 hours
                        Log.w(TAG, "Invalid sleep duration: " + durationMinutes + " minutes, skipping");
                        continue;
                    }
                    
                    SleepLocal sleepLocal = new SleepLocal();
                    sleepLocal.sleep_start = startMs;
                    sleepLocal.sleep_end = endMs;
                    sleepLocal.duration = durationMinutes;
                    sleepLocal.synced = false;
                    
                    sleepList.add(sleepLocal);
                } catch (Exception e) {
                    Log.e(TAG, "Error mapping sleep data point", e);
                }
            }
            
            Log.d(TAG, "Mapped " + sleepList.size() + " sleep sessions");
        } catch (Exception e) {
            Log.e(TAG, "Error mapping sleep data", e);
        }
        
        return sleepList;
    }
}

