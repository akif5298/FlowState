package com.personaleenergy.app.ui.reaction;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.flowstate.app.R;
import com.personaleenergy.app.data.collection.ReactionTimeCollector;
import com.flowstate.app.data.models.ReactionTimeData;

public class ReactionTimeActivity extends AppCompatActivity {
    
    private TextView tvInstructions, tvResult;
    private Button btnStart, btnTap;
    private ReactionTimeCollector collector;
    private boolean waitingForColorChange = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reaction_time);
        
        initializeViews();
        collector = new ReactionTimeCollector();
        
        btnStart.setOnClickListener(v -> startTest());
        btnTap.setOnClickListener(v -> recordReaction());
    }
    
    private void initializeViews() {
        tvInstructions = findViewById(R.id.tvInstructions);
        tvResult = findViewById(R.id.tvResult);
        btnStart = findViewById(R.id.btnStart);
        btnTap = findViewById(R.id.btnTap);
        
        btnTap.setEnabled(false);
        updateButtonColorHex("#4CAF50"); // Green
        tvResult.setText("Ready to test");
    }
    
    private void startTest() {
        waitingForColorChange = true;
        btnStart.setEnabled(false);
        btnTap.setEnabled(true);
        tvInstructions.setText("⏳ Wait for the color to change...");
        updateButtonColorHex("#F44336"); // Red
        
        collector.waitForColorChange(() -> runOnUiThread(() -> {
            waitingForColorChange = false;
            tvInstructions.setText("TAP NOW!");
            updateButtonColorHex("#4CAF50"); // Green
        }), 5000);
    }
    
    private void recordReaction() {
        if (!collector.isWaitingForColorChange()) {
            ReactionTimeData result = collector.recordReaction();
            
            String resultText = String.format(
                    "%d ms",
                    result.getReactionTimeMs()
            );
            tvResult.setText(resultText);
            
            btnStart.setEnabled(true);
            btnTap.setEnabled(false);
            tvInstructions.setText("🎉 Test Complete!");
            updateButtonColorHex("#4CAF50");
        }
    }
    
    private void updateButtonColorHex(String colorHex) {
        btnTap.setBackgroundTintList(
            android.content.res.ColorStateList.valueOf(
                android.graphics.Color.parseColor(colorHex)
            )
        );
    }
}

