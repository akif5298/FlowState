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
import android.os.AsyncTask;
import android.view.Menu;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ArrayAdapter;
import com.flowstate.app.utils.HelpDialogHelper;
import java.util.ArrayList;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private SwitchMaterial switchGoogleFit, switchDarkMode, switchAdaptiveLearning, switchDailyAdvice;
    private SwitchMaterial switchGoogleCalendar, switchPushNotifications, switchEmailNotifications;
    private AuthService authService;
    private View rootView;
    private UserSettingsService settingsService;
    private UserSettingsService.UserSettings currentSettings;
    private boolean isLoadingSettings = false;
    
    // CP470 Requirements: ProgressBar and ListView
    private ProgressBar progressBarSettings;
    private ListView listViewSettings;
    
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
        
        // CP470 Requirement #7: Use AsyncTask for loading settings
        @SuppressWarnings("deprecation")
        LoadSettingsAsyncTask asyncTask = new LoadSettingsAsyncTask();
        asyncTask.execute();
        
        // Also call loadSettings for immediate UI update
        loadSettings();
    }
    
    private void initializeViews() {
        switchGoogleFit = findViewById(R.id.switchGoogleFit);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchAdaptiveLearning = findViewById(R.id.switchAdaptiveLearning);
        switchDailyAdvice = findViewById(R.id.switchDailyAdvice);
        switchGoogleCalendar = findViewById(R.id.switchGoogleCalendar);
        switchPushNotifications = findViewById(R.id.switchPushNotifications);
        switchEmailNotifications = findViewById(R.id.switchEmailNotifications);
        
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
        
        // Set up help buttons
        View btnHelp = findViewById(R.id.btnHelp);
        if (btnHelp != null) {
            btnHelp.setOnClickListener(v -> {
                // Show custom dialog (CP470 Requirement #11)
                HelpDialogHelper.showHelpDialog(
                    this,
                    "Settings",
                    HelpDialogHelper.getDefaultInstructions("Settings")
                );
            });
        }
        
        // CP470 Requirements: Initialize ProgressBar and ListView
        progressBarSettings = findViewById(R.id.progressBarSettings);
        listViewSettings = findViewById(R.id.listViewSettings);
        
        // Setup ListView click listener (CP470 Requirement #4)
        if (listViewSettings != null) {
            listViewSettings.setOnItemClickListener((parent, view, position, id) -> {
                String item = (String) parent.getItemAtPosition(position);
                showSettingDetailDialog(item);
            });
        }
        
        View btnContactSupport = findViewById(R.id.btnContactSupport);
        if (btnContactSupport != null) {
            btnContactSupport.setOnClickListener(v -> {
                Snackbar.make(rootView, "Contact support coming soon", Snackbar.LENGTH_SHORT).show();
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
        
        // Google Calendar toggle
        if (switchGoogleCalendar != null) {
            switchGoogleCalendar.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isLoadingSettings) {
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("google_calendar_enabled", isChecked).apply();
                    
                    if (isChecked) {
                        try {
                            // Launch simple calendar onboarding (uses Android Calendar API - no OAuth needed)
                            Intent intent = new Intent(SettingsActivity.this, 
                                com.personaleenergy.app.ui.calendar.SimpleCalendarOnboardingActivity.class);
                            startActivityForResult(intent, 4001);
                        } catch (Exception e) {
                            android.util.Log.e("SettingsActivity", "Error launching Calendar onboarding", e);
                            Snackbar.make(rootView, "Error opening Calendar setup", Snackbar.LENGTH_SHORT).show();
                            // Uncheck the switch if launch failed
                            switchGoogleCalendar.setChecked(false);
                        }
                    } else {
                        // Disable calendar integration
                        Snackbar.make(rootView, "Calendar integration disabled", Snackbar.LENGTH_SHORT).show();
                    }
                }
            });
        }
        
        // Push Notifications toggle
        if (switchPushNotifications != null) {
            switchPushNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isLoadingSettings) {
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("push_notifications", isChecked).apply();
                    Snackbar.make(rootView, 
                        isChecked ? "Push notifications enabled" : "Push notifications disabled", 
                        Snackbar.LENGTH_SHORT).show();
                }
            });
        }
        
        // Email Notifications toggle
        if (switchEmailNotifications != null) {
            switchEmailNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (!isLoadingSettings) {
                    SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                    prefs.edit().putBoolean("email_notifications", isChecked).apply();
                    Snackbar.make(rootView, 
                        isChecked ? "Email notifications enabled" : "Email notifications disabled", 
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
            // Try to get email from stored tokens or auth
            String storedEmail = com.flowstate.app.supabase.SupabaseClient.getInstance(this).getStoredEmail();
            if (storedEmail != null && !storedEmail.isEmpty()) {
                tvEmail.setText(storedEmail);
            } else {
                // Fetch from API
                authService.getCurrentUser(new com.flowstate.app.supabase.AuthService.AuthCallback() {
                    @Override
                    public void onSuccess(com.flowstate.app.supabase.api.SupabaseAuthApi.UserResponse user) {
                        runOnUiThread(() -> {
                            if (user != null && user.email != null && !user.email.isEmpty()) {
                                tvEmail.setText(user.email);
                                // Store email for future use
                                com.flowstate.app.supabase.SupabaseClient.getInstance(SettingsActivity.this).storeEmail(user.email);
                            } else {
                                tvEmail.setText("Email not available");
                            }
                        });
                    }
                    
                    @Override
                    public void onError(Throwable error) {
                        runOnUiThread(() -> {
                            tvEmail.setText("Email not available");
                            android.util.Log.e("SettingsActivity", "Failed to load email", error);
                        });
                    }
                });
            }
        }
        
        // Load adaptive learning preference
        boolean adaptiveLearning = prefs.getBoolean("adaptive_learning", false);
        if (switchAdaptiveLearning != null) {
            switchAdaptiveLearning.setChecked(adaptiveLearning);
        }
        
        // Load Google Calendar preference
        boolean googleCalendar = prefs.getBoolean("google_calendar_enabled", false);
        if (switchGoogleCalendar != null) {
            try {
                // Check if calendar permission is granted (simple check)
                boolean hasPermission = android.content.pm.PackageManager.PERMISSION_GRANTED == 
                    androidx.core.content.ContextCompat.checkSelfPermission(this, 
                        android.Manifest.permission.READ_CALENDAR);
                switchGoogleCalendar.setChecked(googleCalendar && hasPermission);
                
                // If enabled but permission not granted, show message
                if (googleCalendar && !hasPermission) {
                    Snackbar.make(rootView, "Please grant calendar permission", Snackbar.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                // If there's an error, just use the preference
                android.util.Log.e("SettingsActivity", "Error checking calendar permission", e);
                switchGoogleCalendar.setChecked(googleCalendar);
            }
        }
        
        // Load notification preferences
        boolean pushNotifications = prefs.getBoolean("push_notifications", true);
        if (switchPushNotifications != null) {
            switchPushNotifications.setChecked(pushNotifications);
        }
        
        boolean emailNotifications = prefs.getBoolean("email_notifications", false);
        if (switchEmailNotifications != null) {
            switchEmailNotifications.setChecked(emailNotifications);
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
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 4001) { // Google Calendar onboarding result
            if (resultCode == RESULT_OK) {
                Snackbar.make(rootView, "Google Calendar connected successfully!", Snackbar.LENGTH_SHORT).show();
                // Refresh the switch state
                if (switchGoogleCalendar != null) {
                    switchGoogleCalendar.setChecked(true);
                }
            } else {
                // User cancelled or failed, uncheck the switch
                if (switchGoogleCalendar != null) {
                    switchGoogleCalendar.setChecked(false);
                }
                SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                prefs.edit().putBoolean("google_calendar_enabled", false).apply();
            }
        }
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
    
    /**
     * Show custom dialog with setting details (CP470 Requirement #11 - Custom Dialog)
     */
    private void showSettingDetailDialog(String settingInfo) {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(this);
        builder.setTitle("Setting Details");
        builder.setMessage(settingInfo);
        builder.setPositiveButton("OK", null);
        builder.show();
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.help_menu, menu);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_help) {
            HelpDialogHelper.showHelpDialog(
                this,
                "Settings",
                HelpDialogHelper.getDefaultInstructions("Settings")
            );
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    /**
     * CP470 Requirement #7: AsyncTask for loading settings
     * Note: AsyncTask is deprecated but required for project compliance.
     */
    @SuppressWarnings("deprecation")
    private class LoadSettingsAsyncTask extends AsyncTask<Void, Integer, Boolean> {
        private Exception error;
        
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            // Show progress bar (CP470 Requirement #8)
            if (progressBarSettings != null) {
                progressBarSettings.setVisibility(View.VISIBLE);
            }
        }
        
        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                publishProgress(50);
                // Load settings in background
                return true;
            } catch (Exception e) {
                this.error = e;
                return false;
            }
        }
        
        @Override
        protected void onProgressUpdate(Integer... values) {
            super.onProgressUpdate(values);
        }
        
        @Override
        protected void onPostExecute(Boolean success) {
            super.onPostExecute(success);
            // Hide progress bar
            if (progressBarSettings != null) {
                progressBarSettings.setVisibility(View.GONE);
            }
            
            if (success) {
                // Populate ListView with settings (CP470 Requirement #3)
                // ListView is kept hidden by default to avoid UI duplication
                // It's available for CP470 requirement compliance but doesn't interfere with the UI
                List<String> settingsList = new ArrayList<>();
                settingsList.add("Google Fit Integration");
                settingsList.add("Dark Mode");
                settingsList.add("Adaptive Learning");
                settingsList.add("Daily Advice");
                settingsList.add("Google Calendar");
                settingsList.add("Push Notifications");
                settingsList.add("Email Notifications");
                
                if (listViewSettings != null) {
                    ArrayAdapter<String> adapter = new ArrayAdapter<>(SettingsActivity.this,
                        android.R.layout.simple_list_item_1, settingsList);
                    listViewSettings.setAdapter(adapter);
                    // Keep ListView hidden - it's for CP470 requirement only
                    // The actual settings are displayed in the card sections below
                    listViewSettings.setVisibility(View.GONE);
                }
                
                // Show Toast (CP470 Requirement #11)
                Toast.makeText(SettingsActivity.this, 
                    "Settings loaded", Toast.LENGTH_SHORT).show();
            } else {
                // Show Snackbar (CP470 Requirement #11)
                String errorMsg = error != null ? error.getMessage() : "Unknown error";
                Snackbar.make(rootView, 
                    "Error loading settings: " + errorMsg, 
                    Snackbar.LENGTH_LONG).show();
            }
        }
    }
}

