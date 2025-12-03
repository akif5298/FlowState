package com.flowstate.app;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.flowstate.core.Config;
import com.flowstate.data.remote.RemoteLegacyPredictRequest;
import com.flowstate.data.remote.RemoteModelClient;
import com.flowstate.data.remote.RemotePredictResponse;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EnergyPredictionActivity extends AppCompatActivity {
    private TextView resultView;
    private Button predictButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_prediction);

        resultView = findViewById(R.id.forecast_result);
        predictButton = findViewById(R.id.predict_button);

        predictButton.setOnClickListener(v -> {
            String baseUrl = Config.REMOTE_MODEL_URL;
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                Toast.makeText(this, "REMOTE_MODEL_URL not configured", Toast.LENGTH_LONG).show();
                return;
            }

            RemoteModelClient client = RemoteModelClient.getInstance(this);
            // Simple legacy history sample; replace with real data as needed
            RemoteLegacyPredictRequest req = new RemoteLegacyPredictRequest(
                    Arrays.asList(72.0, 76.0, 80.0, 74.0, 78.0, 82.0, 88.0, 75.0, 79.0, 84.0, 81.0, 77.0),
                    12
            );

            client.getApi().predictLegacy(req, client.getAuthorizationHeader()).enqueue(new Callback<RemotePredictResponse>() {
                @Override
                public void onResponse(Call<RemotePredictResponse> call, Response<RemotePredictResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        RemotePredictResponse body = response.body();
                        if (body.forecast != null) {
                            resultView.setText("Forecast: " + body.forecast.toString());
                        } else {
                            resultView.setText("No forecast field in response");
                        }
                    } else {
                        resultView.setText("Request failed: " + response.code());
                    }
                }

                @Override
                public void onFailure(Call<RemotePredictResponse> call, Throwable t) {
                    resultView.setText("Error: " + t.getMessage());
                }
            });
        });
    }
}
