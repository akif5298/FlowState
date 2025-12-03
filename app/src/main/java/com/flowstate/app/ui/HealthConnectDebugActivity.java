package com.flowstate.app.ui;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.content.pm.PackageManager;
import android.app.AlertDialog;
import android.os.Build;

import androidx.activity.result.ActivityResultLauncher;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.PermissionController;
import androidx.health.connect.client.permission.HealthPermission;

import com.flowstate.app.R;
import com.flowstate.services.HealthConnectManager;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class HealthConnectDebugActivity extends AppCompatActivity {

    private static final String TAG = "HealthConnectDebug";
    private HealthConnectManager healthConnectManager;
    private TextView tvLog;
    private Button btnCheckPermissions;
    private Button btnReadHeartRate;
    private Button btnReadHrv;
    private Button btnReadSteps;
    private Button btnReadWorkouts;
    private Button btnReadBodyTemp;
    private Button btnRequestPermissions;
    
    private ActivityResultLauncher<Set<String>> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_health_connect_debug);

        healthConnectManager = new HealthConnectManager(this);
        
        // Initialize permission launcher
        if (healthConnectManager.isAvailable()) {
            try {
                requestPermissionLauncher = registerForActivityResult(
                        PermissionController.createRequestPermissionResultContract(),
                        granted -> {
                            log("Permission request result received. Granted: " + granted.size() + " permissions");
                            Set<String> required = healthConnectManager.getRequiredPermissions();
                            
                            if (granted.containsAll(required)) {
                                log("✓ All permissions granted!");
                                Toast.makeText(this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                            } else {
                                log("✗ Permissions denied or partially granted");
                                log("Granted: " + granted);
                                required.removeAll(granted);
                                log("Missing: " + required);
                                
                                // If permissions are denied, guide user to settings
                                showSettingsDialog();
                            }
                        });
                log("Permission launcher initialized successfully.");
            } catch (Exception e) {
                log("Error initializing permission launcher: " + e.getMessage());
                e.printStackTrace();
            }
        } else {
            log("Health Connect not available - permission launcher not initialized.");
        }
        
        tvLog = findViewById(R.id.tvLog);
        btnCheckPermissions = findViewById(R.id.btnCheckPermissions);
        btnRequestPermissions = findViewById(R.id.btnRequestPermissions);
        btnReadHeartRate = findViewById(R.id.btnReadHeartRate);
        btnReadHrv = findViewById(R.id.btnReadHrv);
        btnReadSteps = findViewById(R.id.btnReadSteps);
        btnReadWorkouts = findViewById(R.id.btnReadWorkouts);
        btnReadBodyTemp = findViewById(R.id.btnReadBodyTemp);

        btnCheckPermissions.setOnClickListener(v -> checkPermissions());
        btnRequestPermissions.setOnClickListener(v -> requestPermissionsDirectly());
        btnReadHeartRate.setOnClickListener(v -> readHeartRate());
        btnReadHrv.setOnClickListener(v -> readHrv());
        btnReadSteps.setOnClickListener(v -> readSteps());
        btnReadWorkouts.setOnClickListener(v -> readWorkouts());
        btnReadBodyTemp.setOnClickListener(v -> readBodyTemp());
        
        log("Health Connect Manager Initialized.");
        boolean isAvailable = healthConnectManager.isAvailable();
        log("Is Available: " + isAvailable);
        log("Package Name: " + getPackageName());

        // Automatically request permissions when the activity is created
        requestPermissionsDirectly();
        
        // Enable/disable request button based on availability
        if (btnRequestPermissions != null) {
            btnRequestPermissions.setEnabled(isAvailable && requestPermissionLauncher != null);
            if (!isAvailable) {
                log("⚠ Health Connect is not available. Please install/update Health Connect from Play Store.");
            } else if (requestPermissionLauncher == null) {
                log("⚠ Permission launcher not initialized. This may prevent permission requests.");
            }
        }
        
        // Log instructions
        log("");
        log("=== IMPORTANT INSTRUCTIONS ===");
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            log("⚠ Android 13 or below detected!");
            log("⚠ Health Connect may not recognize FlowState until:");
            log("   1. You request permissions (click button below)");
            log("   2. Force stop Health Connect");
            log("   3. Reopen Health Connect");
            log("   4. FlowState should then appear in Data & access");
        }
        log("");
        log("Steps:");
        log("1. Click 'Request Health Connect Permissions'");
        log("2. If dialog appears: Grant all permissions");
        log("3. If dialog DOESN'T appear:");
        log("   a. Force stop Health Connect (Settings > Apps > Health Connect > Force Stop)");
        log("   b. Reopen Health Connect");
        log("   c. Go to Data & access");
        log("   d. Look for FlowState");
        log("   e. If still not there, try requesting permissions again");
        log("4. After permissions are granted, try reading data");
    }

    private void requestPermissionsDirectly() {
        if (!healthConnectManager.isAvailable()) {
            log("Health Connect not available.");
            Toast.makeText(this, "Health Connect is not available on this device", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (requestPermissionLauncher == null) {
            log("Permission launcher not initialized. Health Connect may not be available.");
            Toast.makeText(this, "Cannot request permissions. Health Connect may not be properly installed.", Toast.LENGTH_LONG).show();
            return;
        }
        
        log("Requesting Health Connect permissions...");
        Set<String> required = healthConnectManager.getRequiredPermissions();
        log("Requesting " + required.size() + " permissions:");
        for (String perm : required) {
            log("  - " + perm);
        }
        
        // IMPORTANT: On Android 13 and below, Health Connect may not recognize the app
        // until it's restarted. Show a warning.
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            log("⚠ Android 13 or below detected.");
            log("⚠ If permission dialog doesn't appear, try:");
            log("   1. Force stop Health Connect app");
            log("   2. Reopen Health Connect");
            log("   3. Try requesting permissions again");
        }
        
        try {
            log("Launching permission request dialog...");
            // Use runOnUiThread to ensure we're on the main thread
            runOnUiThread(() -> {
                try {
                    // Use PermissionController.createRequestPermissionResultContract() which is the correct API
                    // This maps to HealthConnectClient.RequestPermission under the hood in newer libs
                    requestPermissionLauncher.launch(required);
                    log("✓ Permission request launched. Dialog should appear now.");
                    Toast.makeText(this, "Permission dialog should appear. If not, see instructions below.", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    log("✗ Error on UI thread: " + e.getMessage());
                    e.printStackTrace();
                    handlePermissionRequestFailure();
                }
            });
        } catch (Exception e) {
            log("✗ Error launching permission request: " + e.getMessage());
            e.printStackTrace();
            log("Stack trace: " + android.util.Log.getStackTraceString(e));
            handlePermissionRequestFailure();
        }
    }
    
    private void handlePermissionRequestFailure() {
        Toast.makeText(this, "Permission dialog failed. Trying alternative method...", Toast.LENGTH_LONG).show();
        // Show dialog with instructions
        new AlertDialog.Builder(this)
            .setTitle("Permission Request Failed")
            .setMessage("The permission dialog didn't appear. This often happens on Android 13 and below.\n\n" +
                       "Try this:\n" +
                       "1. Force stop Health Connect app (Settings > Apps > Health Connect > Force Stop)\n" +
                       "2. Reopen Health Connect\n" +
                       "3. Go to Data & access\n" +
                       "4. Look for FlowState in the list\n" +
                       "5. If it's not there, try requesting permissions again after restarting Health Connect")
            .setPositiveButton("Open Health Connect", (dialog, which) -> openHealthConnectSettings())
            .setNeutralButton("Try Again", (dialog, which) -> requestPermissionsDirectly())
            .setNegativeButton("Cancel", null)
            .show();
    }
    
    private void openHealthConnectPermissionSettings() {
        try {
            // Try to open Health Connect's data and access screen directly
            Intent intent = new Intent();
            intent.setAction("android.health.connect.action.MANAGE_PERMISSIONS");
            intent.setPackage("com.google.android.apps.healthdata");
            intent.putExtra("android.health.connect.extra.PACKAGE_NAME", getPackageName());
            
            if (intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                log("Opened Health Connect permission settings for FlowState.");
                Toast.makeText(this, "Opened Health Connect. Please grant permissions to FlowState.", Toast.LENGTH_LONG).show();
            } else {
                // Fallback: open Health Connect app
                openHealthConnectSettings();
            }
        } catch (Exception e) {
            log("Error opening Health Connect permission settings: " + e.getMessage());
            openHealthConnectSettings();
        }
    }
    
    private void openHealthConnectSettings() {
        try {
            // Try multiple methods to open Health Connect
            Intent intent = null;
            
            // Method 1: Try to open Health Connect's data and access screen directly
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                // Android 14+: Use the new intent
                intent = new Intent("android.health.connect.action.SHOW_PERMISSIONS_RATIONALE");
                intent.setPackage("com.google.android.apps.healthdata");
            }
            
            // Method 2: Try to open Health Connect app
            if (intent == null || intent.resolveActivity(getPackageManager()) == null) {
                intent = getPackageManager().getLaunchIntentForPackage("com.google.android.apps.healthdata");
            }
            
            if (intent != null && intent.resolveActivity(getPackageManager()) != null) {
                startActivity(intent);
                log("Opened Health Connect app.");
                log("Instructions:");
                log("1. Go to 'Data & access' or 'App permissions'");
                log("2. Look for 'FlowState' in the list");
                log("3. If it's not there, the app hasn't requested permissions yet");
                log("4. Go back and click 'Request Permissions' button again");
            } else {
                // Fallback: Open Health Connect in Play Store or system settings
                try {
                    // Try to open Health Connect settings via system settings
                    Intent settingsIntent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", "com.google.android.apps.healthdata", null);
                    settingsIntent.setData(uri);
                    startActivity(settingsIntent);
                    log("Opened Health Connect app settings. Please open the app from there.");
                } catch (Exception e2) {
                    log("Could not open Health Connect. Please open it manually.");
                    Toast.makeText(this, "Please open Health Connect app manually", Toast.LENGTH_LONG).show();
                }
            }
        } catch (Exception e) {
            log("Error opening Health Connect: " + e.getMessage());
            Toast.makeText(this, "Could not open Health Connect. Please open it manually.", Toast.LENGTH_LONG).show();
        }
    }

    private void showSettingsDialog() {
        new AlertDialog.Builder(this)
            .setTitle("Permissions Required")
            .setMessage("Health Connect permissions need to be granted. They are managed through the Health Connect app, not standard Android settings.\n\nClick 'Open Health Connect' to grant permissions.")
            .setPositiveButton("Open Health Connect", (dialog, which) -> {
                openHealthConnectSettings();
            })
            .setNeutralButton("App Settings", (dialog, which) -> {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                Uri uri = Uri.fromParts("package", getPackageName(), null);
                intent.setData(uri);
                startActivity(intent);
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void checkPermissions() {
        if (!healthConnectManager.isAvailable()) {
            log("Health Connect not available.");
            Toast.makeText(this, "Health Connect is not available on this device", Toast.LENGTH_SHORT).show();
            return;
        }

        Set<String> required = healthConnectManager.getRequiredPermissions();
        log("Required Permissions (" + required.size() + "):");
        for (String perm : required) {
            log("  - " + perm);
        }
        
        healthConnectManager.hasAllPermissions().thenAccept(granted -> {
            runOnUiThread(() -> {
                if (granted) {
                    log("✓ All Permissions Granted!");
                    Toast.makeText(HealthConnectDebugActivity.this, "All permissions granted!", Toast.LENGTH_SHORT).show();
                } else {
                    log("✗ Permissions NOT granted");
                    log("Click 'Request Health Connect Permissions' button to grant them.");
                    Toast.makeText(HealthConnectDebugActivity.this, "Permissions not granted. Use 'Request Permissions' button.", Toast.LENGTH_LONG).show();
                }
            });
        }).exceptionally(e -> {
            runOnUiThread(() -> {
                log("Error checking permissions: " + e.getMessage());
                e.printStackTrace();
            });
            return null;
        });
    }

    private void readHeartRate() {
        Instant end = Instant.now();
        Instant start = end.minus(24, ChronoUnit.HOURS);
        
        log("Reading Heart Rate from " + start + " to " + end);
        
        healthConnectManager.readHeartRate(start, end).thenAccept(records -> {
            runOnUiThread(() -> {
                log("Records found: " + records.size());
                for (int i = 0; i < Math.min(5, records.size()); i++) {
                    // Just logging the first few
                    log("Record: " + records.get(i).bpm + " bpm at " + records.get(i).timestamp);
                }
            });
        }).exceptionally(e -> {
            runOnUiThread(() -> log("Error reading HR: " + e.getMessage()));
            return null;
        });
    }

    private void readHrv() {
        Instant end = Instant.now();
        Instant start = end.minus(24, ChronoUnit.HOURS);
        
        log("Reading HRV from " + start + " to " + end);
        
        healthConnectManager.readHRV(start, end).thenAccept(records -> {
            runOnUiThread(() -> {
                log("HRV Records found: " + records.size());
                for (int i = 0; i < Math.min(5, records.size()); i++) {
                    log("Record: " + records.get(i).rmssd + " ms at " + records.get(i).timestamp);
                }
            });
        }).exceptionally(e -> {
            runOnUiThread(() -> log("Error reading HRV: " + e.getMessage()));
            return null;
        });
    }

    private void readSteps() {
        Instant end = Instant.now();
        Instant start = end.minus(24, ChronoUnit.HOURS);
        
        log("Reading Steps from " + start + " to " + end);
        
        healthConnectManager.readSteps(start, end).thenAccept(records -> {
            runOnUiThread(() -> {
                log("Step Records found: " + records.size());
                for (int i = 0; i < Math.min(5, records.size()); i++) {
                    log("Record: " + records.get(i).count + " steps from " + records.get(i).start_time);
                }
            });
        }).exceptionally(e -> {
            runOnUiThread(() -> log("Error reading Steps: " + e.getMessage()));
            return null;
        });
    }

    private void readWorkouts() {
        Instant end = Instant.now();
        Instant start = end.minus(24, ChronoUnit.HOURS);
        
        log("Reading Workouts from " + start + " to " + end);
        
        healthConnectManager.readWorkouts(start, end).thenAccept(records -> {
            runOnUiThread(() -> {
                log("Workout Records found: " + records.size());
                for (int i = 0; i < Math.min(5, records.size()); i++) {
                    log("Record: " + records.get(i).type + " (" + records.get(i).duration_minutes + " mins)");
                }
            });
        }).exceptionally(e -> {
            runOnUiThread(() -> log("Error reading Workouts: " + e.getMessage()));
            return null;
        });
    }

    private void readBodyTemp() {
        Instant end = Instant.now();
        Instant start = end.minus(24, ChronoUnit.HOURS);
        
        log("Reading Body Temp from " + start + " to " + end);
        
        healthConnectManager.readBodyTemperature(start, end).thenAccept(records -> {
            runOnUiThread(() -> {
                log("Body Temp Records found: " + records.size());
                for (int i = 0; i < Math.min(5, records.size()); i++) {
                    log("Record: " + records.get(i).temperature_celsius + " C at " + records.get(i).timestamp);
                }
            });
        }).exceptionally(e -> {
            runOnUiThread(() -> log("Error reading Body Temp: " + e.getMessage()));
            return null;
        });
    }

    private void log(String message) {
        Log.d(TAG, message);
        if (tvLog != null) {
            tvLog.append("\n" + message);
            // Scroll to bottom
            tvLog.post(() -> {
                if (tvLog.getLayout() != null) {
                    final int scrollAmount = tvLog.getLayout().getLineTop(tvLog.getLineCount()) - tvLog.getHeight();
                    if (scrollAmount > 0)
                        tvLog.scrollTo(0, scrollAmount);
                }
            });
        }
    }
}
