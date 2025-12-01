package com.personaleenergy.app.ui.reaction;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;
import com.personaleenergy.app.R;
import com.personaleenergy.app.data.local.entities.ReactionLocal;
import com.personaleenergy.app.data.local.repo.ReactionTimeRepository;
import com.personaleenergy.app.data.collection.ReactionTimeCollector;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ReactionTimeActivity extends AppCompatActivity {
    
    private static final int NUM_TRIALS = 5;
    
    private TextView tvInstructions, tvResult, tvTrialCount;
    private Button btnStart, btnTap;
    private ReactionTimeCollector collector;
    private ReactionTimeRepository repository;
    private Handler mainHandler;
    
    private List<Integer> reactionTimes;
    private int currentTrial = 0;
    private boolean waitingForColorChange = false;
    private long startTime;
    private Random random;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reaction_time);
        
        initializeViews();
        collector = new ReactionTimeCollector();
        repository = new ReactionTimeRepository(this);
        mainHandler = new Handler(Looper.getMainLooper());
        random = new Random();
        reactionTimes = new ArrayList<>();
        
        btnStart.setOnClickListener(v -> startTest());
        btnTap.setOnClickListener(v -> recordReaction());
        
        resetTest();
    }
    
    private void initializeViews() {
        tvInstructions = findViewById(R.id.tvInstructions);
        tvResult = findViewById(R.id.tvResult);
        // tvTrialCount may not exist in layout - check for resource existence first
        int trialCountId = getResources().getIdentifier("tvTrialCount", "id", getPackageName());
        tvTrialCount = trialCountId != 0 ? findViewById(trialCountId) : null;
        btnStart = findViewById(R.id.btnStart);
        btnTap = findViewById(R.id.btnTap);
        
        btnTap.setEnabled(false);
        updateButtonColorHex("#4CAF50"); // Green
    }
    
    private void resetTest() {
        currentTrial = 0;
        reactionTimes.clear();
        waitingForColorChange = false;
        btnStart.setEnabled(true);
        btnTap.setEnabled(false);
        updateButtonColorHex("#4CAF50");
        tvInstructions.setText("Press Start to begin test");
        tvResult.setText("Ready to test");
        updateTrialCount();
    }
    
    private void updateTrialCount() {
        if (tvTrialCount != null) {
            tvTrialCount.setText(String.format("Trial %d/%d", currentTrial, NUM_TRIALS));
        }
    }
    
    private void startTest() {
        if (currentTrial >= NUM_TRIALS) {
            // All trials completed, calculate median and save
            finishAllTrials();
            return;
        }
        
        waitingForColorChange = true;
        btnStart.setEnabled(false);
        btnTap.setEnabled(true);
        tvInstructions.setText("⏳ Wait for the color to change...");
        updateButtonColorHex("#F44336"); // Red
        
        // Random delay between 2-5 seconds
        long randomDelay = random.nextInt(3000) + 2000; // 2000-5000ms
        
        mainHandler.postDelayed(() -> {
            if (waitingForColorChange) {
                waitingForColorChange = false;
                startTime = System.currentTimeMillis();
                runOnUiThread(() -> {
                    tvInstructions.setText("TAP NOW!");
                    updateButtonColorHex("#4CAF50"); // Green
                });
            }
        }, randomDelay);
    }
    
    private void recordReaction() {
        if (waitingForColorChange) {
            // Too early - reset this trial
            tvInstructions.setText("Too early! Wait for the color change...");
            updateButtonColorHex("#F44336"); // Red
            // Restart this trial after a short delay
            mainHandler.postDelayed(() -> startTest(), 1000);
            return;
        }
        
        long endTime = System.currentTimeMillis();
        int reactionTimeMs = (int) (endTime - startTime);
        
        // Record reaction time
        reactionTimes.add(reactionTimeMs);
        currentTrial++;
        
        // Show result for this trial
        String resultText = String.format("Trial %d: %d ms", currentTrial, reactionTimeMs);
        tvResult.setText(resultText);
        updateTrialCount();
        
        if (currentTrial >= NUM_TRIALS) {
            // All trials completed
            finishAllTrials();
        } else {
            // Continue to next trial
            btnStart.setEnabled(true);
            btnTap.setEnabled(false);
            tvInstructions.setText("Press Start for next trial");
            updateButtonColorHex("#4CAF50");
        } 
    }
    
    private void finishAllTrials() {
        // Validate that we have reaction times
        if (reactionTimes == null || reactionTimes.isEmpty()) {
            android.util.Log.e("ReactionTimeActivity", "No reaction times to save");
            Toast.makeText(this, "Error: No reaction times recorded. Please complete the test.", Toast.LENGTH_LONG).show();
            return;
        }
        
        // Calculate median
        List<Integer> sortedTimes = new ArrayList<>(reactionTimes);
        Collections.sort(sortedTimes);
        
        int medianMs;
        if (sortedTimes.size() == 0) {
            android.util.Log.e("ReactionTimeActivity", "Empty sorted times list");
            Toast.makeText(this, "Error: No valid reaction times to save.", Toast.LENGTH_LONG).show();
            return;
        } else if (sortedTimes.size() % 2 == 0) {
            // Even number of trials - average of two middle values
            int mid1 = sortedTimes.get(sortedTimes.size() / 2 - 1);
            int mid2 = sortedTimes.get(sortedTimes.size() / 2);
            medianMs = (mid1 + mid2) / 2;
        } else {
            // Odd number of trials - middle value
            medianMs = sortedTimes.get(sortedTimes.size() / 2);
        }
        
        android.util.Log.d("ReactionTimeActivity", "Calculated median: " + medianMs + " ms from " + sortedTimes.size() + " trials");
        
        // Make final copy for use in lambda
        final int finalMedianMs = medianMs;
        final int finalTestCount = sortedTimes.size();
        
        // Create and save ReactionLocal entity
        // Add small random offset to timestamp to avoid unique constraint violations
        long timestamp = System.currentTimeMillis() + (long)(Math.random() * 100);
        ReactionLocal reactionLocal = new ReactionLocal(
            timestamp,
            medianMs,
            finalTestCount
        );
        
        android.util.Log.d("ReactionTimeActivity", "Saving reaction test: timestamp=" + timestamp + ", medianMs=" + medianMs + ", testCount=" + finalTestCount);
        
        // Save to Room database with callback
        repository.save(reactionLocal, new ReactionTimeRepository.DataCallback<Long>() {
            @Override
            public void onSuccess(Long id) {
                android.util.Log.d("ReactionTimeActivity", "Successfully saved reaction test with id: " + id);
                runOnUiThread(() -> {
                    // Show success message
                    Toast.makeText(ReactionTimeActivity.this, "Test saved to database! (Median: " + finalMedianMs + " ms)", Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onError(Exception error) {
                android.util.Log.e("ReactionTimeActivity", "Error saving reaction test", error);
                runOnUiThread(() -> {
                    String errorMsg = error.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = error.getClass().getSimpleName();
                    }
                    Toast.makeText(ReactionTimeActivity.this, "Error saving test: " + errorMsg, Toast.LENGTH_LONG).show();
                });
            }
        });
        
        // Show final results
        String resultText = String.format(
                "Median: %d ms\n\n" +
                "All Trials:\n%s",
                medianMs,
                formatReactionTimes()
        );
        tvResult.setText(resultText);
        tvInstructions.setText("🎉 Test Complete!");
        btnStart.setText("Start New Test");
        btnStart.setEnabled(true);
        btnTap.setEnabled(false);
        updateButtonColorHex("#4CAF50");
        
        // Reset for next test
        mainHandler.postDelayed(() -> resetTest(), 3000);
    }
    
    private String formatReactionTimes() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < reactionTimes.size(); i++) {
            sb.append(String.format("Trial %d: %d ms", i + 1, reactionTimes.get(i)));
            if (i < reactionTimes.size() - 1) {
                sb.append("\n");
            }
        }
        return sb.toString();
    }
    
    private void updateButtonColorHex(String colorHex) {
        btnTap.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(colorHex)
            )
        );
    }
}
