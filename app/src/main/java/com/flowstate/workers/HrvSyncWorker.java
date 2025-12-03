package com.flowstate.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.HrvLocal;
import com.flowstate.services.HealthConnectManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Worker to sync HRV data from Health Connect to local DB.
 */
public class HrvSyncWorker extends Worker {

    private static final String TAG = "HrvSyncWorker";
    public static final String PERIODIC_WORK_NAME = "hrv_sync_periodic";
    
    private final Context context;
    private final AppDb db;
    private final HealthConnectManager healthConnectManager;

    public HrvSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
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

            Log.d(TAG, "Syncing HRV data from Health Connect...");
            
            // Get data from Kotlin manager (returns CompletableFuture)
            List<HrvLocal> hrvList = healthConnectManager.readHRV(start, end).get(30, TimeUnit.SECONDS);
            
            if (hrvList != null && !hrvList.isEmpty()) {
                Log.d(TAG, "Found " + hrvList.size() + " HRV records. Saving to DB...");
                db.hrvDao().insertAll(hrvList);
            } else {
                Log.d(TAG, "No HRV records found.");
            }
            
            return Result.success();
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "Error syncing HRV data", e);
            return Result.retry();
        }
    }
    
    public static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(HrvSyncWorker.class).build();
    }

    public static PeriodicWorkRequest createPeriodicWorkRequest() {
        return new PeriodicWorkRequest.Builder(HrvSyncWorker.class, 1, TimeUnit.HOURS).build();
    }
}

