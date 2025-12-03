package com.flowstate.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import com.flowstate.app.R;
import com.flowstate.app.ui.typing.TypingSpeedActivity;
import com.flowstate.app.ui.reaction.ReactionTimeActivity;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button openPredict = findViewById(R.id.open_predict_button);
        if (openPredict != null) {
            openPredict.setOnClickListener(v -> {
                Intent intent = new Intent(this, EnergyPredictionActivity.class);
                startActivity(intent);
            });
        }
        
        // Typing Speed Test button
        Button btnTypingSpeed = findViewById(R.id.btnTypingSpeed);
        if (btnTypingSpeed != null) {
            btnTypingSpeed.setOnClickListener(v -> {
                Intent intent = new Intent(this, TypingSpeedActivity.class);
                startActivity(intent);
            });
        }
        
        // Reaction Time Test button
        Button btnReactionTime = findViewById(R.id.btnReactionTime);
        if (btnReactionTime != null) {
            btnReactionTime.setOnClickListener(v -> {
                Intent intent = new Intent(this, ReactionTimeActivity.class);
                startActivity(intent);
            });
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_energy_prediction) {
            Intent intent = new Intent(this, EnergyPredictionActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
