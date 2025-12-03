package com.flowstate.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.SleepLocal;
import com.flowstate.services.HealthConnectManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Worker to sync sleep data from Health Connect to local DB.
 */
public class SleepSyncWorker extends Worker {

    private static final String TAG = "SleepSyncWorker";
    public static final String PERIODIC_WORK_NAME = "sleep_sync_periodic";
    
    private final Context context;
    private final AppDb db;
    private final HealthConnectManager healthConnectManager;

    public SleepSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
        super(context, params);
        this.context = context.getApplicationContext();
        this.db = AppDb.getInstance(this.context);
        this.healthConnectManager = new HealthConnectManager(this.context);
    }

    @NonNull
    @Override
    public Result doWork() {
        if (!healthConnectManager.isAvailable()) {
            Log.e(TAG, "Health Connect unavailable, skipping sync");
            return Result.failure();
        }

        try {
            // Sync last 7 days of sleep data (for weekly chart)
            Instant end = Instant.now();
            Instant start = end.minus(7, ChronoUnit.DAYS);

            Log.d(TAG, "Syncing Sleep data from Health Connect (last 7 days)...");
            
            // Get data from Kotlin manager (returns CompletableFuture)
            List<SleepLocal> sleepList = healthConnectManager.readSleep(start, end).get(30, TimeUnit.SECONDS);
            
            if (sleepList != null && !sleepList.isEmpty()) {
                Log.d(TAG, "Found " + sleepList.size() + " sleep records. Saving to DB...");
                int inserted = 0;
                for (SleepLocal sleep : sleepList) {
                    try {
                        db.sleepDao().insert(sleep);
                        inserted++;
                    } catch (Exception e) {
                        // Duplicate entry (unique constraint on sleep_start), skip
                        Log.d(TAG, "Sleep record already exists for " + sleep.sleep_start);
                    }
                }
                Log.d(TAG, "Inserted " + inserted + " new sleep records");
            } else {
                Log.d(TAG, "No sleep records found in Health Connect.");
            }
            
            return Result.success();
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "Error syncing sleep data", e);
            return Result.retry();
        }
    }
    
    public static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(SleepSyncWorker.class).build();
    }

    public static PeriodicWorkRequest createPeriodicWorkRequest() {
        return new PeriodicWorkRequest.Builder(SleepSyncWorker.class, 1, TimeUnit.HOURS).build();
    }
}
