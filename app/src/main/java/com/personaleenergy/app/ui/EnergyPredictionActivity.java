package com.personaleenergy.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.personaleenergy.app.R;
import com.personaleenergy.app.data.local.entities.PredictionLocal;
import com.personaleenergy.app.data.local.repo.EnergyPredictionRepository;
import com.personaleenergy.app.domain.features.FeatureRow;
import com.personaleenergy.app.domain.features.FeatureService;
import com.personaleenergy.app.domain.ml.EnergyPredictor;
import com.personaleenergy.app.ui.OnboardingActivity;

import java.text.SimpleDateFormat;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for generating and displaying energy predictions
 * 
 * On click "Generate today": calls FeatureService.buildFor(today) → 
 * EnergyPredictor.predict(today, feats) → save to local database
 */
public class EnergyPredictionActivity extends AppCompatActivity {
    
    private static final String TAG = "EnergyPredictionActivity";
    
    private TextView tvPredictions;
    private Button btnGenerateToday;
    private FeatureService featureService;
    private EnergyPredictor energyPredictor;
    private EnergyPredictionRepository predictionRepository;
    private ExecutorService executor;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_prediction);
        
        initializeViews();
        
        // Initialize services
        featureService = new FeatureService(this);
        energyPredictor = new EnergyPredictor();
        predictionRepository = new EnergyPredictionRepository(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Set up button click listener (with null check)
        if (btnGenerateToday != null) {
            btnGenerateToday.setOnClickListener(v -> generateToday());
        } else {
            Log.e(TAG, "btnGenerateToday not found in layout!");
        }
        
        // Load existing predictions for today
        loadTodayPredictions();
    }
    
    private void initializeViews() {
        tvPredictions = findViewById(R.id.tvPredictions);
        btnGenerateToday = findViewById(R.id.btnLoadData); // Use existing button ID
        
        // Update button text
        if (btnGenerateToday != null) {
            btnGenerateToday.setText("Generate Today");
        }
    }
    
    /**
     * Generate predictions for today
     */
    private void generateToday() {
        if (btnGenerateToday != null) {
            btnGenerateToday.setEnabled(false);
        }
        updateStatus("Generating predictions for today...");
        
        executor.execute(() -> {
            try {
                LocalDate today = LocalDate.now();
                Log.d(TAG, "Building features for today: " + today);
                
                // Step 1: Build features for today
                List<FeatureRow> features = featureService.buildFor(today);
                
                if (features.isEmpty()) {
                    mainHandler.post(() -> {
                        // No features available, show onboarding message
                        showNoDataMessage();
                        if (btnGenerateToday != null) {
                            btnGenerateToday.setEnabled(true);
                        }
                    });
                    return;
                }
                
                Log.d(TAG, "Built " + features.size() + " feature rows");
                
                // Step 2: Predict energy levels
                List<PredictionLocal> predictions = energyPredictor.predict(today, features);
                
                Log.d(TAG, "Generated " + predictions.size() + " predictions");
                
                // Step 3: Save predictions locally (synced=false)
                predictionRepository.saveAll(predictions);
                
                Log.d(TAG, "Saved " + predictions.size() + " predictions to local database");
                
                // Step 4: Display predictions
                mainHandler.post(() -> {
                    displayPredictions(predictions);
                    updateStatus("Predictions generated successfully!");
                    if (btnGenerateToday != null) {
                        btnGenerateToday.setEnabled(true);
                    }
                    Toast.makeText(this, "Predictions saved locally!", Toast.LENGTH_SHORT).show();
                });
                
            } catch (Exception e) {
                Log.e(TAG, "Error generating predictions", e);
                mainHandler.post(() -> {
                    updateStatus("Error generating predictions: " + e.getMessage());
                    if (btnGenerateToday != null) {
                        btnGenerateToday.setEnabled(true);
                    }
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }
    
    /**
     * Show message when no data is found
     */
    private void showNoDataMessage() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("No Data Found")
                .setMessage("Oh! Looks like we have no data for you. Get started with our onboarding to begin tracking your energy levels!")
                .setPositiveButton("Go to Onboarding", (dialog, which) -> {
                    Intent intent = new Intent(EnergyPredictionActivity.this, OnboardingActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }

    /**
     * Load existing predictions for today
     */
    private void loadTodayPredictions() {
        executor.execute(() -> {
            try {
                LocalDate today = LocalDate.now();
                long dayStartMs = today.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                long dayEndMs = today.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
                
                predictionRepository.getByDateRange(dayStartMs, dayEndMs, 
                    new EnergyPredictionRepository.DataCallback<List<PredictionLocal>>() {
                        @Override
                        public void onSuccess(List<PredictionLocal> predictions) {
                            mainHandler.post(() -> {
                                if (predictions.isEmpty()) {
                                    updateStatus("No predictions for today. Click 'Generate Today' to create predictions.");
                                } else {
                                    displayPredictions(predictions);
                                    updateStatus("Loaded " + predictions.size() + " predictions for today");
                                }
                            });
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            Log.e(TAG, "Error loading predictions", e);
                            mainHandler.post(() -> {
                                updateStatus("Error loading predictions");
                            });
                        }
                    });
            } catch (Exception e) {
                Log.e(TAG, "Error loading today's predictions", e);
            }
        });
    }
    
    /**
     * Display predictions in the UI
     */
    private void displayPredictions(List<PredictionLocal> predictions) {
        if (predictions.isEmpty()) {
            tvPredictions.setText("No predictions available.");
            return;
        }
        
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        
        for (PredictionLocal pred : predictions) {
            // Convert epoch milliseconds to Date
            java.util.Date date = new java.util.Date(pred.predictionTime);
            String time = sdf.format(date);
            String level = pred.level;
            String confidence = String.format(Locale.getDefault(), "%.0f%%", pred.confidence * 100);
            
            // Add color/emoji based on level
            String emoji = "";
            if ("HIGH".equals(level)) {
                emoji = "🟢";
            } else if ("MEDIUM".equals(level)) {
                emoji = "🟡";
            } else {
                emoji = "🔴";
            }
            
            sb.append(emoji)
              .append(" ").append(time)
              .append(" - ").append(level)
              .append(" (").append(confidence).append(" confidence)\n");
        }
        
        tvPredictions.setText(sb.toString());
    }
    
    /**
     * Update status message
     */
    private void updateStatus(String message) {
        // Update predictions TextView with status message
        tvPredictions.setText(message);
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}
