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
            // Sync last 24 hours of data
            Instant end = Instant.now();
            Instant start = end.minus(24, ChronoUnit.HOURS);

            Log.d(TAG, "Syncing Sleep data from Health Connect...");
            
            // Get data from Kotlin manager (returns CompletableFuture)
            List<SleepLocal> sleepList = healthConnectManager.readSleep(start, end).get(30, TimeUnit.SECONDS);
            
            if (sleepList != null && !sleepList.isEmpty()) {
                Log.d(TAG, "Found " + sleepList.size() + " sleep records. Saving to DB...");
                db.sleepDao().insertAll(sleepList);
            } else {
                Log.d(TAG, "No sleep records found.");
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
