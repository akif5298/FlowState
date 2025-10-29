package com.personaleenergy.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.personaleenergy.app.R;
import com.personaleenergy.app.ui.EnergyDashboardActivity;

public class LoginActivity extends AppCompatActivity {
    
    private EditText etUsername, etPassword;
    private Button btnLogin;
    private TextView tvForgotPassword, tvSignUp;
    private View cardView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        
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
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            
            if (username.isEmpty() || password.isEmpty()) {
                // Show error
                etUsername.setError(username.isEmpty() ? "Username required" : null);
                etPassword.setError(password.isEmpty() ? "Password required" : null);
            } else {
                // Navigate directly to dashboard with navigation
                Intent intent = new Intent(this, EnergyDashboardActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
        });
        
        tvForgotPassword.setOnClickListener(v -> {
            // TODO: Implement forgot password flow
            findViewById(R.id.tvLoginStatus).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvLoginStatus)).setText("Forgot password - coming soon");
        });
        
        tvSignUp.setOnClickListener(v -> {
            // TODO: Implement sign up flow
            findViewById(R.id.tvLoginStatus).setVisibility(View.VISIBLE);
            ((TextView) findViewById(R.id.tvLoginStatus)).setText("Sign up - coming soon");
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

