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
import com.flowstate.workers.RemoteSyncWorker;

import java.util.List;

/**
 * Utility class to manage sync scheduling
 * Handles both local (Health Connect → Room) and remote (Room → Backend) syncing
 */
public class SyncScheduler {
    
    private static final String TAG = "SyncScheduler";
    
    /**
     * Schedule hourly sync for biometric data from Health Connect to Room DB
     * Also schedules remote sync to backend every 2 hours
     * 
     * @param context Application context
     */
    public static void scheduleHourlySync(Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        
        // Schedule heart rate sync (Health Connect → Room DB)
        workManager.enqueueUniquePeriodicWork(
            HeartRateSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            HeartRateSyncWorker.createPeriodicWorkRequest()
        );
        
        // Schedule sleep sync (Health Connect → Room DB)
        workManager.enqueueUniquePeriodicWork(
            SleepSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP, // Keep existing if already scheduled
            SleepSyncWorker.createPeriodicWorkRequest()
        );
        
        // Schedule steps sync (Health Connect → Room DB)
        workManager.enqueueUniquePeriodicWork(
            StepsSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            StepsSyncWorker.createPeriodicWorkRequest()
        );
        
        // Schedule remote sync (Room DB → Backend API)
        workManager.enqueueUniquePeriodicWork(
            RemoteSyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            RemoteSyncWorker.createPeriodicWorkRequest()
        );
        
        Log.d(TAG, "Hourly sync scheduled for HR, Sleep, and Steps (Health Connect → Room DB)");
        Log.d(TAG, "2-hour sync scheduled for remote backend (Room DB → Backend API)");
    }
    
    /**
     * Cancel all sync operations (both local and remote)
     * 
     * @param context Application context
     */
    public static void cancelHourlySync(Context context) {
        WorkManager workManager = WorkManager.getInstance(context.getApplicationContext());
        
        // Cancel local syncs (Health Connect → Room DB)
        workManager.cancelUniqueWork(HeartRateSyncWorker.PERIODIC_WORK_NAME);
        workManager.cancelUniqueWork(SleepSyncWorker.PERIODIC_WORK_NAME);
        workManager.cancelUniqueWork(StepsSyncWorker.PERIODIC_WORK_NAME);
        
        // Cancel remote sync (Room DB → Backend API)
        workManager.cancelUniqueWork(RemoteSyncWorker.PERIODIC_WORK_NAME);
        
        Log.d(TAG, "All sync operations cancelled (local and remote)");
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

