package com.flowstate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.text.TextUtils;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.WorkManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.flowstate.services.HealthConnectManager;
import com.flowstate.app.R;
import com.flowstate.workers.HeartRateSyncWorker;
import com.flowstate.workers.SleepSyncWorker;
import com.flowstate.workers.StepsSyncWorker;
import com.flowstate.workers.HrvSyncWorker;
import com.flowstate.workers.WorkoutSyncWorker;
import com.flowstate.workers.SyncScheduler;
import com.flowstate.app.ui.settings.SettingsActivity;
import com.flowstate.app.ui.data.DataLogsActivity;
import com.flowstate.app.ui.insights.WeeklyInsightsActivity;
import com.flowstate.app.ui.typing.TypingSpeedActivity;
import com.flowstate.app.ui.reaction.ReactionTimeActivity;
import com.flowstate.services.EnergyPredictionService;
import com.flowstate.app.data.models.EnergyLevel;
import com.flowstate.app.data.models.EnergyPredictionResult;
import java.util.List;
import java.util.Locale;

import com.flowstate.data.local.entities.PredictionLocal;
import com.flowstate.data.local.repo.EnergyPredictionRepository;
import com.flowstate.domain.features.FeatureRow;
import com.flowstate.domain.features.FeatureService;
import com.flowstate.domain.ml.EnergyPredictor;
import com.flowstate.services.DataChecker;
import com.flowstate.app.ui.OnboardingActivity;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.os.Handler;
import android.os.Looper;

import com.flowstate.app.ui.charts.ChartManager;
import com.github.mikephil.charting.charts.LineChart;
import com.google.android.material.slider.Slider;
import com.flowstate.data.local.entities.ManualEnergyInputLocal;
import com.flowstate.data.local.dao.ManualEnergyInputDao;
import com.flowstate.data.local.AppDb;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

// New imports for dialog
import android.view.LayoutInflater;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.chip.Chip;
import com.google.android.material.button.MaterialButtonToggleGroup;
import com.flowstate.services.DeviceUsageCollector;
import com.flowstate.services.WeatherCollector;

import android.widget.ProgressBar; // Added import

import com.flowstate.services.PredictionManager; // Added
import com.google.android.material.snackbar.Snackbar; // Added

import com.flowstate.app.ui.history.HistoryActivity; // Added
import androidx.appcompat.widget.Toolbar; // Added
import android.view.Menu; // Added
import android.view.MenuItem; // Added

public class EnergyDashboardActivity extends AppCompatActivity {
    
    private static final long CHECK_IN_PREFILL_WINDOW_MS = 12 * 60 * 60 * 1000L;
    
    private BottomNavigationView bottomNav;
    private TextView tvCurrentEnergy, tvEnergyLevel, tvAIInsight, tvPredictionStatus, tvLastCheckInSummary;
    private MaterialCardView cardGraph;
    private Button btnSyncNow, btnGenerateForecast;
    private LineChart energyChart;
    private ProgressBar progressBarPrediction; 
    // private Slider sliderEnergy; // Removed
    private Button btnDailyCheckIn; 
    
    private HealthConnectManager hcManager;
    private WorkManager workManager;
    // private EnergyPredictionService predictionService; // Removed, now managed by PredictionManager
    private PredictionManager predictionManager; // Added
    
    // Prediction generation components
    private FeatureService featureService;
    private EnergyPredictor energyPredictor;
    private EnergyPredictionRepository predictionRepository;
    private DataChecker dataChecker;
    private ExecutorService executor;
    private Handler mainHandler;
    
    // private boolean isUpdating = false; // Managed by PredictionManager now
    
    private String currentTechnicalExplanation = "No details available.";
    
    private com.flowstate.services.HealthDataAggregator healthAggregator;
    
    // New collectors
    private DeviceUsageCollector deviceUsageCollector;
    private WeatherCollector weatherCollector;
    private ManualEnergyInputDao manualInputDao;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Check if onboarding is needed
        if (!OnboardingActivity.isOnboardingComplete(this)) {
            // Redirect to onboarding
            startActivity(new Intent(this, OnboardingActivity.class));
            finish();
            return;
        }

        setContentView(R.layout.activity_energy_dashboard);
        
        manualInputDao = AppDb.getInstance(this).manualEnergyInputDao();
        
        // Initialize services for prediction
        featureService = new FeatureService(this);
        energyPredictor = new EnergyPredictor();
        predictionRepository = EnergyPredictionRepository.getInstance(this); // Use singleton
        dataChecker = new DataChecker(this);
        healthAggregator = new com.flowstate.services.HealthDataAggregator(this);
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // Initialize PredictionManager
        predictionManager = PredictionManager.getInstance(this);
        
        // Initialize collectors
        deviceUsageCollector = new DeviceUsageCollector(this);
        weatherCollector = new WeatherCollector(this);
        
        initializeViews();
        
        // Setup Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        
        setupBottomNavigation();
        setupClickListeners();
        setupObservers(); // Added
        
        // Load initial data
        loadEnergyData();
        loadTodayPredictions();
        refreshLastCheckInSummary();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        
        if (id == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        } else if (id == R.id.action_help) {
            showHelpDialog();
            return true;
        }
        
        return super.onOptionsItemSelected(item);
    }

    private void showHelpDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(R.string.help_title)
            .setMessage(getString(R.string.help_message_format, getString(R.string.authors_list)))
            .setPositiveButton(R.string.close, null)
            .show();
    }

    private void setupObservers() {
        predictionManager.getStatus().observe(this, status -> {
            switch (status) {
                case CHECKING_DATA:
                case LOADING:
                    if (progressBarPrediction != null) progressBarPrediction.setVisibility(View.VISIBLE);
                    if (btnGenerateForecast != null) btnGenerateForecast.setEnabled(false);
                    break;
                case SUCCESS:
                case ERROR:
                    if (progressBarPrediction != null) progressBarPrediction.setVisibility(View.GONE);
                    if (btnGenerateForecast != null) btnGenerateForecast.setEnabled(true);
                    
                    if (status == PredictionManager.PredictionStatus.SUCCESS) {
                         // Refresh data from repository/cache
                         loadEnergyData();
                         Toast.makeText(this, R.string.forecast_updated, Toast.LENGTH_SHORT).show();
                    }
                    break;
                default:
                    if (progressBarPrediction != null) progressBarPrediction.setVisibility(View.GONE);
                    if (btnGenerateForecast != null) btnGenerateForecast.setEnabled(true);
            }
        });
        
        predictionManager.getStatusMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                updateStatus(message);
                if (message.startsWith("AI Error")) {
                    Toast.makeText(this, message, Toast.LENGTH_LONG).show();
                }
            }
        });
    }
    
    private void initializeViews() {
        bottomNav = findViewById(R.id.bottomNavigation);
        tvCurrentEnergy = findViewById(R.id.tvCurrentEnergy);
        tvEnergyLevel = findViewById(R.id.tvEnergyLevel);
        tvAIInsight = findViewById(R.id.tvAIInsight);
        cardGraph = findViewById(R.id.cardGraph);
        tvPredictionStatus = findViewById(R.id.tvPredictionStatus);
        progressBarPrediction = findViewById(R.id.progressBarPrediction);
        btnGenerateForecast = findViewById(R.id.btnGenerateForecast);
        
        energyChart = findViewById(R.id.energyChart);
        // sliderEnergy = findViewById(R.id.sliderEnergy); // Removed
        
        // Find new button (or handle if null for safety)
        btnDailyCheckIn = findViewById(R.id.btnDailyCheckIn);
        tvLastCheckInSummary = findViewById(R.id.tvLastCheckInSummary);
        
        // Setup Chart
        ChartManager.setupLineChart(energyChart, "Energy Forecast");
        
        // btnSyncNow may not exist in layout - check for resource existence first
        int syncNowId = getResources().getIdentifier("btnSyncNow", "id", getPackageName());
        btnSyncNow = syncNowId != 0 ? findViewById(syncNowId) : null;
        
        // Initialize managers
        hcManager = new HealthConnectManager(this);
        workManager = WorkManager.getInstance(this);
        // predictionService = new EnergyPredictionService(this); // Removed
        
        // Set up sync button if present
        if (btnSyncNow != null) {
            btnSyncNow.setOnClickListener(v -> {
                v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
                syncNow();
            });
            
            // Update button state based on availability
            boolean isAvailable = hcManager.isAvailable();
            btnSyncNow.setEnabled(isAvailable);
            btnSyncNow.setVisibility(isAvailable ? View.VISIBLE : View.GONE);
            
            // Ensure hourly sync is scheduled if available
            if (isAvailable) {
                SyncScheduler.scheduleHourlySync(this);
            }
        }
        
        // Null safety checks
        if (bottomNav == null) {
            android.util.Log.e("EnergyDashboard", "bottomNavigation not found in layout");
        }
    }
    
    private void setupBottomNavigation() {
        if (bottomNav == null) {
            return;
        }
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                // Already here, no action needed
                return true;
            } else if (itemId == R.id.nav_data) {
                startActivity(new Intent(this, DataLogsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_insights) {
                startActivity(new Intent(this, WeeklyInsightsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            }
            return false;
        });
        
        // Set dashboard as selected
        if (bottomNav != null) {
            bottomNav.setSelectedItemId(R.id.nav_dashboard);
        }
    }
    
    private void setupClickListeners() {
        // Energy Score click listener for detailed explanation
        if (tvCurrentEnergy != null) {
            tvCurrentEnergy.setOnClickListener(v -> showDetailedExplanation());
        }
        if (tvEnergyLevel != null) {
            tvEnergyLevel.setOnClickListener(v -> showDetailedExplanation());
        }

        // Manual check-in button
        if (btnDailyCheckIn != null) {
            btnDailyCheckIn.setOnClickListener(v -> showManualEntryDialog());
        }
        
        View cardDailyCheckIn = findViewById(R.id.cardDailyCheckIn);
        if (cardDailyCheckIn != null) {
            cardDailyCheckIn.setOnClickListener(v -> showManualEntryDialog());
        }
        
        // Generate forecast button
        if (btnGenerateForecast != null) {
            btnGenerateForecast.setOnClickListener(v -> generateToday());
        }
        
        // Cognitive tests
        View cardTypingTest = findViewById(R.id.cardTypingTest);
        if (cardTypingTest != null) {
            cardTypingTest.setOnClickListener(v -> {
                startActivity(new Intent(this, TypingSpeedActivity.class));
            });
        }
        
        View cardReactionTest = findViewById(R.id.cardReactionTest);
        if (cardReactionTest != null) {
            cardReactionTest.setOnClickListener(v -> {
                startActivity(new Intent(this, ReactionTimeActivity.class));
            });
        }
    }
    
    private void showManualEntryDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        LayoutInflater inflater = this.getLayoutInflater();
        View dialogView = inflater.inflate(R.layout.dialog_manual_entry, null);
        builder.setView(dialogView);
        
        final android.app.AlertDialog dialog = builder.create();
        
        // Find CheckBoxes
        android.widget.CheckBox cbEnergy = dialogView.findViewById(R.id.cbEnergyInput);
        android.widget.CheckBox cbPhysical = dialogView.findViewById(R.id.cbPhysicalTired);
        android.widget.CheckBox cbMental = dialogView.findViewById(R.id.cbMentalTired);
        android.widget.CheckBox cbMeal = dialogView.findViewById(R.id.cbMealImpact);
        android.widget.CheckBox cbAccuracy = dialogView.findViewById(R.id.cbPredictionAccuracy);
        android.widget.CheckBox cbTask = dialogView.findViewById(R.id.cbRecentTask);
        android.widget.CheckBox cbCaffeine = dialogView.findViewById(R.id.cbCaffeine);
        android.widget.CheckBox cbEmotion = dialogView.findViewById(R.id.cbEmotion);

        // Find Input Views
        Slider sliderEnergy = dialogView.findViewById(R.id.sliderEnergyInput);
        Slider sliderPhysical = dialogView.findViewById(R.id.sliderPhysicalTired);
        Slider sliderMental = dialogView.findViewById(R.id.sliderMentalTired);
        RadioGroup rgMeal = dialogView.findViewById(R.id.rgMealImpact);
        MaterialButtonToggleGroup toggleAccuracy = dialogView.findViewById(R.id.toggleAccuracy);
        
        // Buttons in ToggleGroup need manual disabling
        View btnLow = dialogView.findViewById(R.id.btnInaccuratelyLow);
        View btnAccurate = dialogView.findViewById(R.id.btnAccurate);
        View btnHigh = dialogView.findViewById(R.id.btnInaccuratelyHigh);
        
        com.google.android.material.textfield.TextInputLayout tilRecentTask = dialogView.findViewById(R.id.tilRecentTask);
        TextInputEditText etRecentTask = dialogView.findViewById(R.id.etRecentTask);
        
        android.widget.NumberPicker npCaffeine = dialogView.findViewById(R.id.npCaffeine);
        ChipGroup chipGroupEmotion = dialogView.findViewById(R.id.chipGroupEmotion);
        Button btnSave = dialogView.findViewById(R.id.btnSaveCheckIn);
        
        // Setup Caffeine Picker
        npCaffeine.setMinValue(0);
        npCaffeine.setMaxValue(10);
        npCaffeine.setValue(0);
        
        // --- Setup Logic: Disable all inputs initially ---
        // Sliders
        sliderEnergy.setEnabled(false);
        sliderPhysical.setEnabled(false);
        sliderMental.setEnabled(false);
        
        // RadioGroup
        for (int i = 0; i < rgMeal.getChildCount(); i++) {
            rgMeal.getChildAt(i).setEnabled(false);
        }
        
        // Accuracy Buttons
        btnLow.setEnabled(false);
        btnAccurate.setEnabled(false);
        btnHigh.setEnabled(false);
        
        // Text Input
        tilRecentTask.setEnabled(false);
        etRecentTask.setEnabled(false);
        
        // Caffeine
        npCaffeine.setEnabled(false);
        
        // Emotion Chips
        for (int i = 0; i < chipGroupEmotion.getChildCount(); i++) {
            chipGroupEmotion.getChildAt(i).setEnabled(false);
            chipGroupEmotion.getChildAt(i).setClickable(false);
        }

        // --- Setup Listeners to Enable/Disable ---
        cbEnergy.setOnCheckedChangeListener((buttonView, isChecked) -> sliderEnergy.setEnabled(isChecked));
        cbPhysical.setOnCheckedChangeListener((buttonView, isChecked) -> sliderPhysical.setEnabled(isChecked));
        cbMental.setOnCheckedChangeListener((buttonView, isChecked) -> sliderMental.setEnabled(isChecked));
        
        cbMeal.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (int i = 0; i < rgMeal.getChildCount(); i++) {
                rgMeal.getChildAt(i).setEnabled(isChecked);
            }
        });
        
        cbAccuracy.setOnCheckedChangeListener((buttonView, isChecked) -> {
            btnLow.setEnabled(isChecked);
            btnAccurate.setEnabled(isChecked);
            btnHigh.setEnabled(isChecked);
            if (!isChecked) toggleAccuracy.clearChecked();
        });
        
        cbTask.setOnCheckedChangeListener((buttonView, isChecked) -> {
            tilRecentTask.setEnabled(isChecked);
            etRecentTask.setEnabled(isChecked);
        });
        
        cbCaffeine.setOnCheckedChangeListener((buttonView, isChecked) -> npCaffeine.setEnabled(isChecked));
        
        cbEmotion.setOnCheckedChangeListener((buttonView, isChecked) -> {
            for (int i = 0; i < chipGroupEmotion.getChildCount(); i++) {
                chipGroupEmotion.getChildAt(i).setEnabled(isChecked);
                chipGroupEmotion.getChildAt(i).setClickable(isChecked);
            }
            if (!isChecked) chipGroupEmotion.clearCheck();
        });
        
        btnSave.setOnClickListener(v -> {
            // Gather data ONLY if checkbox is checked
            
            Integer finalEnergy = cbEnergy.isChecked() ? (int) sliderEnergy.getValue() : null; 
            Integer finalPhysical = cbPhysical.isChecked() ? (int) sliderPhysical.getValue() : null;
            Integer finalMental = cbMental.isChecked() ? (int) sliderMental.getValue() : null;
            
            String mealImpact = null;
            if (cbMeal.isChecked()) {
                int selectedMealId = rgMeal.getCheckedRadioButtonId();
                if (selectedMealId == R.id.rbSluggish) mealImpact = "Sluggish";
                else if (selectedMealId == R.id.rbSharp) mealImpact = "Sharp";
                else if (selectedMealId == R.id.rbNeutral) mealImpact = "Neutral";
            }
            
            Integer accuracyRating = null; 
            if (cbAccuracy.isChecked()) {
                int selectedAccuracyId = toggleAccuracy.getCheckedButtonId();
                if (selectedAccuracyId == R.id.btnAccurate) accuracyRating = 0;
                else if (selectedAccuracyId == R.id.btnInaccuratelyHigh) accuracyRating = 1;
                else if (selectedAccuracyId == R.id.btnInaccuratelyLow) accuracyRating = -1;
            }
            
            String recentTask = null;
            if (cbTask.isChecked()) {
                recentTask = etRecentTask.getText().toString().trim();
                if (recentTask.isEmpty()) recentTask = null;
            }
            
            Integer caffeineCups = cbCaffeine.isChecked() ? npCaffeine.getValue() : null;
            
            String emotion = null;
            if (cbEmotion.isChecked()) {
                int selectedChipId = chipGroupEmotion.getCheckedChipId();
                if (selectedChipId != View.NO_ID) {
                    Chip chip = dialogView.findViewById(selectedChipId);
                    emotion = chip.getText().toString();
                }
            }
            
            // Only save if at least one field is provided? 
            // Or just save anyway, as even a null record serves as a timestamped "check-in attempt"?
            // The prompt implies we want to save reported data points.
            // If all null, maybe skip saving?
            boolean hasData = finalEnergy != null || finalPhysical != null || finalMental != null || 
                              mealImpact != null || accuracyRating != null || recentTask != null || 
                              caffeineCups != null || emotion != null;
                              
            if (hasData) {
                saveComprehensiveCheckIn(finalEnergy, finalPhysical, finalMental, mealImpact, accuracyRating, recentTask, caffeineCups, emotion);
                Toast.makeText(this, "Check-in saved", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "No data to save", Toast.LENGTH_SHORT).show();
            }
            dialog.dismiss();
        });
        
        dialog.show();
        
        populateDialogWithLatestInput(
            dialog,
            cbEnergy,
            sliderEnergy,
            cbPhysical,
            sliderPhysical,
            cbMental,
            sliderMental,
            cbMeal,
            rgMeal,
            cbAccuracy,
            toggleAccuracy,
            cbTask,
            etRecentTask,
            cbCaffeine,
            npCaffeine,
            cbEmotion,
            chipGroupEmotion
        );
    }
    
    private void saveComprehensiveCheckIn(Integer energy, Integer physical, Integer mental, String meal, Integer accuracyRating, String recentTask, Integer caffeineCups, String emotion) {
        executor.execute(() -> {
            try {
                // To support "only save what you edit" and session persistence:
                // We fetch the most recent input from today to merge values if needed, 
                // OR we just insert a new record with partial nulls and let the aggregator handle the "last valid value" logic.
                // The prompt says "while still in the app that self report data persists if u wanna add more data".
                // This implies we should fill the dialog with previous values if they exist from this session.
                // For database storage, it's cleaner to store a new snapshot.
                
                ManualEnergyInputLocal input = new ManualEnergyInputLocal();
                input.timestamp = System.currentTimeMillis();
                input.energyLevel = energy;
                input.physicalTiredness = physical;
                input.mentalTiredness = mental;
                input.mealImpact = meal;
                input.predictionAccuracyRating = accuracyRating;
                input.recentTask = recentTask;
                input.caffeineIntake = caffeineCups;
                input.currentEmotion = emotion;
                
                // ... (Device usage and weather collection remains the same)
                if (deviceUsageCollector.hasPermission()) {
                    input.deviceUsageSeconds = deviceUsageCollector.getTotalScreenTimeToday() / 1000;
                } else {
                    input.deviceUsageSeconds = -1L;
                }
                
                try {
                    WeatherCollector.WeatherInfo weather = weatherCollector.getCurrentWeather().join();
                    input.weatherCondition = weather.condition + ", " + weather.temperatureCelsius + "C";
                } catch (Exception e) {
                    input.weatherCondition = "Unknown";
                }
                
                ManualEnergyInputDao dao = manualInputDao != null ? manualInputDao : AppDb.getInstance(this).manualEnergyInputDao();
                dao.insert(input);
                
                // Save to session cache for UI repopulation (transient)
                // We can implement a static cache in PredictionManager or a SessionManager
                // For simplicity, let's just assume the DB insert is enough for now, 
                // and we'll fix the aggregator to look back.
                
                runOnUiThread(() -> {
                    // Toast.makeText(this, "Daily check-in saved!", Toast.LENGTH_SHORT).show();
                    // Requirement: Snackbar
                    View rootView = findViewById(android.R.id.content);
                    Snackbar.make(rootView, "Daily check-in saved!", Snackbar.LENGTH_LONG).show();
                    
                    updateLastCheckInSummaryText(input);
                });
            } catch (Exception e) {
                android.util.Log.e("EnergyDashboard", "Error saving check-in", e);
                runOnUiThread(() -> 
                    Toast.makeText(this, "Error saving check-in", Toast.LENGTH_SHORT).show()
                );
            }
        });
    }
    
    private void populateDialogWithLatestInput(
            android.app.AlertDialog dialog,
            android.widget.CheckBox cbEnergy,
            Slider sliderEnergy,
            android.widget.CheckBox cbPhysical,
            Slider sliderPhysical,
            android.widget.CheckBox cbMental,
            Slider sliderMental,
            android.widget.CheckBox cbMeal,
            RadioGroup rgMeal,
            android.widget.CheckBox cbAccuracy,
            MaterialButtonToggleGroup toggleAccuracy,
            android.widget.CheckBox cbTask,
            TextInputEditText etRecentTask,
            android.widget.CheckBox cbCaffeine,
            android.widget.NumberPicker npCaffeine,
            android.widget.CheckBox cbEmotion,
            ChipGroup chipGroupEmotion
    ) {
        if (manualInputDao == null) {
            return;
        }
        executor.execute(() -> {
            ManualEnergyInputLocal latest = manualInputDao.getMostRecent();
            if (latest == null) {
                return;
            }
            long age = System.currentTimeMillis() - latest.timestamp;
            if (age > CHECK_IN_PREFILL_WINDOW_MS) {
                return;
            }
            runOnUiThread(() -> {
                if (isFinishing() || !dialog.isShowing()) {
                    return;
                }
                if (latest.energyLevel != null) {
                    cbEnergy.setChecked(true);
                    sliderEnergy.setValue(latest.energyLevel.floatValue());
                }
                if (latest.physicalTiredness != null) {
                    cbPhysical.setChecked(true);
                    sliderPhysical.setValue(latest.physicalTiredness.floatValue());
                }
                if (latest.mentalTiredness != null) {
                    cbMental.setChecked(true);
                    sliderMental.setValue(latest.mentalTiredness.floatValue());
                }
                if (!TextUtils.isEmpty(latest.mealImpact)) {
                    cbMeal.setChecked(true);
                    if ("Sluggish".equalsIgnoreCase(latest.mealImpact)) {
                        rgMeal.check(R.id.rbSluggish);
                    } else if ("Sharp".equalsIgnoreCase(latest.mealImpact)) {
                        rgMeal.check(R.id.rbSharp);
                    } else if ("Neutral".equalsIgnoreCase(latest.mealImpact)) {
                        rgMeal.check(R.id.rbNeutral);
                    }
                }
                if (latest.predictionAccuracyRating != null) {
                    cbAccuracy.setChecked(true);
                    if (latest.predictionAccuracyRating == 0) {
                        toggleAccuracy.check(R.id.btnAccurate);
                    } else if (latest.predictionAccuracyRating > 0) {
                        toggleAccuracy.check(R.id.btnInaccuratelyHigh);
                    } else {
                        toggleAccuracy.check(R.id.btnInaccuratelyLow);
                    }
                }
                if (!TextUtils.isEmpty(latest.recentTask)) {
                    cbTask.setChecked(true);
                    etRecentTask.setText(latest.recentTask);
                }
                if (latest.caffeineIntake != null) {
                    cbCaffeine.setChecked(true);
                    int min = npCaffeine.getMinValue();
                    int max = npCaffeine.getMaxValue();
                    int value = Math.max(min, Math.min(max, latest.caffeineIntake));
                    npCaffeine.setValue(value);
                }
                if (!TextUtils.isEmpty(latest.currentEmotion)) {
                    cbEmotion.setChecked(true);
                    for (int i = 0; i < chipGroupEmotion.getChildCount(); i++) {
                        View child = chipGroupEmotion.getChildAt(i);
                        if (child instanceof Chip) {
                            Chip chip = (Chip) child;
                            if (chip.getText().toString().equalsIgnoreCase(latest.currentEmotion)) {
                                chipGroupEmotion.check(chip.getId());
                                break;
                            }
                        }
                    }
                }
            });
        });
    }
    
    private void refreshLastCheckInSummary() {
        if (manualInputDao == null || tvLastCheckInSummary == null) {
            return;
        }
        executor.execute(() -> {
            ManualEnergyInputLocal latest = manualInputDao.getMostRecent();
            runOnUiThread(() -> {
                if (isFinishing()) {
                    return;
                }
                updateLastCheckInSummaryText(latest);
            });
        });
    }
    
    private void updateLastCheckInSummaryText(ManualEnergyInputLocal input) {
        if (tvLastCheckInSummary == null) {
            return;
        }
        if (input == null) {
            tvLastCheckInSummary.setText("No check-ins yet");
            return;
        }
        tvLastCheckInSummary.setText(buildCheckInSummaryText(input));
    }
    
    private String buildCheckInSummaryText(ManualEnergyInputLocal input) {
        java.text.DateFormat timeFormat = android.text.format.DateFormat.getTimeFormat(this);
        String timeText = timeFormat.format(new Date(input.timestamp));
        List<String> parts = new ArrayList<>();
        if (input.energyLevel != null) {
            parts.add("Energy " + input.energyLevel + "/10");
        }
        if (input.physicalTiredness != null) {
            parts.add("Physical " + input.physicalTiredness + "/10");
        }
        if (input.mentalTiredness != null) {
            parts.add("Mental " + input.mentalTiredness + "/10");
        }
        if (!TextUtils.isEmpty(input.currentEmotion)) {
            parts.add("Mood " + input.currentEmotion);
        }
        if (input.caffeineIntake != null) {
            int cups = input.caffeineIntake;
            parts.add(cups + (cups == 1 ? " cup caffeine" : " cups caffeine"));
        }
        if (!TextUtils.isEmpty(input.mealImpact)) {
            parts.add(input.mealImpact);
        }
        if (parts.isEmpty()) {
            return "Last check-in at " + timeText;
        }
        return "Last check-in " + timeText + " · " + TextUtils.join(" · ", parts);
    }

    private void syncNow() {
        if (!hcManager.isAvailable()) {
            Toast.makeText(this, "Health Connect is not available", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Enqueue all sync workers
        workManager.enqueue(HeartRateSyncWorker.createWorkRequest());
        workManager.enqueue(SleepSyncWorker.createWorkRequest());
        workManager.enqueue(StepsSyncWorker.createWorkRequest());
        workManager.enqueue(HrvSyncWorker.createWorkRequest());
        workManager.enqueue(WorkoutSyncWorker.createWorkRequest());
        
        Toast.makeText(this, "Syncing health data...", Toast.LENGTH_SHORT).show();
        android.util.Log.d("EnergyDashboard", "Sync workers enqueued");
    }
    
    private void loadEnergyData() {
        // Show loading state
        if (tvCurrentEnergy != null) {
            tvCurrentEnergy.setText("...");
        }
        if (tvEnergyLevel != null) {
            tvEnergyLevel.setText("Loading...");
        }
        if (tvAIInsight != null) {
            tvAIInsight.setText("Checking for recent energy data...");
        }

        // Check for recent predictions first (cache strategy)
        predictionRepository.getLatestPrediction(new EnergyPredictionRepository.DataCallback<PredictionLocal>() {
            @Override
            public void onSuccess(PredictionLocal latest) {
                // We accept predictions up to 2 hours old as "current" enough for display to avoid constant re-fetching
                // The hourly chart will still show the future path
                long validityThreshold = System.currentTimeMillis() - (2 * 60 * 60 * 1000);
                
                if (latest != null && latest.predictionTime > validityThreshold) {
                    // Cache hit: Use recent data
                    runOnUiThread(() -> {
                        android.util.Log.d("EnergyDashboard", "Using cached prediction from " + new java.util.Date(latest.predictionTime));
                        updateUIWithPrediction(latest.predictedLevel, latest.explanation, latest.actionableInsight, null); 
                        
                        // Fetch the full range for the chart from DB
                        fetchPredictionsFromDb();
                    });
                } else {
                    // Cache miss or stale: Call API
                    runOnUiThread(() -> {
                        android.util.Log.d("EnergyDashboard", "No fresh prediction found, calling API");
                        fetchPredictionsFromApi();
                    });
                }
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> fetchPredictionsFromApi());
            }
        });
    }

    private void fetchPredictionsFromDb() {
        // Load predictions covering today AND tomorrow to ensure we get the full future forecast
        long now = System.currentTimeMillis();
        long startQuery = LocalDate.now().atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli();
        long endQuery = startQuery + (48 * 60 * 60 * 1000); // 48 hours window

        predictionRepository.getByDateRange(startQuery, endQuery, new EnergyPredictionRepository.DataCallback<List<PredictionLocal>>() {
            @Override
            public void onSuccess(List<PredictionLocal> predictions) {
                runOnUiThread(() -> {
                    if (predictions.isEmpty()) {
                        fetchPredictionsFromApi(); // Fallback if empty
                        return;
                    }

                    // Sort by time
                    predictions.sort((p1, p2) -> Long.compare(p1.predictionTime, p2.predictionTime));
                    
                    // Group predictions by "generation batch" (based on close timestamps or explanation similarity)
                    // Or more simply: find the most recent 'current' prediction and use ITS associated future points
                    
                    // 1. Find the latest prediction that is <= now (The "Current" Anchor)
                    PredictionLocal latestAnchor = null;
                    for (PredictionLocal p : predictions) {
                        if (p.predictionTime <= now + 60000) { 
                            latestAnchor = p;
                        } else {
                            break; 
                        }
                    }
                    
                    if (latestAnchor == null && !predictions.isEmpty()) {
                        // All predictions are in future? Grab first one.
                        latestAnchor = predictions.get(0);
                    }
                    
                    if (latestAnchor == null) {
                        fetchPredictionsFromApi();
                        return;
                    }

                    // 2. Filter the list to only include points that belong to this Anchor's "batch"
                    // We assume points in the same batch share the exact same explanation/insight string 
                    // AND are sequentially spaced.
                    List<Double> hourlyValues = new ArrayList<>();
                    PredictionLocal finalAnchor = latestAnchor;
                    
                    // Add the anchor itself first? 
                    // updateChart expects a list starting from current hour.
                    
                    // Let's iterate through predictions starting from the anchor's time
                    for (PredictionLocal p : predictions) {
                        if (p.predictionTime >= finalAnchor.predictionTime) {
                            // Check if it looks like part of the same batch (same explanation)
                            // This prevents mixing old future points with new ones if they overlap
                            if (p.explanation != null && p.explanation.equals(finalAnchor.explanation)) {
                                hourlyValues.add(p.predictedLevel);
                            }
                        }
                    }
                    
                    // Limit to 12 points
                    if (hourlyValues.size() > 12) {
                        hourlyValues = hourlyValues.subList(0, 12);
                    }

                    if (!hourlyValues.isEmpty()) {
                        updateUIWithPrediction(finalAnchor.predictedLevel, finalAnchor.explanation, finalAnchor.actionableInsight, hourlyValues);
                        updateStatus(getString(R.string.forecast_from_cache));
                    } else {
                        fetchPredictionsFromApi();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> fetchPredictionsFromApi());
            }
        });
    }

    private void fetchPredictionsFromApi() {
        if (tvAIInsight != null) {
            tvAIInsight.setText(R.string.calculating_energy);
        }
        
        // Use PredictionManager to fetch predictions
        predictionManager.generateForecast(false); // No need to re-check minimal data for auto-fetch, or maybe yes? Let's say false for speed or handled inside
    }
    
    // Removed old fetchPredictionsFromApi logic
    // Removed saveApiResultsToDb (now in PredictionManager)

    private void updateUIWithPrediction(Double currentEnergy, String technicalExplanation, String actionableInsight, List<Double> hourlyPredictions) {
        if (tvCurrentEnergy != null && currentEnergy != null) {
            tvCurrentEnergy.setText(String.format(Locale.getDefault(), "%.0f", currentEnergy));
        }
        
        // Store technical explanation for detail view
        if (technicalExplanation != null && !technicalExplanation.isEmpty()) {
            this.currentTechnicalExplanation = technicalExplanation;
        }
        
        // Convert numeric level to enum
        EnergyLevel level;
        if (currentEnergy != null) {
            if (currentEnergy >= 70) {
                level = EnergyLevel.HIGH;
            } else if (currentEnergy >= 40) {
                level = EnergyLevel.MEDIUM;
            } else {
                level = EnergyLevel.LOW;
            }
        } else {
            level = EnergyLevel.MEDIUM; // Default
        }
        
        if (tvEnergyLevel != null) {
            tvEnergyLevel.setText(level.toString());
        }
        
        // Display Gemini's actionable insight
        if (tvAIInsight != null) {
            if (actionableInsight != null && !actionableInsight.isEmpty()) {
                tvAIInsight.setText(actionableInsight);
            } else {
                // Fallback to generated insight
                String insight = generateInsight(currentEnergy != null ? currentEnergy : 50.0, level);
                tvAIInsight.setText(insight);
            }
        }
        
        // Update Chart
        if (hourlyPredictions != null && !hourlyPredictions.isEmpty()) {
            updateChart(hourlyPredictions);
        }
    }

    
    private void updateChart(List<Double> predictions) {
        if (energyChart == null) return;
        
        List<String> labels = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        int currentHour = cal.get(Calendar.HOUR_OF_DAY);
        
        for (int i = 0; i < predictions.size(); i++) {
            int hour = (currentHour + i) % 24;
            labels.add(String.format(Locale.getDefault(), "%02d:00", hour));
        }
        
        ChartManager.updateLineChart(energyChart, predictions, labels, "Energy Level");
    }
    
    // Removed old saveManualEnergyInput

    /**
     * Generate predictions for today
     */
    private void generateToday() {
        // Use PredictionManager to handle generation
        if (hcManager.isAvailable()) {
            predictionManager.generateForecast(true);
        } else {
            // Fallback to local model (still using activity logic for now, or move to manager later)
            proceedWithGeneration(); 
        }
    }

    /**
     * Show message when insufficient data is found, listing what is missing
     */
    private void showInsufficientDataDialog(DataChecker.DataCheckResult result) {
        StringBuilder missing = new StringBuilder();
        if (!result.hasHrData) missing.append(getString(R.string.missing_hr));
        if (!result.hasSleepData) missing.append(getString(R.string.missing_sleep));
        if (!result.hasTypingData) missing.append(getString(R.string.missing_typing));
        if (!result.hasReactionData) missing.append(getString(R.string.missing_reaction));
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle(R.string.more_data_needed_title)
                .setMessage(getString(R.string.more_data_needed_msg, missing.toString()))
                .setPositiveButton(R.string.add_data_btn, (dialog, which) -> {
                    dialog.dismiss();
                })
                .setCancelable(true)
                .show();
    }
    
    /**
     * Proceed with prediction generation after data check passes
     * (Legacy/Local Model only - used when Health Connect unavailable)
     */
    private void proceedWithGeneration() {
        // We still use local boolean for legacy local model generation since that's not in PredictionManager yet
        // If we move everything to PredictionManager, we can remove this.
        // For now, let's just update the status text.
        if (tvPredictionStatus != null) tvPredictionStatus.setText("Generating predictions for today...");
        
        executor.execute(() -> {
            try {
                LocalDate today = LocalDate.now();
                android.util.Log.d("EnergyDashboard", "Building features for today: " + today);
                
                // Step 1: Build features for today
                List<FeatureRow> features = featureService.buildFor(today);
                
                if (features.isEmpty()) {
                    mainHandler.post(() -> {
                        // No features available, show simple message
                        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(EnergyDashboardActivity.this);
                        builder.setTitle("No Data Found")
                                .setMessage("Oh! Looks like we have no data for you. Get started with our onboarding to begin tracking your energy levels!")
                                .setPositiveButton("Go to Onboarding", (dialog, which) -> {
                                    Intent intent = new Intent(EnergyDashboardActivity.this, OnboardingActivity.class);
                                    startActivity(intent);
                                })
                                .setNegativeButton("Cancel", (dialog, which) -> {
                                    dialog.dismiss();
                                })
                                .setCancelable(true)
                                .show();

                        if (btnGenerateForecast != null) {
                            btnGenerateForecast.setEnabled(true);
                        }
                    });
                    return;
                }
                
                // Step 2: Predict energy levels
                List<PredictionLocal> predictions = energyPredictor.predict(today, features);
                
                // Step 3: Save predictions locally (synced=false)
                predictionRepository.saveAll(predictions);
                
                // Step 4: Display predictions
                mainHandler.post(() -> {
                    // Update main UI components
                    loadEnergyData();
                    updateStatus("Predictions updated!");
                    if (btnGenerateForecast != null) {
                        btnGenerateForecast.setEnabled(true);
                    }
                    Toast.makeText(this, "Forecast updated!", Toast.LENGTH_SHORT).show();
                });
                
                } catch (Exception e) {
                android.util.Log.e("EnergyDashboard", "Error generating predictions", e);
                mainHandler.post(() -> {
                    updateStatus("Error generating predictions: " + e.getMessage());
                    if (btnGenerateForecast != null) {
                        btnGenerateForecast.setEnabled(true);
                    }
                    Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
            }
        });
    }

    /**
     * Load existing predictions for today to check status
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
                                    updateStatus(getString(R.string.tap_generate_forecast));
                                } else {
                                    updateStatus(getString(R.string.forecast_available));
                                }
                            });
                        }
                        
                        @Override
                        public void onError(Exception e) {
                            // Ignore error for status check
                        }
                    });
            } catch (Exception e) {
                // Ignore error
            }
        });
    }

    /**
     * Update status message
     */
    private void updateStatus(String message) {
        if (tvPredictionStatus != null) {
            tvPredictionStatus.setText(message);
        }
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        refreshLastCheckInSummary();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
    
    /**
     * Show detailed explanation dialog
     */
    private void showDetailedExplanation() {
        String explanation = currentTechnicalExplanation;
        String score = tvCurrentEnergy != null ? tvCurrentEnergy.getText().toString() : "--";
        
        // Fetch current health summary to show inputs
        executor.execute(() -> {
            com.flowstate.app.data.models.HealthDataSummary summary = healthAggregator.createHealthSummary(24);
            
            mainHandler.post(() -> {
                if (summary == null) {
                    // Handle null summary
                    new androidx.appcompat.app.AlertDialog.Builder(this)
                        .setTitle(getString(R.string.energy_score_analysis, score))
                        .setMessage(explanation + "\n\n" + getString(R.string.input_details_not_available))
                        .setPositiveButton(R.string.close, null)
                        .show();
                    return;
                }

                StringBuilder inputDetails = new StringBuilder();
                if (summary.getManualEnergyLevel() != null) {
                    inputDetails.append(getString(R.string.self_reported_energy, summary.getManualEnergyLevel())).append("\n");
                }
                if (summary.getPhysicalTiredness() != null) {
                    inputDetails.append(getString(R.string.physical_tiredness, summary.getPhysicalTiredness())).append("\n");
                }
                if (summary.getMentalTiredness() != null) {
                    inputDetails.append(getString(R.string.mental_tiredness, summary.getMentalTiredness())).append("\n");
                }
                if (summary.getMealImpact() != null) {
                    inputDetails.append(getString(R.string.meal_impact, summary.getMealImpact())).append("\n");
                }
                if (summary.getCurrentEmotion() != null) {
                    inputDetails.append(getString(R.string.current_emotion, summary.getCurrentEmotion())).append("\n");
                }
                if (summary.getCaffeineIntakeCups() != null) {
                    inputDetails.append(getString(R.string.caffeine_intake, summary.getCaffeineIntakeCups())).append("\n");
                }
                if (summary.getRecentTask() != null) {
                    inputDetails.append(getString(R.string.recent_task, summary.getRecentTask())).append("\n");
                }
                if (summary.getPredictionAccuracyRating() != null) {
                    String accuracyText;
                    if (summary.getPredictionAccuracyRating() == 0) {
                        accuracyText = getString(R.string.accuracy_accurate);
                    } else if (summary.getPredictionAccuracyRating() > 0) {
                        accuracyText = getString(R.string.accuracy_high);
                    } else {
                        accuracyText = getString(R.string.accuracy_low);
                    }
                    inputDetails.append(getString(R.string.last_prediction_accuracy, accuracyText)).append("\n");
                }
                
                inputDetails.append("• Sleep: ").append(summary.getLastNightSleepHours() != null && summary.getLastNightSleepHours() > 0 ? String.format(Locale.getDefault(), "%.1fh", summary.getLastNightSleepHours()) : "N/A").append("\n");
                inputDetails.append("• Heart Rate: ").append(summary.getAvgHeartRate() != null && summary.getAvgHeartRate() > 0 ? String.format(Locale.getDefault(), "%.0f bpm", summary.getAvgHeartRate()) : "N/A").append("\n");
                inputDetails.append("• HRV: ").append(summary.getAvgHRV() != null && summary.getAvgHRV() > 0 ? String.format(Locale.getDefault(), "%.0f ms", summary.getAvgHRV()) : "N/A").append("\n");
                inputDetails.append("• Steps: ").append(summary.getTodaySteps() != null ? summary.getTodaySteps() : "N/A").append("\n");
                inputDetails.append("• Typing: ").append(summary.getRecentTypingWPM() != null && summary.getRecentTypingWPM() > 0 ? String.format(Locale.getDefault(), "%.0f WPM", summary.getRecentTypingWPM()) : "N/A").append("\n");
                inputDetails.append("• Reaction: ").append(summary.getRecentReactionTimeMs() != null && summary.getRecentReactionTimeMs() > 0 ? summary.getRecentReactionTimeMs() + " ms" : "N/A").append("\n");
                
                new androidx.appcompat.app.AlertDialog.Builder(this)
                    .setTitle(getString(R.string.energy_score_analysis, score))
                    .setMessage(explanation + "\n\n" + inputDetails.toString())
                    .setPositiveButton(R.string.close, null)
                    .show();
            });
        });
    }

    /**
     * Generate AI insight based on energy level
     */
    private String generateInsight(double energyLevel, EnergyLevel level) {
        if (level == EnergyLevel.HIGH) {
            return getString(R.string.insight_high);
        } else if (level == EnergyLevel.MEDIUM) {
            return getString(R.string.insight_medium);
        } else {
            return getString(R.string.insight_low);
        }
    }
}
