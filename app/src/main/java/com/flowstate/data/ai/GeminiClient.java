package com.flowstate.data.ai;

import android.content.Context;
import android.util.Log;

import com.flowstate.core.Config;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Client for Google Gemini API to generate AI insights from energy forecasts
 */
public class GeminiClient {
    private static final String TAG = "GeminiClient";
    private static final String GEMINI_API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent";
    
    private static GeminiClient instance;
    private final ExecutorService executor;
    private final String apiKey;
    
    private GeminiClient(Context context) {
        this.executor = Executors.newSingleThreadExecutor();
        this.apiKey = Config.GEMINI_API_KEY;
    }
    
    public static synchronized GeminiClient getInstance(Context context) {
        if (instance == null) {
            instance = new GeminiClient(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Generate AI insight from energy forecast
     * @param forecast List of predicted energy values
     * @param callback Callback to receive the generated insight text
     */
    public void generateInsight(List<Double> forecast, InsightCallback callback) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            callback.onFailure(new Exception("Gemini API key not configured in local.properties"));
            return;
        }
        
        executor.execute(() -> {
            try {
                String prompt = buildPrompt(forecast);
                String insight = callGeminiApi(prompt);
                callback.onSuccess(insight);
            } catch (Exception e) {
                Log.e(TAG, "Failed to generate insight", e);
                callback.onFailure(e);
            }
        });
    }
    
    private String buildPrompt(List<Double> forecast) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an energy level coach analyzing biometric data from Health Connect. This 12-hour energy forecast is based on:\n");
        sb.append("• **Sleep Quality & Duration** (PRIMARY FACTORS - most important for energy)\n");
        sb.append("• **HRV (Heart Rate Variability)** (KEY INDICATOR - shows recovery and stress levels)\n");
        sb.append("• **Sleep Timing** (when the user slept affects circadian rhythm)\n");
        sb.append("• Heart Rate (cardiovascular health indicator)\n");
        sb.append("• Step Count (activity level)\n");
        sb.append("• Typing Speed & Reaction Time (cognitive performance)\n");
        sb.append("• Time of Day (circadian rhythm patterns)\n\n");
        sb.append("Energy forecast starting from now: ");
        
        // Get current time and find peak/low periods
        java.util.Calendar cal = java.util.Calendar.getInstance();
        double maxEnergy = Double.MIN_VALUE;
        double minEnergy = Double.MAX_VALUE;
        int peakHour = 0;
        int lowHour = 0;
        
        for (int i = 0; i < forecast.size(); i++) {
            int hour = (cal.get(java.util.Calendar.HOUR_OF_DAY) + i) % 24;
            String amPm = hour >= 12 ? "PM" : "AM";
            int displayHour = hour == 0 ? 12 : (hour > 12 ? hour - 12 : hour);
            double energy = forecast.get(i);
            
            sb.append(String.format("%d:00%s: %.1f", displayHour, amPm, energy));
            if (i < forecast.size() - 1) sb.append(", ");
            
            // Track peak and low
            if (energy > maxEnergy) {
                maxEnergy = energy;
                peakHour = hour;
            }
            if (energy < minEnergy) {
                minEnergy = energy;
                lowHour = hour;
            }
        }
        
        // Format peak and low times
        String peakAmPm = peakHour >= 12 ? "PM" : "AM";
        int peakDisplay = peakHour == 0 ? 12 : (peakHour > 12 ? peakHour - 12 : peakHour);
        String lowAmPm = lowHour >= 12 ? "PM" : "AM";
        int lowDisplay = lowHour == 0 ? 12 : (lowHour > 12 ? lowHour - 12 : lowHour);
        
        sb.append("\n\nYou MUST include ALL of the following:\n");
        sb.append(String.format("1. PEAK ENERGY: State exactly '%d:00%s (%.0f/100)'\n", peakDisplay, peakAmPm, maxEnergy));
        sb.append(String.format("2. LOWEST ENERGY: State exactly '%d:00%s (%.0f/100)'\n", lowDisplay, lowAmPm, minEnergy));
        sb.append("3. **PRIMARY EXPLANATION**: Explain WHY the peak and low occur at these times by focusing on:\n");
        sb.append("   - How their SLEEP HOURS and SLEEP QUALITY from Health Connect data directly caused these energy patterns\n");
        sb.append("   - How their HRV (Heart Rate Variability) indicates recovery quality and stress levels affecting these times\n");
        sb.append("   - How their SLEEP TIMING (when they went to bed/woke up) aligns or conflicts with their circadian rhythm\n");
        sb.append("   - Connect the dots: 'Your low energy at [time] is because...' or 'Your peak at [time] is due to...'\n");
        sb.append("4. TASK SCHEDULING: Give specific advice on what types of tasks to schedule during peak vs low energy times\n");
        sb.append("5. HEALTH ADVICE: Provide actionable sleep/HRV recommendations to improve energy (e.g., 'Sleep earlier to shift peak energy', 'Increase sleep duration by 1 hour', 'Improve HRV through stress management')\n");
        sb.append("\nFormat as a clear, conversational paragraph (4-6 sentences). EMPHASIZE the sleep and HRV connection to energy levels. Be direct about cause and effect.");
        return sb.toString();
    }
    
    private String callGeminiApi(String prompt) throws Exception {
        URL url = new URL(GEMINI_API_BASE + "?key=" + apiKey);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(15000);
        
        // Build request body
        JSONObject requestBody = new JSONObject();
        JSONArray contents = new JSONArray();
        JSONObject content = new JSONObject();
        JSONArray parts = new JSONArray();
        JSONObject part = new JSONObject();
        part.put("text", prompt);
        parts.put(part);
        content.put("parts", parts);
        contents.put(content);
        requestBody.put("contents", contents);
        
        // Send request
        try (OutputStream os = conn.getOutputStream()) {
            byte[] input = requestBody.toString().getBytes("utf-8");
            os.write(input, 0, input.length);
        }
        
        // Read response
        int responseCode = conn.getResponseCode();
        if (responseCode != 200) {
            // Try to read error response
            String errorMsg = "";
            try (BufferedReader br = new BufferedReader(
                    new InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                StringBuilder errorResponse = new StringBuilder();
                String line;
                while ((line = br.readLine()) != null) {
                    errorResponse.append(line.trim());
                }
                errorMsg = errorResponse.toString();
            } catch (Exception e) {
                errorMsg = e.getMessage();
            }
            Log.e(TAG, "Gemini API error: " + responseCode + " - " + errorMsg);
            throw new Exception("Gemini API returned code " + responseCode + ": " + errorMsg);
        }
        
        StringBuilder response = new StringBuilder();
        try (BufferedReader br = new BufferedReader(
                new InputStreamReader(conn.getInputStream(), "utf-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                response.append(line.trim());
            }
        }
        
        // Parse response
        JSONObject responseJson = new JSONObject(response.toString());
        JSONArray candidates = responseJson.getJSONArray("candidates");
        if (candidates.length() > 0) {
            JSONObject candidate = candidates.getJSONObject(0);
            JSONObject responseContent = candidate.getJSONObject("content");
            JSONArray responseParts = responseContent.getJSONArray("parts");
            if (responseParts.length() > 0) {
                JSONObject responsePart = responseParts.getJSONObject(0);
                return responsePart.getString("text");
            }
        }
        
        throw new Exception("No text in Gemini response");
    }
    
    public interface InsightCallback {
        void onSuccess(String insight);
        void onFailure(Exception e);
    }
}
