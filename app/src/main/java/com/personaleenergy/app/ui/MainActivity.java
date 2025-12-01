package com.personaleenergy.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import com.personaleenergy.app.R;
import com.personaleenergy.app.health.HealthConnectManager;
import com.personaleenergy.app.ui.typing.TypingSpeedActivity;
import com.personaleenergy.app.ui.reaction.ReactionTimeActivity;
import com.personaleenergy.app.ui.EnergyDashboardActivity;
import com.personaleenergy.app.ui.OnboardingActivity;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;

public class MainActivity extends AppCompatActivity {

    private HealthConnectManager healthConnectManager;
    private Button btnTyping, btnReaction, btnEnergy, btnConnectHealth;

    private final ActivityResultLauncher<Set<String>> requestPermissions = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            result -> {
                if (result.containsValue(true)) {
                    readHealthConnectData();
                } else {
                    Toast.makeText(this, "Health Connect permissions were not granted.", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (!OnboardingActivity.isOnboardingComplete(this)) {
            startActivity(new Intent(MainActivity.this, OnboardingActivity.class));
            finish();
            return;
        }

        initializeMainActivity();
    }

    private void initializeMainActivity() {
        setContentView(R.layout.activity_main);

        initializeViews();
        healthConnectManager = new HealthConnectManager(this);

        animateViews();

        btnConnectHealth.setOnClickListener(v -> {
            v.performHapticFeedback(android.view.HapticFeedbackConstants.KEYBOARD_TAP);
            healthConnectManager.checkPermissionsAndRun(requestPermissions);
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

    private void readHealthConnectData() {
        Instant end = Instant.now();
        Instant start = end.minus(1, ChronoUnit.DAYS);
        Toast.makeText(this, "Reading Health Connect data...", Toast.LENGTH_SHORT).show();
        healthConnectManager.readData(start, end, new HealthConnectManager.HealthDataCallback() {
            @Override
            public void onDataLoaded(String data) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Data from Health Connect: " + data, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> {
                    Toast.makeText(MainActivity.this, "Error reading Health Connect data.", Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void animateViews() {
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
        btnConnectHealth = findViewById(R.id.btnConnectHealth);
        btnTyping = findViewById(R.id.btnTypingSpeed);
        btnReaction = findViewById(R.id.btnReactionTime);
        btnEnergy = findViewById(R.id.btnEnergyPrediction);
    }
}
