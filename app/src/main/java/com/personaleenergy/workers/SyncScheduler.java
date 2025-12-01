package com.personaleenergy.workers;

import android.content.Context;
import android.util.Log;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.WorkInfo;
import androidx.work.WorkManager;
import com.google.common.util.concurrent.ListenableFuture;
import com.personaleenergy.workers.HeartRateSyncWorker;
import com.personaleenergy.workers.SleepSyncWorker;

import java.util.List;

/**
 * Utility class to manage hourly sync scheduling
 */
public class SyncScheduler {
    
    private static final String TAG = "SyncScheduler";
    
    /**
     * Schedule hourly sync for both heart rate and sleep data
     * 
     * @param context Application context
     */
    public static void scheduleHourlySync(Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        
        // Schedule heart rate sync (Google Fit → Room DB)
        workManager.enqueueUniquePeriodicWork(
            HeartRateSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            HeartRateSyncWorker.createPeriodicWorkRequest()
        );
        
        // Schedule sleep sync (Google Fit → Room DB)
        workManager.enqueueUniquePeriodicWork(
            SleepSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            SleepSyncWorker.createPeriodicWorkRequest()
        );
        
        Log.d(TAG, "Hourly sync scheduled for HR and Sleep");
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
