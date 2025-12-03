package com.personaleenergy.app.ui.calendar;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import com.flowstate.app.R;
import com.flowstate.app.calendar.SimpleCalendarService;
import com.google.android.material.card.MaterialCardView;

/**
 * Simple onboarding for calendar integration using Android Calendar API
 * No OAuth required - just asks for calendar permission
 */
public class SimpleCalendarOnboardingActivity extends AppCompatActivity {
    
    private static final String TAG = "SimpleCalendarOnboarding";
    private static final int CALENDAR_PERMISSION_REQUEST = 5001;
    
    private SimpleCalendarService calendarService;
    private TextView tvTitle, tvDescription, tvStatus;
    private Button btnConnectCalendar, btnSkip;
    private MaterialCardView cardStatus;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_calendar_onboarding);
        
        calendarService = new SimpleCalendarService(this);
        
        initializeViews();
        checkPermissionStatus();
    }
    
    private void initializeViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvStatus = findViewById(R.id.tvStatus);
        btnConnectCalendar = findViewById(R.id.btnConnectCalendar);
        btnSkip = findViewById(R.id.btnSkip);
        cardStatus = findViewById(R.id.cardStatus);
        
        if (tvDescription != null) {
            tvDescription.setText("Grant calendar access to help FlowState build your AI schedule around your existing events. We'll only read your calendar to optimize your task scheduling.");
        }
        
        if (btnConnectCalendar != null) {
            btnConnectCalendar.setOnClickListener(v -> requestCalendarPermission());
        }
        
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> finish());
        }
    }
    
    private void checkPermissionStatus() {
        if (calendarService.hasPermissions()) {
            updateStatus("Calendar access granted! Your schedule will be optimized around your events.", true);
            if (btnConnectCalendar != null) {
                btnConnectCalendar.setText("Connected");
                btnConnectCalendar.setEnabled(false);
            }
            
            // Save preference
            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
            prefs.edit().putBoolean("google_calendar_enabled", true).apply();
            
            // Return success
            setResult(RESULT_OK);
        } else {
            updateStatus("Grant calendar access to optimize your schedule", false);
        }
    }
    
    private void requestCalendarPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_CALENDAR) 
                != PackageManager.PERMISSION_GRANTED) {
            updateStatus("Requesting calendar permission...", false);
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.READ_CALENDAR},
                    CALENDAR_PERMISSION_REQUEST);
        } else {
            // Already granted
            onPermissionGranted();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == CALENDAR_PERMISSION_REQUEST) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                onPermissionGranted();
            } else {
                onPermissionDenied();
            }
        }
    }
    
    private void onPermissionGranted() {
        updateStatus("Calendar access granted! Your schedule will be optimized around your events.", true);
        Toast.makeText(this, "Calendar access granted!", Toast.LENGTH_SHORT).show();
        
        if (btnConnectCalendar != null) {
            btnConnectCalendar.setText("Connected");
            btnConnectCalendar.setEnabled(false);
        }
        
        // Save preference
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("google_calendar_enabled", true).apply();
        
        // Return success
        setResult(RESULT_OK);
        
        // Auto-finish after a delay
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> finish(), 2000);
    }
    
    private void onPermissionDenied() {
        updateStatus("Calendar permission denied. You can enable it later in app settings.", false);
        Toast.makeText(this, "Calendar permission is required for schedule optimization", Toast.LENGTH_LONG).show();
    }
    
    private void updateStatus(String message, boolean isConnected) {
        if (tvStatus != null) {
            tvStatus.setText(message);
        }
        
        if (cardStatus != null) {
            cardStatus.setVisibility(View.VISIBLE);
        }
    }
}

