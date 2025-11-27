package com.flowstate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.snackbar.Snackbar;
import com.flowstate.app.R;
import com.flowstate.app.supabase.AuthService;
import com.flowstate.app.supabase.api.SupabaseAuthApi;
import com.flowstate.core.Config;
import com.flowstate.services.HibpPasswordChecker;
import com.personaleenergy.app.ui.EnergyDashboardActivity;

public class SignUpActivity extends AppCompatActivity {
    
    private EditText etFirstName, etLastName, etUsername, etEmail, etPassword, etConfirmPassword;
    private Button btnSignUp;
    private TextView tvLogin;
    private View rootView;
    private AuthService authService;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);
        
        authService = new AuthService(this);
        initializeViews();
        setupClickListeners();
    }
    
    private void initializeViews() {
        rootView = findViewById(android.R.id.content);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etUsername = findViewById(R.id.etUsername);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etConfirmPassword = findViewById(R.id.etConfirmPassword);
        btnSignUp = findViewById(R.id.btnSignUp);
        tvLogin = findViewById(R.id.tvLogin);
        
        // Make password fields show dots
        etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | 
                               android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        etConfirmPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | 
                                      android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
    }
    
    private void setupClickListeners() {
        btnSignUp.setOnClickListener(v -> {
            String firstName = etFirstName != null ? etFirstName.getText().toString().trim() : "";
            String lastName = etLastName != null ? etLastName.getText().toString().trim() : "";
            String username = etUsername.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString();
            String confirmPassword = etConfirmPassword.getText().toString();
            
            if (validateInput(firstName, lastName, username, email, password, confirmPassword)) {
                signUpUser(firstName, lastName, username, email, password);
            }
        });
        
        tvLogin.setOnClickListener(v -> {
            // Navigate to login
            Intent intent = new Intent(this, LoginActivity.class);
            startActivity(intent);
            overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
            finish();
        });
    }
    
    private boolean validateInput(String firstName, String lastName, String username, String email, String password, String confirmPassword) {
        boolean isValid = true;
        
        if (firstName == null || firstName.isEmpty()) {
            if (etFirstName != null) {
                etFirstName.setError("First name required");
            }
            isValid = false;
        }
        
        if (lastName == null || lastName.isEmpty()) {
            if (etLastName != null) {
                etLastName.setError("Last name required");
            }
            isValid = false;
        }
        
        if (username.isEmpty()) {
            etUsername.setError("Username required");
            isValid = false;
        }
        
        if (email.isEmpty() || !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Valid email required");
            isValid = false;
        }
        
        if (password.isEmpty() || password.length() < 6) {
            etPassword.setError("Password must be at least 6 characters");
            isValid = false;
        }
        
        if (!password.equals(confirmPassword)) {
            etConfirmPassword.setError("Passwords do not match");
            isValid = false;
        }
        
        return isValid;
    }
    
    private void signUpUser(String firstName, String lastName, String username, String email, String password) {
        btnSignUp.setEnabled(false);
        btnSignUp.setText("Checking password...");
        
        // First check password against HIBP
        HibpPasswordChecker passwordChecker = new HibpPasswordChecker(Config.HIBP_SERVICE_URL);
        passwordChecker.checkPassword(password, new HibpPasswordChecker.Callback() {
            @Override
            public void onResult(boolean pwned, int count) {
                if (pwned) {
                    // Password has been compromised
                    btnSignUp.setEnabled(true);
                    btnSignUp.setText("Sign Up");
                    String message = "This is a commonly compromised password, please use a different one";
                    etPassword.setError(message);
                    Snackbar.make(rootView, message, Snackbar.LENGTH_LONG).show();
                } else {
                    // Password is safe, proceed with signup
                    btnSignUp.setText("Signing up...");
                    performSignUp(firstName, lastName, username, email, password);
                }
            }
            
            @Override
            public void onError(Exception error) {
                // If HIBP service is unavailable, log but allow signup to proceed
                // (fail open - don't block users if service is down)
                android.util.Log.w("SignUpActivity", "HIBP password check failed, proceeding with signup", error);
                btnSignUp.setText("Signing up...");
                performSignUp(firstName, lastName, username, email, password);
            }
        });
    }
    
    private void performSignUp(String firstName, String lastName, String username, String email, String password) {
        authService.signUp(email, password, username, firstName, lastName, new AuthService.AuthCallback() {
            @Override
            public void onSuccess(SupabaseAuthApi.UserResponse user) {
                Snackbar.make(rootView, "Sign up successful!", Snackbar.LENGTH_SHORT).show();
                // Navigate to dashboard
                Intent intent = new Intent(SignUpActivity.this, EnergyDashboardActivity.class);
                startActivity(intent);
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                finish();
            }
            
            @Override
            public void onError(Throwable error) {
                btnSignUp.setEnabled(true);
                btnSignUp.setText("Sign Up");
                String errorMessage = error != null && error.getMessage() != null 
                    ? error.getMessage() 
                    : "Sign up failed. Please try again.";
                Snackbar.make(rootView, errorMessage, Snackbar.LENGTH_LONG).show();
            }
        });
    }
}

