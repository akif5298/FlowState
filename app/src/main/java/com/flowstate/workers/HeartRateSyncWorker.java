package com.personaleenergy.workers;

import android.content.Context;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import com.personaleenergy.data.local.repo.BiometricDataRepository;
import com.personaleenergy.domain.mappers.FitMapper;
import com.personaleenergy.services.GoogleFitManager;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.google.android.gms.tasks.Tasks;

import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Worker to sync heart rate data from Google Fit to Room database
 */
public class HeartRateSyncWorker extends Worker {
    
    private static final String TAG = "HrSyncWorker";
    
    public HeartRateSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
    }
    
    @NonNull
    @Override
    public Result doWork() {
        try {
            Log.d(TAG, "Starting heart rate sync");
            
            GoogleFitManager fitManager = new GoogleFitManager(getApplicationContext());
            BiometricDataRepository repo = new BiometricDataRepository(getApplicationContext());
            
            // Get time range for sync
            long[] timeRange = fitManager.getBackfillTimeRange();
            long startMs = timeRange[0];
            long endMs = timeRange[1];
            
            // Read heart rate data from Google Fit
            DataReadResponse response = Tasks.await(
                fitManager.readHeartRate(startMs, endMs)
            );
            
            // Map to Room entities
            List<com.personaleenergy.data.local.entities.HrLocal> hrList = FitMapper.mapHr(response);
            
            // Save to Room database
            repo.saveHr(hrList);
            
            // Update last sync time
            fitManager.updateLastSync(endMs);
            
            // Mark first connect as complete if needed
            if (fitManager.isFirstConnect()) {
                fitManager.markFirstConnectComplete();
            }
            
            Log.d(TAG, "Heart rate sync completed. Saved " + hrList.size() + " readings");
            return Result.success();
            
        } catch (Exception e) {
            Log.e(TAG, "Error syncing heart rate data", e);
            return Result.retry();
        }
    }
    
    /**
     * Create a one-time work request for heart rate sync
     */
    public static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(HeartRateSyncWorker.class)
                .setInitialDelay(0, TimeUnit.SECONDS)
                .build();
    }
    
    /**
     * Create a periodic work request for hourly heart rate sync
     * Note: Minimum interval is 15 minutes due to WorkManager constraints
     */
    public static PeriodicWorkRequest createPeriodicWorkRequest() {
        return new PeriodicWorkRequest.Builder(HeartRateSyncWorker.class, 1, TimeUnit.HOURS)
                .setInitialDelay(1, TimeUnit.HOURS) // Start after 1 hour
                .build();
    }
    
    /**
     * Unique work name for periodic sync
     */
    public static final String PERIODIC_WORK_NAME = "hourly_hr_sync";
}
