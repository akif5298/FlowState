package com.personaleenergy.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.flowstate.app.R;
import com.flowstate.app.supabase.AuthService;
import com.flowstate.app.supabase.api.SupabaseAuthApi;
import com.google.android.material.snackbar.Snackbar;
import com.personaleenergy.app.ui.EnergyDashboardActivity;
import com.personaleenergy.app.ui.data.DataLogsActivity;
import com.personaleenergy.app.ui.schedule.AIScheduleActivity;
import com.personaleenergy.app.ui.insights.WeeklyInsightsActivity;
import com.flowstate.app.ui.LoginActivity;
import com.flowstate.data.remote.UserSettingsService;
import androidx.appcompat.app.AppCompatDelegate;
import android.content.SharedPreferences;

public class SettingsActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private SwitchMaterial switchGoogleFit, switchDarkMode, switchAdaptiveLearning, switchDailyAdvice;
    private AuthService authService;
    private View rootView;
    private UserSettingsService settingsService;
    private UserSettingsService.UserSettings currentSettings;
    private boolean isLoadingSettings = false;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        authService = new AuthService(this);
        
        // Check authentication
        if (!authService.isAuthenticated()) {
            // Not authenticated, go to login
            startActivity(new Intent(this, com.flowstate.app.ui.LoginActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_settings);
        rootView = findViewById(android.R.id.content);
        
        settingsService = new UserSettingsService(this);
        
        setupBottomNavigation();
        initializeViews();
        loadSettings();
    }
    
    private void initializeViews() {
        switchGoogleFit = findViewById(R.id.switchGoogleFit);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchAdaptiveLearning = findViewById(R.id.switchAdaptiveLearning);
        switchDailyAdvice = findViewById(R.id.switchDailyAdvice);
        
        // Set up toggle listeners
        setupToggleListeners();
        
        // Set up logout button
        View btnLogout = findViewById(R.id.btnLogout);
        if (btnLogout != null) {
            btnLogout.setOnClickListener(v -> {
                logoutUser();
            });
        }
        
        // Set up export/delete buttons
        View btnExportData = findViewById(R.id.btnExportData);
        if (btnExportData != null) {
            btnExportData.setOnClickListener(v -> {
                // TODO: Implement export functionality
                Snackbar.make(rootView, "Export functionality coming soon", Snackbar.LENGTH_SHORT).show();
            });
        }
        
        View btnDeleteData = findViewById(R.id.btnDeleteData);
        if (btnDeleteData != null) {
            btnDeleteData.setOnClickListener(v -> {
                // TODO: Implement delete functionality
                Snackbar.make(rootView, "Delete functionality coming soon", Snackbar.LENGTH_SHORT).show();
            });
        }
    }
    
    private void setupToggleListeners() {
        // Google Fit toggle
        if (switchGoogleFit != null) {
            switchGoogleFit.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isLoadingSettings && currentSettings != null) {
                    currentSettings.googleFitEnabled = isChecked;
                    saveSettings();
                }
            });
        }
        
        // Dark Mode toggle
        if (switchDarkMode != null) {
            switchDarkMode.setOnCheckedChangeListener(null); // Remove listener temporarily
            switchDarkMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isLoadingSettings) {
                    // Store in SharedPreferences for persistence
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("dark_mode", isChecked).apply();
                    
                    // Apply dark mode
                    applyDarkMode(isChecked);
                    
                    // Restart activity to apply theme changes
                    recreate();
                }
            });
        }
        
        // Adaptive Learning toggle
        if (switchAdaptiveLearning != null) {
            switchAdaptiveLearning.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isLoadingSettings && currentSettings != null) {
                    // Store adaptive learning preference
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("adaptive_learning", isChecked).apply();
                    Snackbar.make(rootView, 
                        isChecked ? "Adaptive learning enabled" : "Adaptive learning disabled", 
                        Snackbar.LENGTH_SHORT).show();
                }
            });
        }
        
        // Daily Advice toggle
        if (switchDailyAdvice != null) {
            switchDailyAdvice.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isLoadingSettings && currentSettings != null) {
                    currentSettings.notificationEnabled = isChecked;
                    saveSettings();
                    Snackbar.make(rootView, 
                        isChecked ? "Daily advice notifications enabled" : "Daily advice notifications disabled", 
                        Snackbar.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private void loadSettings() {
        isLoadingSettings = true;
        
        // Load dark mode from SharedPreferences first (for immediate UI update)
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        if (switchDarkMode != null) {
            switchDarkMode.setChecked(darkMode);
        }
        applyDarkMode(darkMode);
        
        // Load and display user email
        TextView tvEmail = findViewById(R.id.tvEmail);
        if (tvEmail != null) {
            String userId = authService.isAuthenticated() ? 
                com.flowstate.app.supabase.SupabaseClient.getInstance(this).getUserId() : null;
            if (userId != null) {
                // Try to get email from auth user
                com.flowstate.app.supabase.AuthService auth = new com.flowstate.app.supabase.AuthService(this);
                auth.getCurrentUser(new com.flowstate.app.supabase.AuthService.AuthCallback() {
                    @Override
                    public void onSuccess(com.flowstate.app.supabase.api.SupabaseAuthApi.UserResponse user) {
                        runOnUiThread(() -> {
                            if (user != null && user.email != null) {
                                tvEmail.setText(user.email);
                            }
                        });
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        // Keep default text
                    }
                });
            }
        }
        
        // Load adaptive learning preference
        boolean adaptiveLearning = prefs.getBoolean("adaptive_learning", false);
        if (switchAdaptiveLearning != null) {
            switchAdaptiveLearning.setChecked(adaptiveLearning);
        }
        
        // Load settings from Supabase
        settingsService.fetch(new UserSettingsService.SettingsCallback() {
            @Override
            public void onSuccess(UserSettingsService.UserSettings settings) {
                runOnUiThread(() -> {
                    currentSettings = settings;
                    
                    // Update toggles with loaded settings
                    if (switchGoogleFit != null) {
                        switchGoogleFit.setChecked(settings.googleFitEnabled);
                    }
                    if (switchDailyAdvice != null) {
                        switchDailyAdvice.setChecked(settings.notificationEnabled);
                    }
                    
                    isLoadingSettings = false;
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    // Use cached settings or defaults
                    currentSettings = settingsService.getCachedSettings();
                    if (currentSettings == null) {
                        currentSettings = new UserSettingsService.UserSettings();
                    }
                    
                    if (switchGoogleFit != null) {
                        switchGoogleFit.setChecked(currentSettings.googleFitEnabled);
                    }
                    if (switchDailyAdvice != null) {
                        switchDailyAdvice.setChecked(currentSettings.notificationEnabled);
                    }
                    
                    isLoadingSettings = false;
                    android.util.Log.e("SettingsActivity", "Failed to load settings", error);
                });
            }
        });
    }
    
    private void saveSettings() {
        if (currentSettings == null) {
            currentSettings = new UserSettingsService.UserSettings();
        }
        
        settingsService.save(currentSettings, new UserSettingsService.SettingsCallback() {
            @Override
            public void onSuccess(UserSettingsService.UserSettings settings) {
                runOnUiThread(() -> {
                    Snackbar.make(rootView, "Settings saved", Snackbar.LENGTH_SHORT).show();
                });
            }
            
            @Override
            public void onError(Exception error) {
                runOnUiThread(() -> {
                    Snackbar.make(rootView, "Failed to save settings", Snackbar.LENGTH_SHORT).show();
                    android.util.Log.e("SettingsActivity", "Failed to save settings", error);
                });
            }
        });
    }
    
    private void applyDarkMode(boolean enabled) {
        if (enabled) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
    
    private void logoutUser() {
        authService.signOut(new AuthService.AuthCallback() {
            @Override
            public void onSuccess(SupabaseAuthApi.UserResponse user) {
                // Logout successful, navigate to login
                Snackbar.make(rootView, "Logged out successfully", Snackbar.LENGTH_SHORT).show();
                Intent intent = new Intent(SettingsActivity.this, com.flowstate.app.ui.LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
            
            @Override
            public void onError(Throwable error) {
                // Even if API call fails, clear local auth and navigate to login
                Snackbar.make(rootView, "Logged out", Snackbar.LENGTH_SHORT).show();
                Intent intent = new Intent(SettingsActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startActivity(intent);
                finish();
            }
        });
    }
    
    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_settings) {
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(this, EnergyDashboardActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, AIScheduleActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_data) {
                startActivity(new Intent(this, DataLogsActivity.class));
                finish();
                return true;
            } else if (itemId == R.id.nav_insights) {
                startActivity(new Intent(this, WeeklyInsightsActivity.class));
                finish();
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_settings);
    }
}

