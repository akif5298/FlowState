package com.flowstate.workers;

import android.content.Context;
import android.util.Log;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.flowstate.workers.HeartRateSyncWorker;
import com.flowstate.workers.SleepSyncWorker;
import com.flowstate.workers.StepsSyncWorker;
import com.flowstate.workers.HrvSyncWorker;
import com.flowstate.workers.WorkoutSyncWorker;

import java.util.List;

/**
 * Utility class to manage hourly sync scheduling
 */
public class SyncScheduler {
    
    private static final String TAG = "SyncScheduler";
    
    /**
     * Schedule hourly sync for all health data
     * (Room DB → Supabase sync has been removed)
     * 
     * @param context Application context
     */
    public static void scheduleHourlySync(Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        
        // Schedule heart rate sync
        workManager.enqueueUniquePeriodicWork(
            HeartRateSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            HeartRateSyncWorker.createPeriodicWorkRequest()
        );
        
        // Schedule sleep sync
        workManager.enqueueUniquePeriodicWork(
            SleepSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            SleepSyncWorker.createPeriodicWorkRequest()
        );
        
        // Schedule steps sync
        workManager.enqueueUniquePeriodicWork(
            StepsSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            StepsSyncWorker.createPeriodicWorkRequest()
        );
        
        // Schedule HRV sync
        workManager.enqueueUniquePeriodicWork(
            HrvSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            HrvSyncWorker.createPeriodicWorkRequest()
        );
        
        // Schedule workout sync
        workManager.enqueueUniquePeriodicWork(
            WorkoutSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            WorkoutSyncWorker.createPeriodicWorkRequest()
        );
        
        Log.d(TAG, "Hourly sync scheduled for all health metrics");
    }
    
    /**
     * Cancel hourly sync for both heart rate and sleep data
     * 
     * @param context Application context
     */
    public static void cancelHourlySync(Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        
        // Cancel heart rate sync
        workManager.cancelUniqueWork(HeartRateSyncWorker.PERIODIC_WORK_NAME);
        
        // Cancel sleep sync
        workManager.cancelUniqueWork(SleepSyncWorker.PERIODIC_WORK_NAME);
        
        Log.d(TAG, "Hourly sync cancelled for HR and Sleep");
    }
    
    /**
     * Check if hourly sync is scheduled
     * Note: This is asynchronous - use the returned ListenableFuture to check status
     * 
     * @param context Application context
     * @return ListenableFuture with WorkInfo list
     */
    public static ListenableFuture<List<WorkInfo>> getSyncStatus(Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        
        // Return combined work info for both syncs
        return workManager.getWorkInfosForUniqueWork(HeartRateSyncWorker.PERIODIC_WORK_NAME);
    }
}

