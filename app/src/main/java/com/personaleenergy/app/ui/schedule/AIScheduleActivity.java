package com.personaleenergy.app.ui.schedule;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.flowstate.app.R;
import com.personaleenergy.app.ui.EnergyDashboardActivity;
import com.personaleenergy.app.ui.data.DataLogsActivity;
import com.personaleenergy.app.ui.settings.SettingsActivity;
import com.personaleenergy.app.ui.insights.WeeklyInsightsActivity;

public class AIScheduleActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private EditText etTaskInput;
    private Button btnAddTask, btnRegenerate;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Check authentication
        com.flowstate.app.supabase.AuthService authService = new com.flowstate.app.supabase.AuthService(this);
        if (!authService.isAuthenticated()) {
            // Not authenticated, go to login
            startActivity(new Intent(this, com.flowstate.app.ui.LoginActivity.class));
            finish();
            return;
        }
        
        setContentView(R.layout.activity_ai_schedule);
        
        setupBottomNavigation();
        initializeViews();
    }
    
    private void initializeViews() {
        etTaskInput = findViewById(R.id.etTaskInput);
        btnAddTask = findViewById(R.id.btnAddTask);
        btnRegenerate = findViewById(R.id.btnRegenerate);
        
        btnAddTask.setOnClickListener(v -> {
            String task = etTaskInput.getText().toString().trim();
            if (!task.isEmpty()) {
                // TODO: Add task to schedule
                etTaskInput.setText("");
            }
        });
        
        btnRegenerate.setOnClickListener(v -> {
            // TODO: Regenerate schedule
        });
    }
    
    private void setupBottomNavigation() {
        bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_schedule) {
                return true;
            } else if (itemId == R.id.nav_dashboard) {
                startActivity(new Intent(this, EnergyDashboardActivity.class));
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
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                finish();
                return true;
            }
            return false;
        });
        bottomNav.setSelectedItemId(R.id.nav_schedule);
    }
}

