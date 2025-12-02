package com.flowstate.app.ui.typing;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.flowstate.app.R;
import com.flowstate.app.data.collection.TypingSpeedCollector;
import com.flowstate.app.data.models.TypingSpeedData;

public class TypingSpeedActivity extends AppCompatActivity {
    
    private TextView tvSampleText, tvResult;
    private EditText etUserInput;
    private Button btnStart, btnFinish;
    private TypingSpeedCollector collector;
    private boolean testStarted = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_typing_speed);
        
        initializeViews();
        collector = new TypingSpeedCollector();
        
        // Initialize with sample text ready
        String sampleText = collector.prepareTest();
        tvSampleText.setText(sampleText);
        
        // Make Start button optional (just clears and prepares)
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
        etUserInput = findViewById(R.id.etUserInput);
        btnStart = findViewById(R.id.btnStart);
        btnFinish = findViewById(R.id.btnFinish);
        
        // Enable input immediately - will auto-start when typing begins
        etUserInput.setEnabled(true);
        etUserInput.setHint("Start typing to begin the test...");
        btnFinish.setEnabled(true);
        
        // Hide results card initially
        findViewById(R.id.cardResult).setVisibility(android.view.View.GONE);
    }
    
    private void prepareTest() {
        // Just reset and prepare the test
        String sampleText = collector.prepareTest();
        tvSampleText.setText(sampleText);
        etUserInput.setText("");
        etUserInput.setEnabled(true);
        etUserInput.requestFocus();
        testStarted = false;
        btnStart.setText("Reset");
        btnStart.setEnabled(true);
        btnFinish.setEnabled(true);
        
        // Hide results card
        findViewById(R.id.cardResult).setVisibility(android.view.View.GONE);
    }
    
    private void startTest() {
        // Actually start the timer when first character is typed
        if (!testStarted) {
            collector.startTest();
            testStarted = true;
            btnStart.setText("Reset");
            btnStart.setEnabled(true); // Keep enabled as reset button
            
            // Enable finish button
            btnFinish.setEnabled(true);
            
            // Hide results card if visible
            findViewById(R.id.cardResult).setVisibility(android.view.View.GONE);
            
            // Clear hint when test starts
            etUserInput.setHint("Keep typing...");
        }
    }
    
    private void finishTest() {
        if (!testStarted) {
            // If test hasn't started, can't finish
            return;
        }
        
        TypingSpeedData result = collector.finishTest();
        
        etUserInput.setEnabled(false);
        btnFinish.setEnabled(false);
        btnStart.setEnabled(true);
        btnStart.setText("Start");
        testStarted = false;
        
        String resultText = String.format(
                "Words Per Minute\n%d WPM\n\n" +
                "Accuracy\n%.1f%%",
                result.getWordsPerMinute(),
                result.getAccuracy()
        );
        tvResult.setText(resultText);
        
        // Show results card with animation
        findViewById(R.id.cardResult).setVisibility(android.view.View.VISIBLE);
        findViewById(R.id.cardResult).setAlpha(0f);
        findViewById(R.id.cardResult).animate().alpha(1f).setDuration(300);
    }
}

