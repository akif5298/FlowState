package com.flowstate.app.ui.energy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.flowstate.app.R;
import com.flowstate.app.data.collection.GoogleFitManager;
import com.flowstate.app.data.models.BiometricData;
import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.data.models.ProductivitySuggestion;
import com.flowstate.app.ml.EnergyPredictor;
import com.flowstate.app.llm.LLMService;

import java.util.List;
import java.text.SimpleDateFormat;

public class EnergyPredictionActivity extends AppCompatActivity {
    
    private TextView tvPredictions, tvSuggestions, tvAdvice;
    private Button btnLoadData;
    private GoogleFitManager fitManager;
    private EnergyPredictor energyPredictor;
    private LLMService llmService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_prediction);
        
        initializeViews();
        fitManager = new GoogleFitManager(this);
        energyPredictor = new EnergyPredictor();
        llmService = new LLMService();
        
        btnLoadData.setOnClickListener(v -> loadAndPredict());
    }
    
    private void initializeViews() {
        tvPredictions = findViewById(R.id.tvPredictions);
        tvSuggestions = findViewById(R.id.tvSuggestions);
        tvAdvice = findViewById(R.id.tvAdvice);
        btnLoadData = findViewById(R.id.btnLoadData);
    }
    
    private void loadAndPredict() {
        if (!fitManager.isSignedIn()) {
            tvPredictions.setText("Please connect to Google Fit first");
            return;
        }
        
        btnLoadData.setEnabled(false);
        tvPredictions.setText("Loading data...");
        
        fitManager.readCombinedBiometricData(24, new GoogleFitManager.BiometricCallback() {
            @Override
            public void onSuccess(List<BiometricData> data) {
                runOnUiThread(() -> {
                    if (data.isEmpty()) {
                        tvPredictions.setText("No data available. Please collect some biometric data first.");
                        btnLoadData.setEnabled(true);
                        return;
                    }
                    
                    // Generate predictions for next 12 hours
                    List<EnergyPrediction> predictions = energyPredictor.predictEnergyLevels(data, 12);
                    
                    // Display predictions
                    displayPredictions(predictions);
                    
                    // Generate suggestions
                    List<ProductivitySuggestion> suggestions = llmService.generateSchedule(predictions);
                    displaySuggestions(suggestions);
                    
                    // Generate general advice
                    String advice = llmService.generateGeneralAdvice(predictions);
                    tvAdvice.setText("Advice: " + advice);
                    
                    btnLoadData.setEnabled(true);
                });
            }
            
            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    tvPredictions.setText("Error loading data: " + e.getMessage());
                    btnLoadData.setEnabled(true);
                });
            }
        });
    }
    
    private void displayPredictions(List<EnergyPrediction> predictions) {
        StringBuilder sb = new StringBuilder();
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        
        for (EnergyPrediction pred : predictions) {
            String time = sdf.format(pred.getTimestamp());
            String level = pred.getPredictedLevel().toString();
            String emoji = ""; // Removed emojis for modern design
            String confidence = String.format("%.0f%%", pred.getConfidence() * 100);
            
            sb.append(time)
              .append(" - ").append(level).append(" (").append(confidence).append(")\n");
        }
        
        tvPredictions.setText(sb.toString());
    }
    
    private void displaySuggestions(List<ProductivitySuggestion> suggestions) {
        StringBuilder sb = new StringBuilder();
        
        int count = 0;
        for (ProductivitySuggestion suggestion : suggestions) {
            if (count >= 4) break; // Show first 4 suggestions
            count++;
            
            sb.append(suggestion.getTimeSlot()).append("\n")
              .append(suggestion.getSuggestedActivity()).append("\n")
              .append(suggestion.getReasoning()).append("\n\n");
        }
        
        tvSuggestions.setText(sb.toString());
    }
}

