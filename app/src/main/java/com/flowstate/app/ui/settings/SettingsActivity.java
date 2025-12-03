package com.flowstate.app.ui.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.flowstate.app.R;
import com.flowstate.app.ui.EnergyDashboardActivity;
import com.flowstate.app.ui.data.DataLogsActivity;
import com.flowstate.app.ui.schedule.AIScheduleActivity;
import com.flowstate.app.ui.insights.WeeklyInsightsActivity;
import com.flowstate.services.HealthConnectManager;
import com.flowstate.core.Config;

import androidx.activity.result.ActivityResultLauncher;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity {
    
    private static final String PREFS_NAME = "flowstate_settings";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_GEMINI_API_KEY = "gemini_api_key";
    
    private BottomNavigationView bottomNav;
    private SwitchMaterial switchDarkMode;
    private MaterialButton btnHealthConnectPermissions;
    private MaterialButton btnDebugHealthConnect;
    private TextInputEditText etGeminiApiKey;
    private MaterialButton btnSaveGeminiApiKey;
    private MaterialButton btnDeleteData;
    private SharedPreferences prefs;
    private HealthConnectManager healthConnectManager;
    private ActivityResultLauncher<Set<String>> requestPermissionsLauncher;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        healthConnectManager = new HealthConnectManager(this);
        
        // Initialize permission launcher
        requestPermissionsLauncher = registerForActivityResult(
            PermissionController.createRequestPermissionResultContract(),
            granted -> {
                if (granted.containsAll(healthConnectManager.getRequiredPermissions())) {
                    Toast.makeText(this, "Health Connect permissions granted", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, "Permissions not fully granted", Toast.LENGTH_LONG).show();
                }
            }
        );
        
        setupBottomNavigation();
        initializeViews();
        loadSettings();
    }
    
    private void initializeViews() {
        switchDarkMode = findViewById(R.id.switchDarkMode);
        btnHealthConnectPermissions = findViewById(R.id.btnHealthConnectPermissions);
        btnDebugHealthConnect = findViewById(R.id.btnDebugHealthConnect);
        etGeminiApiKey = findViewById(R.id.etGeminiApiKey);
        btnSaveGeminiApiKey = findViewById(R.id.btnSaveGeminiApiKey);
        btnDeleteData = findViewById(R.id.btnDeleteData);
        
        // Dark mode toggle
        switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            saveDarkMode(isChecked);
            applyDarkMode(isChecked);
        });
        
        // Health Connect permissions
        btnHealthConnectPermissions.setOnClickListener(v -> {
            requestHealthConnectPermissions();
        });
        
        // Debug Health Connect
        if (btnDebugHealthConnect != null) {
            btnDebugHealthConnect.setOnClickListener(v -> {
                startActivity(new Intent(this, com.flowstate.app.ui.HealthConnectDebugActivity.class));
            });
        }
        
        // Save Gemini API key
        btnSaveGeminiApiKey.setOnClickListener(v -> {
            saveGeminiApiKey();
        });
        
        // Delete all data
        if (btnDeleteData != null) {
            btnDeleteData.setOnClickListener(v -> {
                showDeleteConfirmationDialog();
            });
        }
    }
    
    private void showDeleteConfirmationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Delete All Data")
            .setMessage("Are you sure you want to delete all stored data? This cannot be undone.")
            .setPositiveButton("Delete", (dialog, which) -> {
                deleteAllData();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void deleteAllData() {
        // Run in background
        new Thread(() -> {
             com.flowstate.data.local.AppDb db = com.flowstate.data.local.AppDb.getInstance(this);
             db.clearAllTables();
             runOnUiThread(() -> 
                 Toast.makeText(this, "All local data deleted", Toast.LENGTH_SHORT).show()
             );
        }).start();
    }

    private void loadSettings() {
        // Load dark mode
        boolean darkMode = prefs.getBoolean(KEY_DARK_MODE, false);
        switchDarkMode.setChecked(darkMode);
        applyDarkMode(darkMode);
        
        // Load Gemini API key (masked)
        String apiKey = prefs.getString(KEY_GEMINI_API_KEY, null);
        if (apiKey != null && !apiKey.isEmpty()) {
            // Show masked version
            etGeminiApiKey.setText("••••••••••••••••");
            etGeminiApiKey.setHint("API key saved (tap to change)");
        } else {
            // Check if set in Config (from local.properties)
            if (Config.GEMINI_API_KEY != null && !Config.GEMINI_API_KEY.isEmpty()) {
                etGeminiApiKey.setHint("API key set in local.properties");
            }
        }
    }
    
    private void saveDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }
    
    private void applyDarkMode(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    private void requestHealthConnectPermissions() {
        try {
            if (!healthConnectManager.isAvailable()) {
                Toast.makeText(this, "Health Connect is not available on this device", Toast.LENGTH_LONG).show();
                return;
            }
            
            // Get required permissions
            var permissions = healthConnectManager.getRequiredPermissions();
            
            // Request permissions using launcher
            requestPermissionsLauncher.launch(permissions);
            
        } catch (Exception e) {
            Toast.makeText(this, "Error requesting permissions: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
    
    private void saveGeminiApiKey() {
        String apiKey = etGeminiApiKey.getText().toString().trim();
        
        if (apiKey.isEmpty() || apiKey.equals("••••••••••••••••")) {
            Toast.makeText(this, "Please enter a valid API key", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Save to SharedPreferences
        prefs.edit().putString(KEY_GEMINI_API_KEY, apiKey).apply();
        
        // Note: This won't update Config.GEMINI_API_KEY at runtime since it's loaded from BuildConfig
        // The user would need to rebuild with the key in local.properties for it to take effect
        // But we can use the saved key directly in GeminiEnergyPredictor
        
        Toast.makeText(this, "API key saved", Toast.LENGTH_SHORT).show();
        etGeminiApiKey.setText("••••••••••••••••");
        etGeminiApiKey.setHint("API key saved (tap to change)");
    }
    
    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_settings) {
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(this, EnergyDashboardActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, AIScheduleActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_data) {
                startActivity(new Intent(this, DataLogsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (itemId == R.id.nav_insights) {
                startActivity(new Intent(this, WeeklyInsightsActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_settings);
    }
}

