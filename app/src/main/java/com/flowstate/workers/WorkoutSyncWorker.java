package com.flowstate.workers;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.OneTimeWorkRequest;
import androidx.work.PeriodicWorkRequest;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.flowstate.data.local.AppDb;
import com.flowstate.data.local.entities.WorkoutLocal;
import com.flowstate.services.HealthConnectManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Worker to sync workout data from Health Connect to local DB.
 */
public class WorkoutSyncWorker extends Worker {

    private static final String TAG = "WorkoutSyncWorker";
    public static final String PERIODIC_WORK_NAME = "workout_sync_periodic";
    
    private final Context context;
    private final AppDb db;
    private final HealthConnectManager healthConnectManager;

    public WorkoutSyncWorker(@NonNull Context context, @NonNull WorkerParameters params) {
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

            Log.d(TAG, "Syncing Workout data from Health Connect...");
            
            // Get data from Kotlin manager (returns CompletableFuture)
            List<WorkoutLocal> workoutList = healthConnectManager.readWorkouts(start, end).get(30, TimeUnit.SECONDS);
            
            if (workoutList != null && !workoutList.isEmpty()) {
                Log.d(TAG, "Found " + workoutList.size() + " workout records. Saving to DB...");
                db.workoutDao().insertAll(workoutList);
            } else {
                Log.d(TAG, "No workout records found.");
            }
            
            return Result.success();
            
        } catch (InterruptedException | ExecutionException | TimeoutException e) {
            Log.e(TAG, "Error syncing workout data", e);
            return Result.retry();
        }
    }
    
    public static OneTimeWorkRequest createWorkRequest() {
        return new OneTimeWorkRequest.Builder(WorkoutSyncWorker.class).build();
    }

    public static PeriodicWorkRequest createPeriodicWorkRequest() {
        return new PeriodicWorkRequest.Builder(WorkoutSyncWorker.class, 1, TimeUnit.HOURS).build();
    }
}

