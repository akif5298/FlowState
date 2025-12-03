package com.flowstate.services;

import android.content.Context;
import android.util.Log;
import com.flowstate.app.data.models.HealthDataSummary;
import com.flowstate.app.data.models.EnergyPredictionResult;
import com.flowstate.core.Config;
import com.google.ai.client.generativeai.GenerativeModel;
import com.google.ai.client.generativeai.java.GenerativeModelFutures;
import com.google.ai.client.generativeai.type.Content;
import com.google.ai.client.generativeai.type.GenerateContentResponse;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

/**
 * Uses Google Gemini API to predict energy levels
 * based on health data from Health Connect
 */
public class GeminiEnergyPredictor {
    
    private static final String TAG = "GeminiEnergyPredictor";
    
    private GenerativeModel model;
    private HealthDataAggregator dataAggregator;
    private Gson gson;
    private boolean isAvailable;
    
    public GeminiEnergyPredictor(Context context) {
        // Try to get API key from settings first, then fall back to Config
        android.content.SharedPreferences prefs = context.getSharedPreferences("flowstate_settings", Context.MODE_PRIVATE);
        String apiKey = prefs.getString("gemini_api_key", null);
        
        if (apiKey == null || apiKey.isEmpty()) {
            // Fall back to Config (from local.properties)
            apiKey = Config.GEMINI_API_KEY;
        }
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.equals("null")) {
            Log.w(TAG, "Gemini API key not configured. Please set it in Settings.");
            this.isAvailable = false;
            return;
        }
        
        try {
            this.dataAggregator = new HealthDataAggregator(context);
            this.gson = new GsonBuilder().create();
            
            // Initialize Gemini model
            // Using constructor directly as Builder might not be available in Java for this version
            // Ensure Config is passed correctly, handling potential nulls
            com.google.ai.client.generativeai.type.RequestOptions requestOptions = new com.google.ai.client.generativeai.type.RequestOptions();
            
            this.model = new GenerativeModel(
                "gemini-2.5-flash", // Using the latest Gemini 2.5 Flash model
                apiKey,
                null, // generationConfig
                null, // safetySettings
                requestOptions, // requestOptions - MUST NOT BE NULL
                null, // tools
                null, // toolsConfig
                null  // systemInstruction
            );
            
            this.isAvailable = true;
            Log.d(TAG, "Gemini Energy Predictor initialized successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "Error initializing Gemini predictor", e);
            this.isAvailable = false;
        }
    }
    
    /**
     * Predicts energy levels for the next 12 hours using Gemini API
     * 
     * @return CompletableFuture with EnergyPredictionResult containing 12 hourly predictions and explanation
     */
    public CompletableFuture<EnergyPredictionResult> predictEnergyLevels12Hours() {
        CompletableFuture<EnergyPredictionResult> future = new CompletableFuture<>();
        
        if (!isAvailable) {
            future.completeExceptionally(new IllegalStateException("Gemini API not available"));
            return future;
        }
        
        try {
            // Get health data summary
            HealthDataSummary healthSummary = dataAggregator.createHealthSummary(48);
            
            // Build prompt with health data
            String prompt = buildPredictionPrompt(healthSummary);
            
            Log.d(TAG, "Sending 12-hour prediction request to Gemini");
            
            GenerativeModelFutures modelFutures = GenerativeModelFutures.from(model);
            
            // Create content from string
            Content content = new Content.Builder().addText(prompt).build();
            
            ListenableFuture<GenerateContentResponse> response = modelFutures.generateContent(content);
            
            response.addListener(() -> {
                try {
                    GenerateContentResponse result = response.get();
                    String textResult = result.getText();
                    Log.d(TAG, "Gemini response: " + textResult);
                    
                    // Parse energy predictions and explanation from response
                    EnergyPredictionResult predictionResult = parsePredictionResult(textResult);
                    
                    if (predictionResult != null && predictionResult.getHourlyPredictions() != null 
                            && !predictionResult.getHourlyPredictions().isEmpty()) {
                        future.complete(predictionResult);
                    } else {
                        future.completeExceptionally(new Exception("Could not parse energy predictions from response"));
                    }
                } catch (ExecutionException | InterruptedException e) {
                    Log.e(TAG, "Error getting Gemini response", e);
                    future.completeExceptionally(e);
                }
            }, java.util.concurrent.Executors.newSingleThreadExecutor());
            
        } catch (Exception e) {
            Log.e(TAG, "Error in prediction", e);
            future.completeExceptionally(e);
        }
        
        return future;
    }
    
    /**
     * Predicts current energy level (convenience method - gets first hour from 12-hour forecast)
     * 
     * @return CompletableFuture with energy level (0-100)
     */
    public CompletableFuture<Double> predictCurrentEnergyLevel() {
        CompletableFuture<Double> future = new CompletableFuture<>();
        
        predictEnergyLevels12Hours().thenAccept(result -> {
            Double current = result.getCurrentEnergyLevel();
            if (current != null) {
                future.complete(current);
            } else {
                future.completeExceptionally(new IllegalStateException("No current energy level in result"));
            }
        }).exceptionally(error -> {
            future.completeExceptionally(error);
            return null;
        });
        
        return future;
    }
    
    /**
     * Builds a detailed prompt for Gemini with health data
     */
    private String buildPredictionPrompt(HealthDataSummary summary) {
        StringBuilder prompt = new StringBuilder();
        
        prompt.append("You are an expert in predicting human energy levels based on biometric data. ");
        prompt.append("Analyze the following health metrics and predict the energy levels for the next 12 hours.\n\n");
        
        prompt.append("HEALTH METRICS:\n");
        prompt.append("================\n\n");
        
        // Time context & Sleep Pressure (CRITICAL context)
        prompt.append(String.format("Time Context (CRITICAL):\n"));
        prompt.append(String.format("  - Current time: %s (%d:00)\n", summary.getTimeOfDay(), summary.getCurrentHour()));
        
        // Calculate Time Awake
        if (summary.getLastWakeTimestamp() != null) {
            long now = System.currentTimeMillis();
            long diffMs = now - summary.getLastWakeTimestamp();
            double hoursAwake = diffMs / (1000.0 * 60.0 * 60.0);
            prompt.append(String.format("  - Time Awake: %.1f hours\n", hoursAwake));
            
            if (hoursAwake > 14.0) {
                prompt.append("  - ALERT: High homeostatic sleep pressure (awake > 14 hours). Energy likely declining.\n");
            }
            if (hoursAwake > 16.0) {
                prompt.append("  - ALERT: Very high sleep pressure. Consider penalizing energy score significantly (<40) if user not engaged in high-alert activity.\n");
            }
        } else {
            // Fallback inference if wake time missing
            if (summary.getCurrentHour() >= 0 && summary.getCurrentHour() < 5) {
                prompt.append("  - Context: Late night / Early morning. High sleep pressure likely.\n");
            }
        }
        prompt.append("\n");
        
        // Sleep (most important for BASELINE, but time awake degrades it)
        if (summary.getLastNightSleepHours() != null) {
            prompt.append(String.format("Sleep History (Baseline Potential):\n"));
            prompt.append(String.format("  - Last night: %.1f hours\n", summary.getLastNightSleepHours()));
            if (summary.getAvgSleepHours() != null) {
                prompt.append(String.format("  - Average (recent): %.1f hours\n", summary.getAvgSleepHours()));
            }
            if (summary.getSleepQuality() != null) {
                prompt.append(String.format("  - Quality estimate: %.0f%%\n", summary.getSleepQuality() * 100));
            }
            prompt.append("\n");
        }
        
        // HRV (very important for recovery)
        if (summary.getCurrentHRV() != null) {
            prompt.append(String.format("HRV - Heart Rate Variability (Recovery Status):\n"));
            prompt.append(String.format("  - Current: %.1f ms\n", summary.getCurrentHRV()));
            if (summary.getAvgHRV() != null) {
                prompt.append(String.format("  - Average: %.1f ms\n", summary.getAvgHRV()));
            }
            prompt.append("\n");
        }
        
        // Heart Rate
        if (summary.getCurrentHeartRate() != null) {
            prompt.append(String.format("Heart Rate (Physiological State):\n"));
            prompt.append(String.format("  - Current: %.0f bpm\n", summary.getCurrentHeartRate()));
            if (summary.getAvgHeartRate() != null) {
                prompt.append(String.format("  - Average: %.0f bpm\n", summary.getAvgHeartRate()));
            }
            prompt.append("\n");
        }
        
        // Activity
        if (summary.getTodaySteps() != null) {
            prompt.append(String.format("Activity Level:\n"));
            prompt.append(String.format("  - Today's steps: %d\n", summary.getTodaySteps()));
            prompt.append("\n");
        }
        
        // Manual Energy Inputs (User Self-Reported - IMPORTANT CONTEXT BUT VERIFY)
        if (summary.getManualEnergyLevel() != null || summary.getPhysicalTiredness() != null) {
            prompt.append("User Self-Reported Energy (IMPORTANT CONTEXT):\n");
            long now = System.currentTimeMillis();
            if (summary.getLastManualInputTimestamp() != null) {
                double hoursAgo = (now - summary.getLastManualInputTimestamp()) / (1000.0 * 60.0 * 60.0);
                prompt.append(String.format("  - Reported %.1f hours ago\n", hoursAgo));
            }
            
            if (summary.getManualEnergyLevel() != null) {
                prompt.append(String.format("  - User explicitly stated energy level: %d/10\n", summary.getManualEnergyLevel()));
            }
            if (summary.getPhysicalTiredness() != null) {
                prompt.append(String.format("  - Physical tiredness: %d/10\n", summary.getPhysicalTiredness()));
            }
            if (summary.getRecentTask() != null) {
                prompt.append(String.format("  - Recent context/task: %s\n", summary.getRecentTask()));
            }
            if (summary.getCaffeineIntakeCups() != null) {
                prompt.append(String.format("  - Caffeine intake: %d cups\n", summary.getCaffeineIntakeCups()));
            }
            if (summary.getPredictionAccuracyRating() != null) {
                String accuracy = "Unknown";
                if (summary.getPredictionAccuracyRating() == 0) accuracy = "Accurate";
                else if (summary.getPredictionAccuracyRating() == 1) accuracy = "User says previous prediction was TOO HIGH";
                else if (summary.getPredictionAccuracyRating() == -1) accuracy = "User says previous prediction was TOO LOW";
                prompt.append(String.format("  - Feedback on previous prediction: %s (Adjust accordingly)\n", accuracy));
            }
            prompt.append("\n");
        }
        
        // Previous Prediction Context (Consistency)
        if (summary.getLastPredictedLevel() != null && summary.getLastPredictionTime() != null) {
             long now = System.currentTimeMillis();
             double hoursSinceLast = (now - summary.getLastPredictionTime()) / (1000.0 * 60.0 * 60.0);
             
             // Only include if recent (< 3 hours)
             if (hoursSinceLast < 3.0) {
                 prompt.append("PREVIOUS PREDICTION (Consistency Check):\n");
                 prompt.append(String.format("  - %.1f hours ago, you predicted: %.1f/100\n", hoursSinceLast, summary.getLastPredictedLevel()));
                 prompt.append("  - Reason: " + summary.getLastPredictionExplanation() + "\n");
                 prompt.append("  - INSTRUCTION: If the health data has not changed significantly since then, keep the new prediction CLOSE to this previous number (+/- 5 points). Changing it wildly without new data destroys user trust. You MAY rephrase the explanation to keep it fresh, but keep the score consistent.\n\n");
             }
        }
        
        prompt.append("PREDICTION INSTRUCTIONS:\n");
        prompt.append("=======================\n");
        prompt.append("Return a JSON object with the following structure:\n");
        prompt.append("{\n");
        prompt.append("  \"hourly_predictions\": [current_score, hour+1, hour+2, ...], // Array of 12 numbers (0-100)\n");
        prompt.append("  \"technical_analysis\": \"Detailed explanation including how manual inputs were weighed against objective data...\",\n");
        prompt.append("  \"actionable_insight\": \"A detailed, actionable paragraph (3-4 sentences) offering specific advice based on the data...\"\n");
        prompt.append("}\n\n");
        
        prompt.append("SCORING GUIDELINES:\n");
        prompt.append("1. **Verify Self-Reports**: Do NOT blindly trust the user's manual energy input. Users often overestimate their energy. Compare their report against objective metrics (HRV, Sleep, Time Awake).\n");
        prompt.append("   - If user reports High Energy (8-10/10) but HRV is low or Sleep was poor (<6h), CAP the score at 70-80. Do NOT output 100 unless objective data supports it.\n");
        prompt.append("   - If user reports Low Energy but data looks good, trust the user (they might be feeling something the sensors missed).\n");
        prompt.append("2. **Explanation**: Explicitly mention the user's self-reported data in the 'technical_analysis' and how you adjusted it based on the objective data (e.g., 'You reported high energy, but your low HRV suggests hidden fatigue, so we adjusted the score slightly lower.').\n");
        prompt.append("3. **Time Awake**: Consider homeostatic sleep pressure. If awake >14 hours, energy is likely lower.\n");
        prompt.append("4. **Circadian Rhythm**: While late night/early morning typically sees lower energy, individual schedules vary.\n");
        prompt.append("5. **Current vs Forecast**: The first number in 'hourly_predictions' is the CURRENT MOMENT. Align this with the adjusted self-report.\n");
        prompt.append("6. **Caffeine**: If caffeine intake is high/recent, it may temporarily counteract sleep pressure.\n");
        prompt.append("7. **FUTURE FORECAST (Next 12 Hours)**: Do NOT assume the user will go to sleep in the next 12 hours unless they have explicitly logged a sleep schedule. Assume they will stay awake.\n");
        
        prompt.append("IMPORTANT: Return ONLY valid JSON. No markdown formatting, no code blocks.\n");
        
        return prompt.toString();
    }
    
    /**
     * Parses energy prediction result from JSON response
     */
    private EnergyPredictionResult parsePredictionResult(String response) {
        try {
            // Clean up response if it contains markdown code blocks
            String jsonStr = response.trim();
            if (jsonStr.startsWith("```json")) {
                jsonStr = jsonStr.substring(7);
            }
            if (jsonStr.startsWith("```")) {
                jsonStr = jsonStr.substring(3);
            }
            if (jsonStr.endsWith("```")) {
                jsonStr = jsonStr.substring(0, jsonStr.length() - 3);
            }
            jsonStr = jsonStr.trim();
            
                try {
                Map<String, Object> json = gson.fromJson(jsonStr, 
                        new com.google.gson.reflect.TypeToken<Map<String, Object>>(){}.getType());
                    
                    List<Double> predictions = null;
                    String explanation = null;
                    String insight = null;
                    
                    // Extract hourly_predictions
                    if (json.containsKey("hourly_predictions")) {
                        Object predObj = json.get("hourly_predictions");
                        if (predObj instanceof List) {
                            List<?> predList = (List<?>) predObj;
                            predictions = new ArrayList<>();
                            for (Object item : predList) {
                                if (item instanceof Number) {
                                    double value = ((Number) item).doubleValue();
                                    predictions.add(Math.max(0, Math.min(100, value)));
                                }
                            }
                        }
                    }
                    
                    // Extract technical_analysis (mapped to explanation)
                    if (json.containsKey("technical_analysis")) {
                        Object expObj = json.get("technical_analysis");
                        if (expObj instanceof String) {
                            explanation = (String) expObj;
                        }
                    } else if (json.containsKey("explanation")) {
                        // Fallback for older prompt format
                        Object expObj = json.get("explanation");
                        if (expObj instanceof String) {
                            explanation = (String) expObj;
                        }
                    }
                    
                    // Extract actionable_insight
                    if (json.containsKey("actionable_insight")) {
                        Object insObj = json.get("actionable_insight");
                        if (insObj instanceof String) {
                            insight = (String) insObj;
                        }
                    }
                    
                if (predictions != null && predictions.size() >= 1) {
                     // Ensure we have 12 values
                    while (predictions.size() < 12) {
                        predictions.add(predictions.get(predictions.size() - 1));
                    }
                    if (predictions.size() > 12) {
                        predictions = predictions.subList(0, 12);
                    }
                    
                        if (explanation == null || explanation.isEmpty()) {
                            explanation = "Energy prediction based on health metrics.";
                        }
                        
                        if (insight == null || insight.isEmpty()) {
                            // Generate simple fallback insight
                             double firstVal = predictions.get(0);
                             if (firstVal > 70) insight = "Your energy is high! Great time for focus.";
                             else if (firstVal > 40) insight = "Moderate energy levels. Pace yourself.";
                             else insight = "Energy is low. Prioritize rest.";
                        }
                        
                        return new EnergyPredictionResult(predictions, explanation, insight);
                    }
                } catch (Exception e) {
                Log.w(TAG, "Error parsing JSON response: " + e.getMessage());
            }
            
            // Fallback: Try to extract from text response using regex if JSON parsing failed
            return parseFromTextResponse(response);
            
        } catch (Exception e) {
            Log.e(TAG, "Error parsing prediction result", e);
        }
        
        return null;
    }
    
    /**
     * Fallback: Extract predictions and explanation from text response
     */
    private EnergyPredictionResult parseFromTextResponse(String response) {
        try {
            List<Double> predictions = new ArrayList<>();
            String explanation = null;
            
            // Look for array pattern like [75, 72, 68, ...]
            java.util.regex.Pattern arrayPattern = java.util.regex.Pattern.compile(
                "\\[\\s*([0-9]+(?:\\.[0-9]+)?(?:\\s*,\\s*[0-9]+(?:\\.[0-9]+)?){0,11})\\s*\\]");
            java.util.regex.Matcher arrayMatcher = arrayPattern.matcher(response);
            
            if (arrayMatcher.find()) {
                String arrayStr = arrayMatcher.group(1);
                String[] numbers = arrayStr.split("\\s*,\\s*");
                for (String num : numbers) {
                    try {
                        double value = Double.parseDouble(num.trim());
                        predictions.add(Math.max(0, Math.min(100, value)));
                    } catch (NumberFormatException e) {
                        // Skip invalid numbers
                    }
                    }
                }
                
            // Pad if needed
            if (!predictions.isEmpty()) {
                while (predictions.size() < 12) {
                    predictions.add(predictions.get(predictions.size() - 1));
                }
                if (predictions.size() > 12) {
                    predictions = predictions.subList(0, 12);
                }
                
                explanation = "Predicted based on your health data.";
                return new EnergyPredictionResult(predictions, explanation);
            }
            
        } catch (Exception e) {
            Log.w(TAG, "Error parsing from text response", e);
        }
        
        return null;
    }
    
    public boolean isAvailable() {
        return isAvailable;
    }
}
