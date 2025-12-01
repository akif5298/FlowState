package com.personaleenergy.app.ui.typing;

import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;
import com.personaleenergy.app.R;
import com.personaleenergy.data.local.entities.TypingLocal;
import com.personaleenergy.data.local.repo.TypingSpeedRepository;
import com.personaleenergy.app.data.collection.TypingSpeedCollector;

public class TypingSpeedActivity extends AppCompatActivity {
    
    private static final int TEST_DURATION_SECONDS = 60;
    
    private TextView tvSampleText, tvResult, tvTimer;
    private EditText etUserInput;
    private Button btnStart, btnFinish;
    private TypingSpeedCollector collector;
    private TypingSpeedRepository repository;
    private CountDownTimer timer;
    private boolean testStarted = false;
    private long startTime;
    private String sampleText;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typing_speed);
        
        initializeViews();
        collector = new TypingSpeedCollector();
        repository = new TypingSpeedRepository(this);
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize with sample text ready
        sampleText = collector.prepareTest();
        tvSampleText.setText(sampleText);
        
        btnStart.setOnClickListener(v -> prepareTest());
        btnFinish.setOnClickListener(v -> finishTest());
        
        // Auto-start when user starts typing
        etUserInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Auto-start test on first character typed
                if (!testStarted && s.length() > 0) {
                    startTest();
                }
                
                // Record typing after test has started
                if (testStarted) {
                    collector.recordTyping(s.toString());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void initializeViews() {
        tvSampleText = findViewById(R.id.tvSampleText);
        tvResult = findViewById(R.id.tvResult);
        // tvTimer may not exist in layout - check for resource existence first
        int timerId = getResources().getIdentifier("tvTimer", "id", getPackageName());
        tvTimer = timerId != 0 ? findViewById(timerId) : null;
        etUserInput = findViewById(R.id.etUserInput);
        btnStart = findViewById(R.id.btnStart);
        btnFinish = findViewById(R.id.btnFinish);
        
        // Initialize timer display
        if (tvTimer != null) {
            tvTimer.setText("60");
        }
        
        // Enable input immediately - will auto-start when typing begins
        etUserInput.setEnabled(true);
        etUserInput.setHint("Start typing to begin the test...");
        btnFinish.setEnabled(false);
        
        // Hide results card initially
        View cardResult = findViewById(R.id.cardResult);
        if (cardResult != null) {
            cardResult.setVisibility(View.GONE);
        }
    }
    
    private void prepareTest() {
        // Cancel any existing timer
        if (timer != null) {
            timer.cancel();
        }
        
        // Reset and prepare the test
        sampleText = collector.prepareTest();
        tvSampleText.setText(sampleText);
        etUserInput.setText("");
        etUserInput.setEnabled(true);
        etUserInput.requestFocus();
        testStarted = false;
        btnStart.setText("Reset");
        btnStart.setEnabled(true);
        btnFinish.setEnabled(false);
        
        // Reset timer display
        if (tvTimer != null) {
            tvTimer.setText(String.valueOf(TEST_DURATION_SECONDS));
        }
        
        // Hide results card
        View cardResult = findViewById(R.id.cardResult);
        if (cardResult != null) {
            cardResult.setVisibility(View.GONE);
        }
    }
    
    private void startTest() {
        if (!testStarted) {
            startTime = System.currentTimeMillis();
            collector.startTest();
            testStarted = true;
            btnStart.setText("Reset");
            btnStart.setEnabled(true);
            btnFinish.setEnabled(true);
            
            // Hide results card if visible
            View cardResult = findViewById(R.id.cardResult);
            if (cardResult != null) {
                cardResult.setVisibility(View.GONE);
            }
            
            // Clear hint when test starts
            etUserInput.setHint("Keep typing...");
            
            // Start 60 second countdown timer
            startTimer();
        }
    }
    
    private void startTimer() {
        if (timer != null) {
            timer.cancel();
        }
        
        timer = new CountDownTimer(TEST_DURATION_SECONDS * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                int secondsRemaining = (int) (millisUntilFinished / 1000);
                if (tvTimer != null) {
                    tvTimer.setText(String.valueOf(secondsRemaining));
                }
            }
            
            @Override
            public void onFinish() {
                // Timer finished, auto-finish test
                finishTest();
            }
        };
        timer.start();
    }
    
    private void finishTest() {
        if (!testStarted) {
            return;
        }
        
        // Cancel timer
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        int durationSecs = (int) (durationMs / 1000);
        
        // Get typed text
        String typedText = etUserInput.getText().toString();
        int totalChars = typedText.length();
        
        // Calculate errors (character-by-character comparison)
        int errors = calculateErrors(sampleText, typedText);
        
        // Calculate WPM = (chars/5) / minutes
        double minutes = durationSecs / 60.0;
        int wpm = (int) ((totalChars / 5.0) / minutes);
        
        // Calculate accuracy = correct/total * 100
        int correctChars = totalChars - errors;
        double accuracy = totalChars > 0 ? (correctChars * 100.0 / totalChars) : 0.0;
        
        // Cap accuracy at 100%
        if (accuracy > 100.0) {
            accuracy = 100.0;
        }
        
        // Make final copies for use in lambda
        final int finalWpm = wpm;
        final double finalAccuracy = accuracy;
        
        // Create and save TypingLocal entity
        long timestamp = System.currentTimeMillis();
        TypingLocal typingLocal = new TypingLocal(
            timestamp,
            wpm,
            accuracy,
            totalChars,
            errors,
            durationSecs,
            sampleText
        );
        
        // Save to Room database with callback
        repository.save(typingLocal, new TypingSpeedRepository.DataCallback<Long>() {
            @Override
            public void onSuccess(Long id) {
                runOnUiThread(() -> {
                    // Show success message
                    Toast.makeText(TypingSpeedActivity.this, "Test saved to database! (WPM: " + finalWpm + ", Accuracy: " + String.format("%.1f%%", finalAccuracy) + ")", Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    Log.e("TypingSpeedActivity", "Error saving typing test", error);
                    Toast.makeText(TypingSpeedActivity.this, "Error saving test: " + error.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
        
        // Update UI
        etUserInput.setEnabled(false);
        btnFinish.setEnabled(false);
        btnStart.setEnabled(true);
        btnStart.setText("Start");
        testStarted = false;
        
        String resultText = String.format(
                "Words Per Minute\n%d WPM\n\n" +
                "Accuracy\n%.1f%%\n\n" +
                "Duration\n%d seconds",
                wpm,
                accuracy,
                durationSecs
        );
        tvResult.setText(resultText);
        
        // Show results card with animation
        View cardResult = findViewById(R.id.cardResult);
        if (cardResult != null) {
            cardResult.setVisibility(View.VISIBLE);
            cardResult.setAlpha(0f);
            cardResult.animate().alpha(1f).setDuration(300);
        }
    }
    
    /**
     * Calculate number of errors by comparing original and typed text character by character
     */
    private int calculateErrors(String original, String typed) {
        int errors = 0;
        int minLength = Math.min(original.length(), typed.length());
        
        // Count character mismatches
        for (int i = 0; i < minLength; i++) {
            if (original.charAt(i) != typed.charAt(i)) {
                errors++;
            }
        }
        
        // Add extra characters as errors
        if (typed.length() > original.length()) {
            errors += (typed.length() - original.length());
        }
        
        // Missing characters as errors
        if (original.length() > typed.length()) {
            errors += (original.length() - typed.length());
        }
        
        return errors;
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (timer != null) {
            timer.cancel();
        }
    }
}
