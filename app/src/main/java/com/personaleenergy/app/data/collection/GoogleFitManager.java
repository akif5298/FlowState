package com.personaleenergy.app.data.collection;

import android.content.Context;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.fitness.Fitness;
import com.google.android.gms.fitness.FitnessOptions;
import com.google.android.gms.fitness.data.DataType;
import com.google.android.gms.fitness.data.Field;
import com.google.android.gms.fitness.request.DataReadRequest;
import com.google.android.gms.fitness.result.DataReadResponse;
import com.personaleenergy.app.data.models.BiometricData;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class GoogleFitManager {
    private static final String TAG = "GoogleFitManager";
    
    private Context context;
    private FitnessOptions fitnessOptions;

    public GoogleFitManager(Context context) {
        this.context = context;
        this.fitnessOptions = FitnessOptions.builder()
                .addDataType(DataType.TYPE_HEART_RATE_BPM, FitnessOptions.ACCESS_READ)
                .addDataType(DataType.TYPE_SLEEP_SEGMENT, FitnessOptions.ACCESS_READ)
                .build();
    }

    public boolean isSignedIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        return account != null;
    }

    public void readCombinedBiometricData(int hours, BiometricCallback callback) {
        long endTime = Calendar.getInstance().getTimeInMillis();
        long startTime = endTime - TimeUnit.HOURS.toMillis(hours);

        DataReadRequest readRequest = new DataReadRequest.Builder()
                .read(DataType.TYPE_HEART_RATE_BPM)
                .read(DataType.TYPE_SLEEP_SEGMENT)
                .setTimeRange(startTime, endTime, TimeUnit.MILLISECONDS)
                .enableServerQueries()
                .build();

        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            callback.onError(new Exception("Not signed in to Google Fit. Please connect first."));
            return;
        }

        try {
            Fitness.getHistoryClient(context, account)
                    .readData(readRequest)
                    .addOnSuccessListener(response -> {
                        List<BiometricData> biometricData = parseCombinedData(response, startTime, endTime);
                        callback.onSuccess(biometricData);
                    })
                    .addOnFailureListener(e -> {
                        Log.e(TAG, "Failed to read biometric data", e);
                        callback.onError(e);
                    });
        } catch (Exception e) {
            Log.e(TAG, "Exception reading biometric data", e);
            callback.onError(e);
        }
    }

    private List<BiometricData> parseCombinedData(DataReadResponse response, long startTime, long endTime) {
        List<BiometricData> list = new ArrayList<>();
        Map<Long, BiometricData> combined = new HashMap<>();

        // Parse heart rate data
        if (response.getDataSet(DataType.TYPE_HEART_RATE_BPM) != null) {
            response.getDataSet(DataType.TYPE_HEART_RATE_BPM).getDataPoints().forEach(dataPoint -> {
                Integer heartRate = dataPoint.getValue(Field.FIELD_BPM).asInt();
                Date timestamp = new Date(dataPoint.getTimestamp(TimeUnit.MILLISECONDS));
                long timeKey = dataPoint.getTimestamp(TimeUnit.MILLISECONDS);
                
                BiometricData data = combined.getOrDefault(timeKey, new BiometricData(timestamp));
                data.setHeartRate(heartRate);
                combined.put(timeKey, data);
            });
        }

        // Parse sleep data
        if (response.getDataSet(DataType.TYPE_SLEEP_SEGMENT) != null) {
            response.getDataSet(DataType.TYPE_SLEEP_SEGMENT).getDataPoints().forEach(dataPoint -> {
                long startTimeMs = dataPoint.getStartTime(TimeUnit.MILLISECONDS);
                long endTimeMs = dataPoint.getEndTime(TimeUnit.MILLISECONDS);
                int durationMinutes = (int) ((endTimeMs - startTimeMs) / 60000);
                
                double quality;
                if (durationMinutes < 360) quality = 0.3;
                else if (durationMinutes < 480) quality = 0.6;
                else if (durationMinutes < 600) quality = 0.9;
                else quality = 0.7;
                
                Date timestamp = new Date(startTimeMs);
                long timeKey = startTimeMs;
                
                BiometricData data = combined.getOrDefault(timeKey, new BiometricData(timestamp));
                data.setSleepMinutes(durationMinutes);
                data.setSleepQuality(quality);
                combined.put(timeKey, data);
            });
        }

        list.addAll(combined.values());
        list.sort(Comparator.comparing(BiometricData::getTimestamp));
        return list;
    }

    public void requestFitnessPermission(GoogleSignInAccount account) {
        if (account == null) {
            Log.e(TAG, "Cannot request permissions: account is null");
            return;
        }
        
        // Request Google Fit permissions
        // Note: In production, you would need to implement the actual permission request flow
        // based on the Google Fit API requirements
        Log.d(TAG, "Google Fit permission request initiated for account");
    }

    public interface BiometricCallback {
        void onSuccess(List<BiometricData> data);
        void onError(Exception e);
    }
}
