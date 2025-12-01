package com.personaleenergy.app.health;

import android.content.Context;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.permission.HealthPermission;
import androidx.health.connect.client.records.StepsRecord;
import androidx.health.connect.client.request.ReadRecordsRequest;
import androidx.health.connect.client.response.ReadRecordsResponse;
import androidx.health.connect.client.time.TimeRangeFilter;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Executors;

public class HealthConnectManager {

    private final Context context;
    private final HealthConnectClient healthConnectClient;

    private final Set<String> PERMISSIONS = new HashSet<>();

    public HealthConnectManager(Context context) {
        this.context = context;
        if (HealthConnectClient.isProviderAvailable(context)) {
            healthConnectClient = HealthConnectClient.getOrCreate(context);
        } else {
            healthConnectClient = null;
        }
        PERMISSIONS.add(HealthPermission.getReadPermission(StepsRecord.class));
    }

    public void checkPermissionsAndRun(ActivityResultLauncher<String[]> requestPermissionsLauncher) {
        if (healthConnectClient == null) {
            Toast.makeText(context, "Health Connect not available", Toast.LENGTH_SHORT).show();
            return;
        }
        ListenableFuture<Set<String>> grantedPermissionsFuture = healthConnectClient.getPermissionController().getGrantedPermissions();
        Futures.addCallback(grantedPermissionsFuture, new FutureCallback<Set<String>>() {
            @Override
            public void onSuccess(Set<String> result) {
                if (result.containsAll(PERMISSIONS)) {
                    // Permissions already granted
                } else {
                    requestPermissionsLauncher.launch(PERMISSIONS.toArray(new String[0]));
                }
            }

            @Override
            public void onFailure(Throwable t) {
                Toast.makeText(context, "Failed to get permissions", Toast.LENGTH_SHORT).show();
            }
        }, Executors.newSingleThreadExecutor());
    }

    public void readData(Instant start, Instant end, HealthDataCallback callback) {
        if (healthConnectClient == null) {
            callback.onError(new IllegalStateException("Health Connect not available"));
            return;
        }

        ReadRecordsRequest<StepsRecord> request = new ReadRecordsRequest<>(StepsRecord.class, TimeRangeFilter.between(start, end));
        ListenableFuture<ReadRecordsResponse<StepsRecord>> future = healthConnectClient.readRecords(request);

        Futures.addCallback(future, new FutureCallback<ReadRecordsResponse<StepsRecord>>() {
            @Override
            public void onSuccess(ReadRecordsResponse<StepsRecord> result) {
                long totalSteps = 0;
                for (StepsRecord record : result.getRecords()) {
                    totalSteps += record.getCount();
                }
                callback.onDataLoaded("Total steps: " + totalSteps);
            }

            @Override
            public void onFailure(Throwable t) {
                callback.onError(new Exception("Error reading steps data", t));
            }
        }, Executors.newSingleThreadExecutor());
    }

    public interface HealthDataCallback {
        void onDataLoaded(String data);
        void onError(Exception e);
    }
}
