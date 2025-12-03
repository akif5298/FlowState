package com.flowstate.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.services.HealthConnectManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Worker to sync heart rate data from Health Connect to local DB.
 */
public class HeartRateSyncWorker extends Worker {

    private static final String TAG = "HeartRateSyncWorker";
    public static final String PERIODIC_WORK_NAME = "heart_rate_sync_periodic";
    
    private final Context context;
    private final AppDb db;
    private final HealthConnectManager healthConnectManager;

    public HeartRateSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
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

            Log.d(TAG, "Syncing Heart Rate data from Health Connect...");
            
            // Get data from Kotlin manager (returns CompletableFuture)
            List<HrLocal> hrList = healthConnectManager.readHeartRate(start, end).get(30, TimeUnit.SECONDS);
            
            if (hrList != null && !hrList.isEmpty()) {
                Log.d(TAG, "Found " + hrList.size() + " heart rate records. Saving to DB...");
                db.hrDao().insertAll(hrList);
            } else {
                Log.d(TAG, "No heart rate records found.");
            }
            
            return Result.success();
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "Error syncing heart rate data", e);
            return Result.retry();
        }
    }
    
    public static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(HeartRateSyncWorker.class).build();
    }

    public static PeriodicWorkRequest createPeriodicWorkRequest() {
        return new PeriodicWorkRequest.Builder(HeartRateSyncWorker.class, 1, TimeUnit.HOURS).build();
    }
}
