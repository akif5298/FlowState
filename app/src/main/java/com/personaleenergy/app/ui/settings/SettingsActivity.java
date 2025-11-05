package com.personaleenergy.app.ui.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.flowstate.app.R;
import com.personaleenergy.app.ui.EnergyDashboardActivity;
import com.personaleenergy.app.ui.data.DataLogsActivity;
import com.personaleenergy.app.ui.schedule.AIScheduleActivity;
import com.personaleenergy.app.ui.insights.WeeklyInsightsActivity;

public class SettingsActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private SwitchMaterial switchGoogleFit, switchDarkMode, switchAdaptiveLearning, switchDailyAdvice;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        
        setupBottomNavigation();
        initializeViews();
    }
    
    private void initializeViews() {
        switchGoogleFit = findViewById(R.id.switchGoogleFit);
        switchDarkMode = findViewById(R.id.switchDarkMode);
        switchAdaptiveLearning = findViewById(R.id.switchAdaptiveLearning);
        switchDailyAdvice = findViewById(R.id.switchDailyAdvice);
        
        // Set up logout button
        findViewById(R.id.btnLogout).setOnClickListener(v -> {
            // TODO: Implement logout logic
            finish();
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

