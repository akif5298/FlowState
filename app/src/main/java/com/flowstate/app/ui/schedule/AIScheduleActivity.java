package com.flowstate.app.ui.schedule;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.flowstate.app.R;
import com.flowstate.app.ui.EnergyDashboardActivity;
import com.flowstate.app.ui.data.DataLogsActivity;
import com.flowstate.app.ui.settings.SettingsActivity;
import com.flowstate.app.ui.insights.WeeklyInsightsActivity;

public class AIScheduleActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private EditText etTaskInput;
    private Button btnAddTask, btnRegenerate;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
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

