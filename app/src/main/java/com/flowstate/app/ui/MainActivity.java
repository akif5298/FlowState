package com.flowstate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.flowstate.app.R;
import com.flowstate.app.data.collection.GoogleFitManager;
import com.flowstate.app.ui.typing.TypingSpeedActivity;
import com.flowstate.app.ui.reaction.ReactionTimeActivity;

public class MainActivity extends AppCompatActivity {
    
    private GoogleSignInClient googleSignInClient;
    private GoogleFitManager fitManager;
    private Button btnConnectFit, btnTyping, btnReaction, btnEnergy;
    private TextView tvStatus;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        
        initializeViews();
        setupGoogleSignIn();
        fitManager = new GoogleFitManager(this);
        
        updateConnectionStatus();
        animateViews();
        
        btnConnectFit.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            connectGoogleFit();
        });
        
        btnTyping.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, TypingSpeedActivity.class));
        });
        
        btnReaction.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, ReactionTimeActivity.class));
        });
        
        btnEnergy.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            startActivity(new Intent(this, EnergyDashboardActivity.class));
            finish();
        });
    }
    
    private void animateViews() {
        // Animate logo icons
        View logoIcons = findViewById(R.id.logoIcons);
        if (logoIcons != null) {
            logoIcons.setAlpha(0f);
            logoIcons.setTranslationY(-30f);
            logoIcons.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(800)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
        
        // Stagger animation for cards
        animateCardView(R.id.cardConnectFit, 100);
        animateCardView(R.id.cardTyping, 200);
        animateCardView(R.id.cardReaction, 300);
        animateCardView(R.id.cardEnergy, 400);
        animateCardView(R.id.cardInfo, 500);
    }
    
    private void animateCardView(int viewId, long delay) {
        View card = findViewById(viewId);
        if (card != null) {
            card.setAlpha(0f);
            card.setTranslationY(40f);
            card.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(600)
                    .setStartDelay(delay)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        }
    }
    
    private void initializeViews() {
        btnConnectFit = findViewById(R.id.btnConnectFit);
        btnTyping = findViewById(R.id.btnTypingSpeed);
        btnReaction = findViewById(R.id.btnReactionTime);
        btnEnergy = findViewById(R.id.btnEnergyPrediction);
        tvStatus = findViewById(R.id.tvStatus);
    }
    
    private void setupGoogleSignIn() {
        GoogleSignInOptions options = new GoogleSignInOptions.Builder(
                GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();
        
        googleSignInClient = GoogleSignIn.getClient(this, options);
    }
    
    private void connectGoogleFit() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
        if (account != null && fitManager.isSignedIn()) {
            tvStatus.setText("Already connected to Google Fit");
            return;
        }
        
        Intent signInIntent = googleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, 1001);
    }
    
    private void updateConnectionStatus() {
        if (fitManager.isSignedIn()) {
            tvStatus.setText("Connected to Google Fit");
            btnConnectFit.setText("🔗 Disconnect");
        } else {
            tvStatus.setText("Connect to sync biometric data");
            btnConnectFit.setText("🔗 Connect Google Fit");
        }
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == 1001) {
            GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
            if (account != null) {
                fitManager.requestFitnessPermission(account);
                updateConnectionStatus();
            }
        }
    }
}

