package com.personaleenergy.app.ui.calendar;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.flowstate.app.R;
import com.flowstate.app.calendar.GoogleCalendarAuthManager;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.common.api.ApiException;
import com.google.android.material.card.MaterialCardView;

/**
 * Onboarding activity for Google Calendar integration
 */
public class GoogleCalendarOnboardingActivity extends AppCompatActivity {
    
    private static final String TAG = "CalendarOnboarding";
    private static final int GOOGLE_CALENDAR_SIGN_IN = 3001;
    
    private GoogleCalendarAuthManager authManager;
    private TextView tvTitle, tvDescription, tvStatus;
    private Button btnConnectCalendar, btnSkip;
    private MaterialCardView cardStatus;
    private Handler mainHandler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_google_calendar_onboarding);
        
        mainHandler = new Handler(Looper.getMainLooper());
        authManager = new GoogleCalendarAuthManager(this);
        
        initializeViews();
        checkConnectionStatus();
    }
    
    private void initializeViews() {
        tvTitle = findViewById(R.id.tvTitle);
        tvDescription = findViewById(R.id.tvDescription);
        tvStatus = findViewById(R.id.tvStatus);
        btnConnectCalendar = findViewById(R.id.btnConnectCalendar);
        btnSkip = findViewById(R.id.btnSkip);
        cardStatus = findViewById(R.id.cardStatus);
        
        if (btnConnectCalendar != null) {
            btnConnectCalendar.setOnClickListener(v -> connectGoogleCalendar());
        }
        
        if (btnSkip != null) {
            btnSkip.setOnClickListener(v -> finish());
        }
    }
    
    private void checkConnectionStatus() {
        if (authManager.isSignedIn()) {
            String email = authManager.getConnectedAccountEmail();
            updateStatus("Connected to: " + (email != null ? email : "Google Calendar"), true);
            if (btnConnectCalendar != null) {
                btnConnectCalendar.setText("Disconnect");
                btnConnectCalendar.setOnClickListener(v -> disconnectCalendar());
            }
        } else {
            updateStatus("Connect your Google Calendar to schedule tasks around your existing events", false);
        }
    }
    
    private void connectGoogleCalendar() {
        updateStatus("Connecting to Google Calendar...", false);
        
        try {
            Intent signInIntent = authManager.getSignInClient().getSignInIntent();
            startActivityForResult(signInIntent, GOOGLE_CALENDAR_SIGN_IN);
        } catch (Exception e) {
            android.util.Log.e(TAG, "Error starting sign-in", e);
            updateStatus("Error starting connection. Please try again.", false);
            Toast.makeText(this, 
                "Unable to start Google Sign-In. Please ensure Google Play Services is up to date.", 
                Toast.LENGTH_LONG).show();
        }
    }
    
    private void disconnectCalendar() {
        authManager.disconnect();
        
        // Update preference
        android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        prefs.edit().putBoolean("google_calendar_enabled", false).apply();
        
        updateStatus("Disconnected from Google Calendar", false);
        if (btnConnectCalendar != null) {
            btnConnectCalendar.setText("Connect Google Calendar");
            btnConnectCalendar.setOnClickListener(v -> connectGoogleCalendar());
        }
        Toast.makeText(this, "Disconnected from Google Calendar", Toast.LENGTH_SHORT).show();
    }
    
    private void updateStatus(String message, boolean isConnected) {
        if (tvStatus != null) {
            tvStatus.setText(message);
        }
        
        if (cardStatus != null) {
            cardStatus.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == GOOGLE_CALENDAR_SIGN_IN) {
            try {
                GoogleSignInAccount account = GoogleSignIn.getSignedInAccountFromIntent(data)
                        .getResult(ApiException.class);
                
                    if (account != null) {
                        // Check if calendar scope is granted
                        boolean hasScope = account.getGrantedScopes() != null && 
                            (account.getGrantedScopes().contains("https://www.googleapis.com/auth/calendar.readonly") ||
                             account.getGrantedScopes().contains(com.google.api.services.calendar.CalendarScopes.CALENDAR_READONLY));
                        
                        if (hasScope) {
                            // Save connection
                            authManager.saveConnectionStatus(account);
                            
                            // Update preference
                            android.content.SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
                            prefs.edit().putBoolean("google_calendar_enabled", true).apply();
                            
                            // Success!
                            updateStatus("Successfully connected to Google Calendar!", true);
                            Toast.makeText(this, "Google Calendar connected successfully!", Toast.LENGTH_SHORT).show();
                            
                            if (btnConnectCalendar != null) {
                                btnConnectCalendar.setText("Disconnect");
                                btnConnectCalendar.setOnClickListener(v -> disconnectCalendar());
                            }
                            
                            // Return success result
                            setResult(RESULT_OK);
                            
                            // Auto-finish after a delay
                            mainHandler.postDelayed(() -> finish(), 2000);
                        } else {
                            // Scope not granted, need to request again
                            updateStatus("Calendar access not granted. Please try again.", false);
                            Toast.makeText(this, "Please grant calendar access", Toast.LENGTH_LONG).show();
                            
                            // Try to request permissions again
                            Intent signInIntent = authManager.getSignInClient().getSignInIntent();
                            startActivityForResult(signInIntent, GOOGLE_CALENDAR_SIGN_IN);
                        }
                    } else {
                        updateStatus("Connection cancelled", false);
                    }
            } catch (ApiException e) {
                android.util.Log.e(TAG, "Sign-in failed", e);
                String errorMessage = getErrorMessage(e.getStatusCode());
                updateStatus("Connection failed: " + errorMessage, false);
                
                // Show a more helpful message based on error code
                if (e.getStatusCode() == 10) {
                    // DEVELOPER_ERROR - OAuth not configured
                    Toast.makeText(this, 
                        "Google Calendar integration requires OAuth setup. Please contact support or check Google Cloud Console configuration.", 
                        Toast.LENGTH_LONG).show();
                } else {
                    Toast.makeText(this, "Failed to connect: " + errorMessage, Toast.LENGTH_LONG).show();
                }
            } catch (Exception e) {
                android.util.Log.e(TAG, "Unexpected error during sign-in", e);
                updateStatus("Connection failed: " + e.getMessage(), false);
                Toast.makeText(this, "An unexpected error occurred. Please try again.", Toast.LENGTH_SHORT).show();
            }
        }
    }
    
    /**
     * Get user-friendly error message from status code
     */
    private String getErrorMessage(int statusCode) {
        switch (statusCode) {
            case 10: // DEVELOPER_ERROR
                return "OAuth configuration required. The Google Calendar API needs to be set up in Google Cloud Console:\n\n" +
                       "1. Enable Google Calendar API in Google Cloud Console\n" +
                       "2. Create OAuth 2.0 credentials\n" +
                       "3. Add your app's SHA-1 fingerprint\n" +
                       "4. Configure OAuth consent screen";
            case 12500: // SIGN_IN_CANCELLED
                return "Sign-in was cancelled";
            case 7: // NETWORK_ERROR
                return "Network error. Please check your internet connection.";
            case 8: // INTERNAL_ERROR
                return "Internal error. Please try again later.";
            case 4: // SIGN_IN_REQUIRED
                return "Please sign in to your Google account";
            default:
                return "Error code: " + statusCode + ". Please try again or contact support.";
        }
    }
}

