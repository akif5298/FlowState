package com.flowstate.app.ui.typing;

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
import com.flowstate.app.R;
import com.flowstate.data.local.entities.TypingLocal;
import com.flowstate.data.local.repo.TypingSpeedRepository;
import com.flowstate.app.data.collection.TypingSpeedCollector;

import android.text.style.ForegroundColorSpan;
import android.text.Spannable;
import android.text.SpannableString;
import android.graphics.Color;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;

public class TypingSpeedActivity extends AppCompatActivity {
    
    private static final int TEST_DURATION_SECONDS = 30;
    
    private TextView tvSampleText, tvResult, tvTimer;
    private EditText etUserInput;
    private Button btnStart;
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
        
        if (savedInstanceState != null && savedInstanceState.containsKey("sampleText")) {
            sampleText = savedInstanceState.getString("sampleText");
            // If we had text, set it, but we still need to "prepare" the collector with it
            // However, collector.prepareTest() generates NEW text.
            // We should modify collector or just set it manually if possible, 
            // but collector stores currentSampleText private. 
            // Actually, I can just use the text I have and ignore collector's generation for now,
            // but finishTest relies on collector.currentSampleText (which is private).
            // This suggests I should rely on a fresh test on rotation for simplicity, 
            // UNLESS I expose setSampleText in Collector.
            // For now, I'll let it generate new text on rotation to avoid complex state restoration of the Collector.
            // But I will try to keep it if I can.
            // Let's just generate new text for now.
             sampleText = collector.prepareTest();
        } else {
            // Initialize with sample text ready
            sampleText = collector.prepareTest();
        }
        
        tvSampleText.setText(sampleText);
        
        btnStart.setOnClickListener(v -> prepareTest());
        
        // Auto-start when user starts typing
        etUserInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // Auto-start test on first character typed
                if (!testStarted && s.length() > 0) {
                    startTest();
                    // Force a tiny UI update to ensure timer is visible immediately?
                    if (tvTimer != null) tvTimer.setText(String.valueOf(TEST_DURATION_SECONDS));
                    Toast.makeText(TypingSpeedActivity.this, "Test Started!", Toast.LENGTH_SHORT).show();
                }
                
                // Record typing after test has started
                if (testStarted) {
                    collector.recordTyping(s.toString());
                }

                // Update visual feedback (fade correctly typed words)
                updateVisualFeedback(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }
    
    private void initializeViews() {
        tvSampleText = findViewById(R.id.tvSampleText);
        tvResult = findViewById(R.id.tvResult);
        // Direct ID lookup instead of dynamic string lookup for reliability
        tvTimer = findViewById(R.id.tvTimer); 
        // Fallback for dynamic if direct fails (though tvTimer is not in XML provided previously, let's add it)
        if (tvTimer == null) {
             int timerId = getResources().getIdentifier("tvTimer", "id", getPackageName());
             tvTimer = timerId != 0 ? findViewById(timerId) : null;
        }
        
        etUserInput = findViewById(R.id.etUserInput);
        btnStart = findViewById(R.id.btnStart);
        
        // Initialize timer display
        if (tvTimer != null) {
            tvTimer.setText(String.valueOf(TEST_DURATION_SECONDS));
        } else {
            // Log warning if timer view is missing
            Log.w("TypingSpeedActivity", "tvTimer view not found in layout");
        }
        
        // Enable input immediately - will auto-start when typing begins
        etUserInput.setEnabled(true);
        etUserInput.setHint("Start typing to begin the test...");
        
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

        // Hide keyboard
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(etUserInput.getWindowToken(), 0);
        }
        
        long endTime = System.currentTimeMillis();
        long durationMs = endTime - startTime;
        int durationSecs = (int) (durationMs / 1000);
        
        // Get typed text
        String typedText = etUserInput.getText().toString();
        
        // Use collector to calculate results properly
        collector.recordTyping(typedText);
        // We need to manually inject start/end times into collector if they weren't set correctly
        // But since we are calling finishTest, we can trust collector's logic if we set startTime
        // Actually, collector uses its own startTime. Let's rely on Activity's timing for display but Collector for data model?
        // Or better: Let the collector handle the math.
        
        // NOTE: The activity was calculating WPM independently. Let's align it with Collector's new logic.
        int totalChars = typedText.length();
        int errors = calculateErrors(sampleText, typedText);
        
        if (durationSecs < 1) durationSecs = 1;
        double minutes = durationSecs / 60.0;
        
        // Net WPM Calculation (Same as Collector)
        double grossWPM = (totalChars / 5.0) / minutes;
        double errorRate = errors / minutes;
        int wpm = (int) Math.max(0, grossWPM - errorRate);
        
        // Calculate accuracy
        int correctChars = totalChars - errors;
        double accuracy = totalChars > 0 ? (correctChars * 100.0 / totalChars) : 0.0;
        if (accuracy < 0) accuracy = 0;
        if (accuracy > 100.0) accuracy = 100.0;
        
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
                    Toast.makeText(TypingSpeedActivity.this, "Test saved! Net WPM: " + finalWpm, Toast.LENGTH_LONG).show();
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    Log.e("TypingSpeedActivity", "Error saving typing test", error);
                    Toast.makeText(TypingSpeedActivity.this, "Error saving test", Toast.LENGTH_SHORT).show();
                });
            }
        });
        
        // Update UI
        etUserInput.setEnabled(false);
        btnStart.setEnabled(true);
        btnStart.setText("Start");
        testStarted = false;
        
        String resultText = String.format(
                "Net Speed\n%d WPM\n\n" +
                "Accuracy\n%.1f%%\n\n" +
                "Errors\n%d",
                wpm,
                accuracy,
                errors
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
     * Updates the sample text with color highlighting based on user input.
     * Uses word-based alignment to prevent cascading errors.
     */
    private void updateVisualFeedback(String typed) {
        if (sampleText == null) return;

        SpannableString spannable = new SpannableString(sampleText);
        
        // Clear all existing spans first
        Object[] spans = spannable.getSpans(0, spannable.length(), Object.class);
        for (Object span : spans) {
            spannable.removeSpan(span);
        }

        // Split both texts into words to align them
        String[] sampleWords = sampleText.split("\\s+");
        String[] typedWords = typed.split("\\s+");
        
        // We need to track the actual character indices in the original string
        int sampleIndex = 0;
        
        // Iterate through words
        int maxWords = Math.min(sampleWords.length, typedWords.length);
        
        // Handle the case where the user ends with a space (meaning they are starting the next word)
        // split() drops trailing empty strings, so we need to be careful.
        boolean endsWithSpace = typed.endsWith(" ");
        int typedWordsCount = typedWords.length;
        if (endsWithSpace) {
            // The last "word" in typedWords is complete. The user is logically on the next word.
            // But for visual feedback of *past* words, split is fine.
        }

        for (int i = 0; i < maxWords; i++) {
            String sWord = sampleWords[i];
            String tWord = typedWords[i];
            
            // Find start of this word in sampleText (accounting for spaces)
            // We loop until we find the start of the word to skip multiple spaces if any
            while (sampleIndex < sampleText.length() && Character.isWhitespace(sampleText.charAt(sampleIndex))) {
                sampleIndex++;
            }
            
            int wordStartIndex = sampleIndex;
            
            // Compare characters in this word
            int charLimit = Math.min(sWord.length(), tWord.length());
            
            for (int k = 0; k < charLimit; k++) {
                if (sWord.charAt(k) == tWord.charAt(k)) {
                    // Match - Gray
                    spannable.setSpan(new ForegroundColorSpan(Color.LTGRAY), 
                        wordStartIndex + k, wordStartIndex + k + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    // Mismatch - Red
                    spannable.setSpan(new ForegroundColorSpan(Color.RED), 
                        wordStartIndex + k, wordStartIndex + k + 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
            }
            
            // If typed word is longer than sample word, mark the extra chars in sample as error?
            // No, we can't mark "extra" chars in sample because they don't exist.
            // We could mark the *rest* of the sample word as red if the typed word was shorter?
            // "missing characters" style logic.
            // If the user has *finished* this word (moved to next), then missing chars are errors.
            // We know the user finished this word if i < typedWords.length - 1 OR (i == last && endsWithSpace)
            boolean wordFinished = (i < typedWords.length - 1) || (i == typedWords.length - 1 && endsWithSpace);
            
            if (wordFinished && tWord.length() < sWord.length()) {
                // Mark remaining characters in this word as red (missing)
                spannable.setSpan(new ForegroundColorSpan(Color.RED), 
                    wordStartIndex + tWord.length(), wordStartIndex + sWord.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
            } else if (!wordFinished && tWord.length() < sWord.length()) {
                // Word not finished, don't mark remaining as error yet.
            }

            // Move sampleIndex past this word
            sampleIndex += sWord.length();
        }

        tvSampleText.setText(spannable);
    }

    /**
     * Calculate number of errors by comparing original and typed text word by word.
     * Prevents cascading errors when a word length mismatch occurs.
     */
    private int calculateErrors(String original, String typed) {
        if (original == null || typed == null) return 0;
        
        String[] sampleWords = original.split("\\s+");
        String[] typedWords = typed.split("\\s+");
        
        int errors = 0;
        int loopLimit = Math.min(sampleWords.length, typedWords.length);
        
        for (int i = 0; i < loopLimit; i++) {
            String sWord = sampleWords[i];
            String tWord = typedWords[i];
            
            int wordErrors = 0;
            int lengthLimit = Math.min(sWord.length(), tWord.length());
            
            // 1. Character mismatches
            for (int k = 0; k < lengthLimit; k++) {
                if (sWord.charAt(k) != tWord.charAt(k)) {
                    wordErrors++;
                }
            }
            
            // 2. Extra characters typed
            if (tWord.length() > sWord.length()) {
                wordErrors += (tWord.length() - sWord.length());
            }
            
            // 3. Missing characters (only if this isn't the *current* incomplete word)
            // If we are looking at the last typed word, and the user hasn't hit space, 
            // it might just be incomplete.
            // But for the final calculation in finishTest(), we assume typing is DONE.
            // So all missing characters count as errors.
            if (sWord.length() > tWord.length()) {
                wordErrors += (sWord.length() - tWord.length());
            }
            
            errors += wordErrors;
        }
        
        // 4. Missing words (entire words not typed)
        // If the user stopped early, do we count untyped words as errors?
        // Usually, WPM is based on *what was typed*. Untyped text is just not credited.
        // So we do NOT add errors for remaining words in sampleWords.
        
        // 5. Extra words? (if typed has more words than sample)
        if (typedWords.length > sampleWords.length) {
            for (int i = sampleWords.length; i < typedWords.length; i++) {
                errors += typedWords[i].length();
            }
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
