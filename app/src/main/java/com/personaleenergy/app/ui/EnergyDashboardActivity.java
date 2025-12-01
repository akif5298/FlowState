package com.personaleenergy.app.ui;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.personaleenergy.app.R;

public class EnergyDashboardActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_dashboard);

        TextView textView = findViewById(R.id.textView);
        textView.setText("Energy Dashboard is under construction.");
    }
}
