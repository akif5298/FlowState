package com.flowstate.app.ui.reaction;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.flowstate.app.R;
import com.flowstate.data.local.entities.ReactionLocal;
import com.flowstate.data.local.repo.ReactionTimeRepository;

import java.util.Random;

/**
 * Activity for reaction time test
 * Saves results to local Room database
 */
public class ReactionTimeActivity extends AppCompatActivity {
    
    private static final String TAG = "ReactionTimeActivity";
    private static final int MIN_WAIT_MS = 2000;
    private static final int MAX_WAIT_MS = 5000;
    
    private TextView tvInstructions, tvResult;
    private Button btnStart, btnTap;
    private ReactionTimeRepository repository;
    private Handler handler;
    
    private long colorChangeTime = 0;
    private boolean waitingForReaction = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reaction_time);
        
        repository = new ReactionTimeRepository(this);
        handler = new Handler(Looper.getMainLooper());
        
        initializeViews();
        setupListeners();
    }
    
    private void initializeViews() {
        tvInstructions = findViewById(R.id.tvInstructions);
        tvResult = findViewById(R.id.tvResult);
        btnStart = findViewById(R.id.btnStart);
        btnTap = findViewById(R.id.btnTap);
        
        btnTap.setEnabled(false);
        btnTap.setBackgroundColor(0xFFF44336); // Red
        tvInstructions.setText("Press Start to begin");
    }
    
    private void setupListeners() {
        btnStart.setOnClickListener(v -> startTest());
        btnTap.setOnClickListener(v -> recordReaction());
    }
    
    private void startTest() {
        btnStart.setEnabled(false);
        btnTap.setEnabled(true);
        btnTap.setBackgroundColor(0xFFF44336); // Red
        tvInstructions.setText("⏳ Wait for the color to change...");
        tvResult.setText("Ready...");
        waitingForReaction = false;
        
        // Random delay before color change
        Random random = new Random();
        int delay = MIN_WAIT_MS + random.nextInt(MAX_WAIT_MS - MIN_WAIT_MS);
        
        handler.postDelayed(() -> {
            colorChangeTime = System.currentTimeMillis();
            waitingForReaction = true;
            btnTap.setBackgroundColor(0xFF4CAF50); // Green
            tvInstructions.setText("TAP NOW!");
        }, delay);
    }
    
    private void recordReaction() {
        if (!waitingForReaction) {
            Toast.makeText(this, "Too early! Wait for green.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        long reactionTime = System.currentTimeMillis() - colorChangeTime;
        int reactionTimeMs = (int) reactionTime;
        
        // Save to database
        long timestamp = System.currentTimeMillis();
        ReactionLocal reactionLocal = new ReactionLocal(timestamp, reactionTimeMs, 1);
        
        repository.save(reactionLocal, new ReactionTimeRepository.DataCallback<Long>() {
            @Override
            public void onSuccess(Long id) {
                Log.d(TAG, "Reaction time test saved with id: " + id);
                
                // Small delay to ensure DB transaction completes, then trigger sync
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    androidx.work.OneTimeWorkRequest syncWork = 
                        com.flowstate.workers.RemoteSyncWorker.createWorkRequest();
                    androidx.work.WorkManager.getInstance(ReactionTimeActivity.this).enqueue(syncWork);
                    Log.d(TAG, "Remote sync triggered with fresh reaction data");
                }, 500); // 500ms delay
                
                runOnUiThread(() -> {
                    Toast.makeText(ReactionTimeActivity.this, 
                        "Test saved & syncing to backend!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error saving reaction time test", error);
                runOnUiThread(() -> {
                    Toast.makeText(ReactionTimeActivity.this, 
                        "Error saving test: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // Display result
        tvResult.setText(String.format("Your reaction time: %d ms", reactionTimeMs));
        tvInstructions.setText("🎉 Test Complete! Data saved.");
        
        btnTap.setEnabled(false);
        btnStart.setEnabled(true);
        btnTap.setBackgroundColor(0xFF4CAF50); // Green
        waitingForReaction = false;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        handler.removeCallbacksAndMessages(null);
    }
}
