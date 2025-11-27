package com.flowstate.workers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.flowstate.data.local.repo.BiometricDataRepository;
import com.flowstate.domain.mappers.FitMapper;
import com.flowstate.services.GoogleFitManager;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Tasks;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Worker to sync sleep data from Google Fit to Room database
 */
public class SleepSyncWorker extends Worker {
    
    private static final String TAG = "SleepSyncWorker";
    
    public SleepSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Starting sleep sync");
            
            GoogleFitManager fitManager = new GoogleFitManager(getApplicationContext());
            BiometricDataRepository repo = new BiometricDataRepository(getApplicationContext());
            
            // Get time range for sync
            long[] timeRange = fitManager.getBackfillTimeRange();
            long startMs = timeRange[0];
            long endMs = timeRange[1];
            
            // Read sleep data from Google Fit
            DataReadResponse response = Tasks.await(
                fitManager.readSleep(startMs, endMs)
            );
            
            // Map to Room entities
            List<com.flowstate.data.local.entities.SleepLocal> sleepList = FitMapper.mapSleep(response);
            
            // Save to Room database
            repo.saveSleep(sleepList);
            
            // Update last sync time
            fitManager.updateLastSync(endMs);
            
            // Mark first connect as complete if needed
            if (fitManager.isFirstConnect()) {
                fitManager.markFirstConnectComplete();
            }
            
            Log.d(TAG, "Sleep sync completed. Saved " + sleepList.size() + " sessions");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing sleep data", e);
            return Result.retry();
        }
    }
    
    /**
     * Create a one-time work request for sleep sync
     */
    public static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(SleepSyncWorker.class)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Create a periodic work request for hourly sleep sync
     * Note: Minimum interval is 15 minutes due to WorkManager constraints
     */
    public static PeriodicWorkRequest createPeriodicWorkRequest() {
        return new PeriodicWorkRequest.Builder(SleepSyncWorker.class, 1, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS) // Start after 1 hour
                .build();
    }
    
    /**
     * Unique work name for periodic sync
     */
    public static final String PERIODIC_WORK_NAME = "hourly_sleep_sync";
}

