package com.flowstate.app.ui.typing;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.flowstate.app.R;
import com.flowstate.data.local.entities.TypingLocal;
import com.flowstate.data.local.repo.TypingSpeedRepository;

/**
 * Activity for typing speed test
 * Saves results to local Room database
 */
public class TypingSpeedActivity extends AppCompatActivity {
    
    private static final String TAG = "TypingSpeedActivity";
    private static final String SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog. " +
            "Code is poetry made to be read and understood by humans. " +
            "Technology should augment human intelligence not replace it.";
    
    private TextView tvSampleText, tvResult;
    private EditText etUserInput;
    private Button btnStart, btnFinish;
    private TypingSpeedRepository repository;
    
    private long startTime = 0;
    private boolean testStarted = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typing_speed);
        
        repository = new TypingSpeedRepository(this);
        initializeViews();
        setupListeners();
    }
    
    private void initializeViews() {
        tvSampleText = findViewById(R.id.tvSampleText);
        tvResult = findViewById(R.id.tvResult);
        etUserInput = findViewById(R.id.etUserInput);
        btnStart = findViewById(R.id.btnStart);
        btnFinish = findViewById(R.id.btnFinish);
        
        // Set up initial state
        tvSampleText.setText(SAMPLE_TEXT);
        etUserInput.setEnabled(true);
        btnFinish.setEnabled(false);
        
        // Hide results card initially
        findViewById(R.id.cardResult).setVisibility(android.view.View.GONE);
    }
    
    private void setupListeners() {
        btnStart.setOnClickListener(v -> startTest());
        btnFinish.setOnClickListener(v -> finishTest());
        
        etUserInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!testStarted && s.length() > 0) {
                    startTest();
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void startTest() {
        if (!testStarted) {
            startTime = System.currentTimeMillis();
            testStarted = true;
            btnStart.setText("Reset");
            btnFinish.setEnabled(true);
            etUserInput.setHint("Keep typing...");
            Log.d(TAG, "Test started");
        } else {
            // Reset
            resetTest();
        }
    }
    
    private void finishTest() {
        if (!testStarted) {
            return;
        }
        
        long endTime = System.currentTimeMillis();
        String typedText = etUserInput.getText().toString().trim();
        
        if (typedText.isEmpty()) {
            Toast.makeText(this, "Please type some text first", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Calculate metrics
        double durationMinutes = (endTime - startTime) / 60000.0;
        String[] words = typedText.split("\\s+");
        int wordCount = words.length;
        int wpm = (int) (wordCount / durationMinutes);
        
        // Calculate accuracy (simple character-based)
        int totalChars = typedText.length();
        int sampleChars = Math.min(SAMPLE_TEXT.length(), totalChars);
        int correctChars = 0;
        for (int i = 0; i < sampleChars; i++) {
            if (SAMPLE_TEXT.charAt(i) == typedText.charAt(i)) {
                correctChars++;
            }
        }
        double accuracy = (correctChars * 100.0) / sampleChars;
        int errors = sampleChars - correctChars;
        int durationSecs = (int) ((endTime - startTime) / 1000);
        
        // Save to database
        TypingLocal typingLocal = new TypingLocal(
            endTime,
            wpm,
            accuracy,
            totalChars,
            errors,
            durationSecs,
            SAMPLE_TEXT
        );
        
        repository.save(typingLocal, new TypingSpeedRepository.DataCallback<Long>() {
            @Override
            public void onSuccess(Long id) {
                Log.d(TAG, "Typing test saved with id: " + id);
                
                // Small delay to ensure DB transaction completes, then trigger sync
                new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
                    androidx.work.OneTimeWorkRequest syncWork = 
                        com.flowstate.workers.RemoteSyncWorker.createWorkRequest();
                    androidx.work.WorkManager.getInstance(TypingSpeedActivity.this).enqueue(syncWork);
                    Log.d(TAG, "Remote sync triggered with fresh typing data");
                }, 500); // 500ms delay
                
                runOnUiThread(() -> {
                    Toast.makeText(TypingSpeedActivity.this, 
                        "Test saved & syncing to backend!", Toast.LENGTH_SHORT).show();
                });
            }

            @Override
            public void onError(Exception error) {
                Log.e(TAG, "Error saving typing test", error);
                runOnUiThread(() -> {
                    Toast.makeText(TypingSpeedActivity.this, 
                        "Error saving test: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // Display results
        String resultText = String.format(
            "Words Per Minute: %d WPM\nAccuracy: %.1f%%\n\nData saved & synced!",
            wpm, accuracy
        );
        tvResult.setText(resultText);
        
        // Show results card
        findViewById(R.id.cardResult).setVisibility(android.view.View.VISIBLE);
        
        etUserInput.setEnabled(false);
        btnFinish.setEnabled(false);
        btnStart.setText("Start New Test");
        testStarted = false;
    }
    
    private void resetTest() {
        etUserInput.setText("");
        etUserInput.setEnabled(true);
        etUserInput.setHint("Start typing to begin the test...");
        tvResult.setText("");
        findViewById(R.id.cardResult).setVisibility(android.view.View.GONE);
        btnStart.setText("Start");
        btnFinish.setEnabled(false);
        testStarted = false;
        startTime = 0;
    }
}
