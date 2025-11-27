package com.personaleenergy.app.ui.manual;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.flowstate.app.R;
import com.flowstate.data.local.AppDb;
import android.content.Intent;
import com.flowstate.data.local.entities.HrLocal;
import com.flowstate.data.local.entities.SleepLocal;
import com.flowstate.data.local.entities.TypingLocal;
import com.flowstate.data.local.entities.ReactionLocal;
import com.personaleenergy.app.ui.typing.TypingSpeedActivity;
import com.personaleenergy.app.ui.reaction.ReactionTimeActivity;
import androidx.work.WorkManager;
import com.flowstate.workers.SupabaseSyncWorker;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Activity for manual data entry
 * 
 * Allows users to manually enter:
 * - Heart rate data
 * - Sleep data
 * - Typing speed test results
 * - Reaction time test results
 */
public class ManualDataEntryActivity extends AppCompatActivity {
    
    private static final String TAG = "ManualDataEntry";
    private AppDb db;
    private ExecutorService executor;
    private Handler mainHandler;
    
    // Heart Rate Section
    private EditText etHeartRate;
    private Button btnSaveHeartRate;
    
    // Sleep Section
    private EditText etSleepHours;
    private Button btnSaveSleep;
    
    // Typing Section
    private EditText etTypingWpm;
    private EditText etTypingAccuracy;
    private Button btnSaveTyping;
    private Button btnTakeTypingTest;
    
    // Reaction Section
    private EditText etReactionMs;
    private Button btnSaveReaction;
    private Button btnTakeReactionTest;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manual_data_entry);
        
        db = AppDb.getInstance(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        initializeViews();
        setupListeners();
    }
    
    private void initializeViews() {
        // Heart Rate
        etHeartRate = findViewById(R.id.etHeartRate);
        btnSaveHeartRate = findViewById(R.id.btnSaveHeartRate);
        
        // Sleep
        etSleepHours = findViewById(R.id.etSleepHours);
        btnSaveSleep = findViewById(R.id.btnSaveSleep);
        
        // Typing
        etTypingWpm = findViewById(R.id.etTypingWpm);
        etTypingAccuracy = findViewById(R.id.etTypingAccuracy);
        btnSaveTyping = findViewById(R.id.btnSaveTyping);
        // Try to find button for taking typing test
        int typingTestId = getResources().getIdentifier("btnTakeTypingTest", "id", getPackageName());
        btnTakeTypingTest = typingTestId != 0 ? findViewById(typingTestId) : null;
        
        // Reaction
        etReactionMs = findViewById(R.id.etReactionMs);
        btnSaveReaction = findViewById(R.id.btnSaveReaction);
        // Try to find button for taking reaction test
        int reactionTestId = getResources().getIdentifier("btnTakeReactionTest", "id", getPackageName());
        btnTakeReactionTest = reactionTestId != 0 ? findViewById(reactionTestId) : null;
    }
    
    private void setupListeners() {
        if (btnSaveHeartRate != null) {
            btnSaveHeartRate.setOnClickListener(v -> saveHeartRate());
        }
        
        if (btnSaveSleep != null) {
            btnSaveSleep.setOnClickListener(v -> saveSleep());
        }
        
        if (btnSaveTyping != null) {
            btnSaveTyping.setOnClickListener(v -> saveTyping());
        }
        
        if (btnSaveReaction != null) {
            btnSaveReaction.setOnClickListener(v -> saveReaction());
        }
        
        // Set up buttons to navigate to test screens
        if (btnTakeTypingTest != null) {
            btnTakeTypingTest.setOnClickListener(v -> {
                Intent intent = new Intent(ManualDataEntryActivity.this, TypingSpeedActivity.class);
                startActivity(intent);
            });
        }
        
        if (btnTakeReactionTest != null) {
            btnTakeReactionTest.setOnClickListener(v -> {
                Intent intent = new Intent(ManualDataEntryActivity.this, ReactionTimeActivity.class);
                startActivity(intent);
            });
        }
        
        // Also add prompts when users try to save typing/reaction without knowing values
    }
    
    private void saveHeartRate() {
        String hrText = etHeartRate.getText().toString().trim();
        if (hrText.isEmpty()) {
            Toast.makeText(this, "Please enter heart rate", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            int bpm = Integer.parseInt(hrText);
            if (bpm < 40 || bpm > 220) {
                Toast.makeText(this, "Please enter a valid heart rate (40-220 bpm)", Toast.LENGTH_SHORT).show();
                return;
            }
            
            long timestamp = System.currentTimeMillis();
            HrLocal hrLocal = new HrLocal(timestamp, bpm);
            
            executor.execute(() -> {
                try {
                    long id = db.hrDao().insert(hrLocal);
                    Log.d(TAG, "Saved heart rate to database with id: " + id);
                    // Trigger Supabase sync
                    WorkManager.getInstance(ManualDataEntryActivity.this)
                            .enqueue(SupabaseSyncWorker.createWorkRequest());
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Heart rate saved! Syncing to cloud...", Toast.LENGTH_LONG).show();
                        etHeartRate.setText("");
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving heart rate", e);
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Error saving heart rate", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveSleep() {
        String sleepText = etSleepHours.getText().toString().trim();
        if (sleepText.isEmpty()) {
            Toast.makeText(this, "Please enter sleep hours", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            double hours = Double.parseDouble(sleepText);
            if (hours < 0 || hours > 24) {
                Toast.makeText(this, "Please enter a valid sleep duration (0-24 hours)", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Calculate sleep start (assume sleep ended now)
            long sleepEnd = System.currentTimeMillis();
            int durationMinutes = (int) (hours * 60);
            long sleepStart = sleepEnd - (durationMinutes * 60 * 1000L);
            
            SleepLocal sleepLocal = new SleepLocal(sleepStart, sleepEnd, durationMinutes);
            
            executor.execute(() -> {
                try {
                    long id = db.sleepDao().insert(sleepLocal);
                    Log.d(TAG, "Saved sleep to database with id: " + id);
                    // Trigger Supabase sync
                    WorkManager.getInstance(ManualDataEntryActivity.this)
                            .enqueue(SupabaseSyncWorker.createWorkRequest());
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Sleep data saved! Syncing to cloud...", Toast.LENGTH_LONG).show();
                        etSleepHours.setText("");
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving sleep", e);
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Error saving sleep data", Toast.LENGTH_SHORT).show();
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveTyping() {
        String wpmText = etTypingWpm.getText().toString().trim();
        String accuracyText = etTypingAccuracy.getText().toString().trim();
        
        if (wpmText.isEmpty() || accuracyText.isEmpty()) {
            // Show prompt to take the test
            showTypingTestPrompt();
            return;
        }
        
        try {
            int wpm = Integer.parseInt(wpmText);
            double accuracy = Double.parseDouble(accuracyText);
            
            if (wpm < 0 || wpm > 300) {
                Toast.makeText(this, "Please enter a valid WPM (0-300)", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (accuracy < 0 || accuracy > 100) {
                Toast.makeText(this, "Please enter a valid accuracy (0-100)", Toast.LENGTH_SHORT).show();
                return;
            }
            
            long timestamp = System.currentTimeMillis();
            // Estimate other fields for manual entry
            int totalChars = (int) (wpm * 5); // Rough estimate
            int errors = (int) (totalChars * (1 - accuracy / 100.0));
            int durationSecs = 60; // Assume 60 second test
            
            TypingLocal typingLocal = new TypingLocal(
                timestamp, wpm, accuracy, totalChars, errors, durationSecs, "Manual entry"
            );
            
            executor.execute(() -> {
                try {
                    long id = db.typingDao().insert(typingLocal);
                    Log.d(TAG, "Saved typing test to database with id: " + id);
                    // Trigger Supabase sync
                    WorkManager.getInstance(ManualDataEntryActivity.this)
                            .enqueue(SupabaseSyncWorker.createWorkRequest());
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Typing test saved! Syncing to cloud...", Toast.LENGTH_LONG).show();
                        etTypingWpm.setText("");
                        etTypingAccuracy.setText("");
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving typing", e);
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Error saving typing test: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter valid numbers", Toast.LENGTH_SHORT).show();
        }
    }
    
    private void saveReaction() {
        String reactionText = etReactionMs.getText().toString().trim();
        if (reactionText.isEmpty()) {
            // Show prompt to take the test
            showReactionTestPrompt();
            return;
        }
        
        try {
            int medianMs = Integer.parseInt(reactionText);
            if (medianMs < 0 || medianMs > 5000) {
                Toast.makeText(this, "Please enter a valid reaction time (0-5000 ms)", Toast.LENGTH_SHORT).show();
                return;
            }
            
            long timestamp = System.currentTimeMillis();
            ReactionLocal reactionLocal = new ReactionLocal(timestamp, medianMs, 5); // Assume 5 trials
            
            executor.execute(() -> {
                try {
                    long id = db.reactionDao().insert(reactionLocal);
                    Log.d(TAG, "Saved reaction test to database with id: " + id);
                    // Trigger Supabase sync
                    WorkManager.getInstance(ManualDataEntryActivity.this)
                            .enqueue(SupabaseSyncWorker.createWorkRequest());
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Reaction test saved! Syncing to cloud...", Toast.LENGTH_LONG).show();
                        etReactionMs.setText("");
                    });
                } catch (Exception e) {
                    Log.e(TAG, "Error saving reaction", e);
                    mainHandler.post(() -> {
                        Toast.makeText(this, "Error saving reaction test: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
                }
            });
        } catch (NumberFormatException e) {
            Toast.makeText(this, "Please enter a valid number", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * Show prompt to take typing speed test
     */
    private void showTypingTestPrompt() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Don't know your typing speed?")
                .setMessage("Take our typing speed test to get accurate WPM and accuracy measurements!")
                .setPositiveButton("Take Test", (dialog, which) -> {
                    Intent intent = new Intent(ManualDataEntryActivity.this, TypingSpeedActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }
    
    /**
     * Show prompt to take reaction time test
     */
    private void showReactionTestPrompt() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Don't know your reaction time?")
                .setMessage("Take our reaction time test to get accurate measurements!")
                .setPositiveButton("Take Test", (dialog, which) -> {
                    Intent intent = new Intent(ManualDataEntryActivity.this, ReactionTimeActivity.class);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}

