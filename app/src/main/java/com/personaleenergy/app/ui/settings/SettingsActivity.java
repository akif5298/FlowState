package com.personaleenergy.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.personaleenergy.app.R;
import com.personaleenergy.app.health.HealthConnectManager;
import com.personaleenergy.app.ui.EnergyDashboardActivity;
import com.personaleenergy.app.ui.data.DataLogsActivity;
import com.personaleenergy.app.ui.schedule.AIScheduleActivity;
import com.personaleenergy.app.ui.insights.WeeklyInsightsActivity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;

public class SettingsActivity extends AppCompatActivity {

    private BottomNavigationView bottomNav;
    private SwitchMaterial switchHealthConnect, switchDarkMode, switchAdaptiveLearning, switchDailyAdvice;
    private TextView tvHealthData;
    private HealthConnectManager healthConnectManager;

    private final ActivityResultLauncher<String[]> requestPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (result.containsValue(true)) {
                    readHealthConnectData();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        healthConnectManager = new HealthConnectManager(this);

        setupBottomNavigation();
        initializeViews();
    }

    private void initializeViews() {
        switchHealthConnect = findViewById(R.id.switchHealthConnect);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchAdaptiveLearning = findViewById(R.id.switchAdaptiveLearning);
        switchDailyAdvice = findViewById(R.id.switchDailyAdvice);
        tvHealthData = findViewById(R.id.tvHealthData);

        switchHealthConnect.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                healthConnectManager.checkPermissionsAndRun(requestPermissions);
            }
        });

        // Set up logout button
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // TODO: Implement logout logic
            finish();
        });
    }

    private void readHealthConnectData() {
        Instant end = Instant.now();
        Instant start = end.minus(1, ChronoUnit.DAYS);
        healthConnectManager.readData(start, end, new HealthConnectManager.HealthDataCallback() {
            @Override
            public void onDataLoaded(String data) {
                runOnUiThread(() -> {
                    tvHealthData.setVisibility(View.VISIBLE);
                    tvHealthData.append(data + "\n");
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(SettingsActivity.this, "Error reading data from Health Connect", Toast.LENGTH_SHORT).show();
                });
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
