package com.flowstate.services;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.flowstate.app.data.models.EnergyPredictionResult;
import com.flowstate.data.local.entities.PredictionLocal;
import com.flowstate.data.local.repo.EnergyPredictionRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * Singleton manager to handle background prediction tasks that persist across Activity lifecycle
 */
public class PredictionManager {

    private static final String TAG = "PredictionManager";
    private static PredictionManager instance;

    private EnergyPredictionService predictionService;
    private EnergyPredictionRepository predictionRepository;
    private DataChecker dataChecker;
    
    // LiveData for observing status from any Activity
    private final MutableLiveData<PredictionStatus> status = new MutableLiveData<>(PredictionStatus.IDLE);
    private final MutableLiveData<String> statusMessage = new MutableLiveData<>("");
    private final MutableLiveData<EnergyPredictionResult> lastResult = new MutableLiveData<>();

    public enum PredictionStatus {
        IDLE,
        CHECKING_DATA,
        LOADING,
        SUCCESS,
        ERROR
    }

    private PredictionManager(Context context) {
        Context appContext = context.getApplicationContext();
        this.predictionService = new EnergyPredictionService(appContext);
        this.predictionRepository = EnergyPredictionRepository.getInstance(appContext);
        this.dataChecker = new DataChecker(appContext);
    }

    public static synchronized PredictionManager getInstance(Context context) {
        if (instance == null) {
            instance = new PredictionManager(context);
        }
        return instance;
    }

    public LiveData<PredictionStatus> getStatus() {
        return status;
    }

    public LiveData<String> getStatusMessage() {
        return statusMessage;
    }

    public LiveData<EnergyPredictionResult> getLastResult() {
        return lastResult;
    }

    /**
     * Start the prediction generation process
     * This runs asynchronously and updates LiveData which UI observes
     */
    public void generateForecast(boolean checkDataFirst) {
        if (status.getValue() == PredictionStatus.LOADING || status.getValue() == PredictionStatus.CHECKING_DATA) {
            Log.d(TAG, "Prediction already in progress, ignoring request");
            return;
        }

        setStatus(PredictionStatus.CHECKING_DATA, "Checking your data...");

        if (checkDataFirst) {
            dataChecker.hasMinimumDataForPredictions(new DataChecker.DataCheckCallback() {
                @Override
                public void onResult(DataChecker.DataCheckResult result) {
                    if (!result.hasAnyData) {
                        setStatus(PredictionStatus.ERROR, "Insufficient data (Need HR, Sleep, or Test)");
                    } else {
                        startGeminiPrediction();
                    }
                }

                @Override
                public void onError(Exception e) {
                    setStatus(PredictionStatus.ERROR, "Data Check Failed");
                }
            });
        } else {
            startGeminiPrediction();
        }
    }

    private void startGeminiPrediction() {
        setStatus(PredictionStatus.LOADING, "Analyzing health data with Gemini AI...");

        predictionService.getEnergyPredictions12Hours()
            .thenAccept(result -> {
                Log.d(TAG, "Gemini prediction successful");
                
                // Save to DB via repository (which updates cache)
                saveApiResultsToDb(result);
                
                // Update LiveData (must be on main thread)
                postResult(result);
                setStatus(PredictionStatus.SUCCESS, "Forecast updated with AI!");
            })
            .exceptionally(e -> {
                Log.e(TAG, "Gemini prediction failed", e);
                setStatus(PredictionStatus.ERROR, "AI Error: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
                return null;
            });
    }

    private void saveApiResultsToDb(EnergyPredictionResult result) {
         if (result.getHourlyPredictions() == null) return;
         
         List<PredictionLocal> entities = new ArrayList<>();
         long startTime = System.currentTimeMillis();
         
         // Save current
         PredictionLocal current = new PredictionLocal();
         current.predictedLevel = result.getCurrentEnergyLevel() != null ? result.getCurrentEnergyLevel() : 50.0;
         current.explanation = result.getExplanation();
         current.actionableInsight = result.getActionableInsight();
         current.predictionTime = startTime;
         current.synced = false;
         entities.add(current);
         
         // Save hourly forecast (starting from index 1)
         List<Double> hourly = result.getHourlyPredictions();
         for (int i = 1; i < hourly.size(); i++) {
             PredictionLocal p = new PredictionLocal();
             p.predictedLevel = hourly.get(i);
             p.explanation = result.getExplanation(); // Persist explanation for all hourly points
             p.actionableInsight = result.getActionableInsight(); // Persist insight for all hourly points
             p.predictionTime = startTime + (i * 3600000L); // Future hours
             p.synced = false;
             entities.add(p);
         }
         
         predictionRepository.saveAll(entities);
    }

    private void setStatus(PredictionStatus newStatus, String message) {
        // Use postValue to ensure main thread delivery from background
        status.postValue(newStatus);
        statusMessage.postValue(message);
    }
    
    private void postResult(EnergyPredictionResult result) {
        lastResult.postValue(result);
    }
}

