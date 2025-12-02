package com.flowstate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.flowstate.app.R;
import com.flowstate.app.supabase.AuthService;
import com.flowstate.app.supabase.api.SupabaseAuthApi;
import com.personaleenergy.app.ui.EnergyDashboardActivity;

public class LoginActivity extends AppCompatActivity {
    
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignUp;
    private View cardView;
    private AuthService authService;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
        authService = new AuthService(this);
        initializeViews();
        setupClickListeners();
        
        // Animate views
        animateViews();
    }
    
    private void initializeViews() {
        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        tvSignUp = findViewById(R.id.tvSignUp);
        cardView = findViewById(R.id.cardLogin);
        
        // Make password field show dots
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | 
                                 android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }
    
    private void setupClickListeners() {
        btnLogin.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim(); // Using email as username
            String password = etPassword.getText().toString().trim();
            
            if (email.isEmpty() || password.isEmpty()) {
                // Show error
                etUsername.setError(email.isEmpty() ? "Email required" : null);
                etPassword.setError(password.isEmpty() ? "Password required" : null);
            } else {
                loginUser(email, password);
            }
        });
        
        tvForgotPassword.setOnClickListener(v -> {
            String email = etUsername.getText().toString().trim();
            if (email.isEmpty()) {
                etUsername.setError("Enter your email to reset password");
                return;
            }
            resetPassword(email);
        });
        
        tvSignUp.setOnClickListener(v -> {
            // Navigate to sign up activity
            Intent intent = new Intent(this, SignUpActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        });
    }
    
    private void loginUser(String email, String password) {
        btnLogin.setEnabled(false);
        btnLogin.setText("Logging in...");
        
        authService.signIn(email, password, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(SupabaseAuthApi.UserResponse user) {
                Snackbar.make(cardView, "Login successful!", Snackbar.LENGTH_SHORT).show();
                // Navigate to dashboard
                Intent intent = new Intent(LoginActivity.this, EnergyDashboardActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
            
            @Override
            public void onError(Throwable error) {
                btnLogin.setEnabled(true);
                btnLogin.setText("Login");
                String errorMessage = error != null && error.getMessage() != null 
                    ? error.getMessage() 
                    : "Login failed. Please try again.";
                Snackbar.make(cardView, errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });
    }
    
    private void resetPassword(String email) {
        authService.resetPassword(email, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(SupabaseAuthApi.UserResponse user) {
                Snackbar.make(cardView, 
                    "Password reset email sent. Please check your inbox.", 
                    Snackbar.LENGTH_LONG).show();
            }
            
            @Override
            public void onError(Throwable error) {
                String errorMessage = error != null && error.getMessage() != null 
                    ? error.getMessage() 
                    : "Failed to send reset email. Please try again.";
                Snackbar.make(cardView, errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });
    }
    
    private void animateViews() {
        // Fade in animation with scale
        cardView.setAlpha(0f);
        cardView.setScaleX(0.9f);
        cardView.setScaleY(0.9f);
        
        cardView.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(new android.view.animation.DecelerateInterpolator())
                .start();
        
        // Animate app icon
        View appIcon = findViewById(R.id.appIcon);
        if (appIcon != null) {
            appIcon.setAlpha(0f);
            appIcon.setScaleX(0.5f);
            appIcon.setScaleY(0.5f);
            appIcon.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(800)
                    .setStartDelay(200)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
    }
}

