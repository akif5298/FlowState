package com.personaleenergy.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;
import com.personaleenergy.app.R;
import com.personaleenergy.app.ui.settings.SettingsActivity;
import com.personaleenergy.app.ui.data.DataLogsActivity;
import com.personaleenergy.app.ui.schedule.AIScheduleActivity;
import com.personaleenergy.app.ui.insights.WeeklyInsightsActivity;
import com.personaleenergy.app.ui.typing.TypingSpeedActivity;
import com.personaleenergy.app.ui.reaction.ReactionTimeActivity;

public class EnergyDashboardActivity extends AppCompatActivity {
    
    private BottomNavigationView bottomNav;
    private TextView tvCurrentEnergy, tvEnergyLevel, tvAIInsight;
    private MaterialCardView cardGraph, cardCognitive;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_dashboard);
        
        initializeViews();
        setupBottomNavigation();
        setupClickListeners();
        
        // Load initial data
        loadEnergyData();
    }
    
    private void initializeViews() {
        bottomNav = findViewById(R.id.bottomNavigation);
        tvCurrentEnergy = findViewById(R.id.tvCurrentEnergy);
        tvEnergyLevel = findViewById(R.id.tvEnergyLevel);
        tvAIInsight = findViewById(R.id.tvAIInsight);
        cardGraph = findViewById(R.id.cardGraph);
        cardCognitive = findViewById(R.id.cardCognitive);
    }
    
    private void setupBottomNavigation() {
        bottomNav.setOnNavigationItemSelectedListener(item -> {
            int itemId = item.getItemId();
            
            if (itemId == R.id.nav_dashboard) {
                // Already here, no action needed
                return true;
            } else if (itemId == R.id.nav_schedule) {
                startActivity(new Intent(this, AIScheduleActivity.class));
                return true;
            } else if (itemId == R.id.nav_data) {
                startActivity(new Intent(this, DataLogsActivity.class));
                return true;
            } else if (itemId == R.id.nav_insights) {
                startActivity(new Intent(this, WeeklyInsightsActivity.class));
                return true;
            } else if (itemId == R.id.nav_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }
            return false;
        });
        
        // Set dashboard as selected
        bottomNav.setSelectedItemId(R.id.nav_dashboard);
    }
    
    private void setupClickListeners() {
        // Typing test button
        findViewById(R.id.btnTypingTest).setOnClickListener(v -> {
            startActivity(new Intent(this, TypingSpeedActivity.class));
        });
        
        // Reaction test button
        findViewById(R.id.btnReactionTest).setOnClickListener(v -> {
            startActivity(new Intent(this, ReactionTimeActivity.class));
        });
        
        // Manual feedback slider - will be handled in layout
        // Graph click to show details
        cardGraph.setOnClickListener(v -> {
            // Show detailed graph view
            // TODO: Implement detailed graph modal/fragment
        });
    }
    
    private void loadEnergyData() {
        // TODO: Load actual data from ML model
        tvCurrentEnergy.setText("75");
        tvEnergyLevel.setText("HIGH");
        tvAIInsight.setText("Energy dip expected in 25 min. Hydrate or take a short walk.");
    }
}

