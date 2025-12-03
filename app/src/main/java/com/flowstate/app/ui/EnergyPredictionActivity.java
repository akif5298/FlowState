package com.flowstate.app.ui;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.flowstate.core.Config;
import com.flowstate.data.ai.GeminiClient;
import com.flowstate.data.remote.RemoteLegacyPredictRequest;
import com.flowstate.data.remote.RemoteModelClient;
import com.flowstate.data.remote.RemotePredictResponse;
import com.flowstate.data.remote.RemoteMultivariatePredictRequest;
import com.flowstate.app.R;

import java.util.Arrays;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class EnergyPredictionActivity extends AppCompatActivity {
    private TextView resultView;
    private Button predictButton;
    private Button predictMultiButton;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_energy_prediction);

        resultView = findViewById(R.id.forecast_result);
        predictButton = findViewById(R.id.predict_button);
        predictMultiButton = findViewById(R.id.predict_multi_button);

        predictButton.setOnClickListener(v -> {
            String baseUrl = Config.REMOTE_MODEL_URL;
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                Toast.makeText(this, "REMOTE_MODEL_URL not configured", Toast.LENGTH_LONG).show();
                return;
            }

            RemoteModelClient client = RemoteModelClient.getInstance(this);
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
                            resultView.setText("Generating AI insight...");
                            
                            // Get AI-generated insight from Gemini
                            GeminiClient gemini = GeminiClient.getInstance(EnergyPredictionActivity.this);
                            gemini.generateInsight(body.forecast, new GeminiClient.InsightCallback() {
                                @Override
                                public void onSuccess(String insight) {
                                    runOnUiThread(() -> {
                                        resultView.setText(insight);
                                    });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    runOnUiThread(() -> {
                                        resultView.setText("AI insight unavailable: " + e.getMessage());
                                    });
                                }
                            });
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

        predictMultiButton.setOnClickListener(v -> {
            String baseUrl = Config.REMOTE_MODEL_URL;
            if (baseUrl == null || baseUrl.trim().isEmpty()) {
                Toast.makeText(this, "REMOTE_MODEL_URL not configured", Toast.LENGTH_LONG).show();
                return;
            }

            RemoteModelClient client = RemoteModelClient.getInstance(this);
            
            java.util.Map<String, java.util.List<Double>> pastValues = new java.util.HashMap<>();
            pastValues.put("heart_rate", Arrays.asList(72.0, 76.0, 80.0, 74.0, 78.0, 82.0, 88.0, 75.0, 79.0, 84.0, 81.0, 77.0));
            pastValues.put("hrv", Arrays.asList(55.0, 53.0, 52.0, 60.0, 58.0, 56.0, 54.0, 62.0, 59.0, 57.0, 55.0, 60.0));
            
            RemoteMultivariatePredictRequest multiReq = new RemoteMultivariatePredictRequest(
                    pastValues,
                    null,
                    12
            );

            client.getApi().predictMultivariate(multiReq, client.getAuthorizationHeader()).enqueue(new Callback<RemotePredictResponse>() {
                @Override
                public void onResponse(Call<RemotePredictResponse> call, Response<RemotePredictResponse> response) {
                    if (response.isSuccessful() && response.body() != null) {
                        RemotePredictResponse body = response.body();
                        if (body.forecast != null) {
                            resultView.setText("Generating AI insight...");
                            
                            // Get AI-generated insight from Gemini
                            GeminiClient gemini = GeminiClient.getInstance(EnergyPredictionActivity.this);
                            gemini.generateInsight(body.forecast, new GeminiClient.InsightCallback() {
                                @Override
                                public void onSuccess(String insight) {
                                    runOnUiThread(() -> {
                                        resultView.setText(insight);
                                    });
                                }

                                @Override
                                public void onFailure(Exception e) {
                                    runOnUiThread(() -> {
                                        resultView.setText("AI insight unavailable: " + e.getMessage());
                                    });
                                }
                            });
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
