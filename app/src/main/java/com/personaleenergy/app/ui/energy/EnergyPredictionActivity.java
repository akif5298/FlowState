package com.personaleenergy.app.ui.energy;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.personaleenergy.app.R;

public class EnergyPredictionActivity extends AppCompatActivity {
    
    private TextView tvPredictions, tvSuggestions, tvAdvice;
    private Button btnLoadData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_prediction);
        
        initializeViews();
        
        tvPredictions.setText("Energy prediction is not yet implemented for local data.");
        btnLoadData.setEnabled(false);
    }
    
    private void initializeViews() {
        tvPredictions = findViewById(R.id.tvPredictions);
        tvSuggestions = findViewById(R.id.tvSuggestions);
        tvAdvice = findViewById(R.id.tvAdvice);
        btnLoadData = findViewById(R.id.btnLoadData);
    }
}
