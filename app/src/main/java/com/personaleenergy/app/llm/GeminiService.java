package com.personaleenergy.app.llm;

import android.content.Context;
import android.util.Log;
import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.core.Config;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Collections;
import java.util.concurrent.TimeUnit;

/**
 * Google Gemini API Service for generating personalized schedules
 * Uses Gemini 1.5 Flash via REST API to create tailored daily schedules based on energy predictions
 */
public class GeminiService {
    
    private static final String TAG = "GeminiService";
    private static final String GEMINI_API_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final String MODEL_NAME = "gemini-2.5-flash";
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    private final Context context;
    private GoogleCredentials serviceAccountCredentials;
    private static final String SERVICE_ACCOUNT_FILE = "service_account_key.json";
    
    public GeminiService() {
        this.context = null;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        
        // Try to get API key from Config first, fallback to BuildConfig directly
        String keyFromConfig = Config.GEMINI_API_KEY;
        String keyFromBuildConfig = com.flowstate.app.BuildConfig.GEMINI_API_KEY;
        
        // Use BuildConfig directly if Config is null/empty (more reliable)
        String finalKey = null;
        if (keyFromConfig != null && !keyFromConfig.trim().isEmpty()) {
            finalKey = keyFromConfig.trim();
        } else if (keyFromBuildConfig != null && !keyFromBuildConfig.trim().isEmpty()) {
            finalKey = keyFromBuildConfig.trim();
            Log.w(TAG, "⚠️ Using BuildConfig key directly (Config was null/empty)");
        }
        
        this.apiKey = finalKey;
        initializeServiceAccount(null);
    }
    
    public GeminiService(Context context) {
        this.context = context != null ? context.getApplicationContext() : null;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        
        // Try to load service account credentials first
        String finalKey = null;
        if (this.context != null) {
            try {
                InputStream serviceAccountStream = this.context.getAssets().open(SERVICE_ACCOUNT_FILE);
                ServiceAccountCredentials credentials = ServiceAccountCredentials.fromStream(serviceAccountStream);
                serviceAccountStream.close();
                
                // Set the required OAuth scope for Gemini API
                this.serviceAccountCredentials = credentials.createScoped(
                    Collections.singletonList("https://www.googleapis.com/auth/generative-language")
                );
                
                // Refresh credentials to get access token
                this.serviceAccountCredentials.refreshIfExpired();
                
                Log.d(TAG, "✅ Service account credentials loaded successfully");
                Log.d(TAG, "Service account email: " + ((ServiceAccountCredentials) this.serviceAccountCredentials).getClientEmail());
                
                // Set API key to null since we're using service account
                finalKey = null;
            } catch (IOException e) {
                Log.d(TAG, "Service account file not found in assets, falling back to API key: " + e.getMessage());
                // Continue to API key initialization
                finalKey = getApiKeyFromConfig();
            } catch (Exception e) {
                Log.w(TAG, "Error loading service account credentials, falling back to API key", e);
                // Continue to API key initialization
                finalKey = getApiKeyFromConfig();
            }
        } else {
            finalKey = getApiKeyFromConfig();
        }
        
        // Initialize final field - if service account loaded, apiKey is null, otherwise use the key
        this.apiKey = finalKey;
        initializeServiceAccount(this.context);
    }
    
    private String getApiKeyFromConfig() {
        // Try to get API key from Config first, fallback to BuildConfig directly
        String keyFromConfig = Config.GEMINI_API_KEY;
        String keyFromBuildConfig = com.flowstate.app.BuildConfig.GEMINI_API_KEY;
        
        // Use BuildConfig directly if Config is null/empty (more reliable)
        if (keyFromConfig != null && !keyFromConfig.trim().isEmpty()) {
            return keyFromConfig.trim();
        } else if (keyFromBuildConfig != null && !keyFromBuildConfig.trim().isEmpty()) {
            Log.w(TAG, "⚠️ Using BuildConfig key directly (Config was null/empty)");
            return keyFromBuildConfig.trim();
        }
        return null;
    }
    
    private void initializeServiceAccount(Context context) {
        // Debug logging for API key
        String keyFromConfig = Config.GEMINI_API_KEY;
        String keyFromBuildConfig = com.flowstate.app.BuildConfig.GEMINI_API_KEY;
        
        Log.d(TAG, "=== Gemini API Key Debug ===");
        Log.d(TAG, "Config.GEMINI_API_KEY: " + (keyFromConfig != null ? "not null (length: " + keyFromConfig.length() + ")" : "null"));
        Log.d(TAG, "BuildConfig.GEMINI_API_KEY: " + (keyFromBuildConfig != null ? "not null (length: " + keyFromBuildConfig.length() + ")" : "null"));
        
        if (apiKey == null || apiKey.isEmpty() || apiKey.trim().isEmpty()) {
            Log.e(TAG, "⚠️ Gemini API key is NULL or EMPTY - API calls will fail");
            Log.e(TAG, "⚠️ Make sure GEMINI_API_KEY is set in local.properties and rebuild the app");
            
            // If Config key is empty, try BuildConfig directly
            if ((keyFromConfig == null || keyFromConfig.isEmpty()) && keyFromBuildConfig != null && !keyFromBuildConfig.isEmpty()) {
                Log.w(TAG, "⚠️ Config key is empty, but BuildConfig has a key. Using BuildConfig key directly.");
                // Note: We can't reassign final field, but we'll use it in the request method
            }
        } else {
            // Trim the key to remove any whitespace
            String trimmedKey = apiKey.trim();
            if (!trimmedKey.equals(apiKey)) {
                Log.w(TAG, "⚠️ API key had whitespace - trimmed");
            }
            
            // Log first 10 and last 4 characters for debugging (don't log full key for security)
            String maskedKey = trimmedKey.length() > 14 
                ? trimmedKey.substring(0, 10) + "..." + trimmedKey.substring(trimmedKey.length() - 4)
                : "***";
            Log.d(TAG, "✅ Gemini API key loaded: " + maskedKey);
            Log.d(TAG, "✅ API key length: " + trimmedKey.length() + " characters");
            
            // Validate API key format (should start with AIza)
            if (!trimmedKey.startsWith("AIza")) {
                Log.e(TAG, "❌ ERROR: API key doesn't start with 'AIza' - INVALID KEY");
                Log.e(TAG, "❌ API key starts with: " + (trimmedKey.length() > 4 ? trimmedKey.substring(0, 4) : trimmedKey));
            } else {
                Log.d(TAG, "✅ API key format looks valid (starts with 'AIza')");
            }
        }
        Log.d(TAG, "=== End Gemini API Key Debug ===");
    }
    
    /**
     * Generate personalized schedule using Google Gemini
     */
    public void generateSchedule(
            List<EnergyPrediction> energyPredictions,
            List<String> userTasks,
            List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> tasksWithEnergy,
            List<String> existingEvents,
            ScheduleCallback callback) {
        
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "Gemini API key not configured");
            callback.onError(new Exception("Gemini API key not configured. Please add GEMINI_API_KEY to local.properties"));
            return;
        }
        
        // Build prompt for Gemini
        String prompt = buildSchedulePrompt(energyPredictions, userTasks, tasksWithEnergy, existingEvents, null);
        
        // Make API call
        makeGeminiRequest(prompt, callback);
    }
    
    /**
     * Build comprehensive prompt for Gemini
     */
    private String buildSchedulePrompt(
            List<EnergyPrediction> energyPredictions,
            List<String> userTasks,
            List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> tasksWithEnergy,
            List<String> existingEvents,
            String sleepPatternInfo) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI productivity coach creating an optimized daily schedule based on data-driven energy predictions.\n\n");
        
        // Add sleep pattern information if available
        if (sleepPatternInfo != null && !sleepPatternInfo.isEmpty()) {
            prompt.append("SLEEP PATTERNS (CRITICAL - DO NOT SCHEDULE DURING SLEEP HOURS):\n");
            prompt.append(sleepPatternInfo);
            prompt.append("\nCRITICAL SLEEP RULES:\n");
            prompt.append("- NEVER schedule tasks between 11:00 PM and 6:00 AM (sleep hours)\n");
            prompt.append("- All tasks must be scheduled between 6:00 AM and 11:00 PM only\n");
            prompt.append("- Do NOT schedule any tasks at 11:00 PM, 12:00 AM (midnight), or any hour between 1:00 AM and 5:59 AM\n");
            prompt.append("- The earliest time to schedule tasks is 6:00 AM\n");
            prompt.append("- The latest time to schedule tasks is 10:59 PM\n\n");
        } else {
            // Default sleep window if no data available
            prompt.append("SLEEP PATTERNS:\n");
            prompt.append("CRITICAL: Do NOT schedule tasks between 11:00 PM and 6:00 AM (sleep hours).\n");
            prompt.append("All tasks must be scheduled between 6:00 AM and 11:00 PM only.\n\n");
        }
        
        // Energy predictions analysis - DETAILED DATA FOR INFORMED DECISIONS
        prompt.append("ENERGY PREDICTIONS FOR TODAY (Hour-by-Hour Data):\n");
        prompt.append("These predictions are based on machine learning analysis of sleep, heart rate, cognitive tests, and historical patterns.\n");
        prompt.append("Use these predictions to match task energy requirements to predicted energy levels.\n\n");
        
        Map<Integer, List<EnergyPrediction>> byHour = new HashMap<>();
        for (EnergyPrediction pred : energyPredictions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pred.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            byHour.computeIfAbsent(hour, k -> new ArrayList<>()).add(pred);
        }
        
        // Show all hours with predictions, even if sparse
        boolean hasPredictions = false;
        for (int hour = 0; hour < 24; hour++) {
            if (byHour.containsKey(hour)) {
                hasPredictions = true;
                List<EnergyPrediction> preds = byHour.get(hour);
                EnergyPrediction latest = preds.get(preds.size() - 1);
                String period = hour < 12 ? "AM" : "PM";
                int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
                String timeStr = String.format("%d:00 %s", displayHour, period);
                prompt.append(String.format("- %s: %s energy (confidence: %.0f%%)\n", 
                    timeStr, latest.getPredictedLevel(), latest.getConfidence() * 100));
            }
        }
        
        if (!hasPredictions) {
            prompt.append("⚠️ No energy predictions available for today. Use general patterns: morning = higher energy, afternoon = medium, evening = lower.\n");
        }
        
        // Find peak energy hours
        List<Integer> peakHours = findPeakEnergyHours(energyPredictions);
        List<Integer> lowHours = findLowEnergyHours(energyPredictions);
        
        prompt.append("\n📊 ENERGY ANALYSIS SUMMARY:\n");
        prompt.append("PEAK ENERGY HOURS (best for HIGH energy tasks): ");
        for (int i = 0; i < peakHours.size(); i++) {
            int hour = peakHours.get(i);
            String period = hour < 12 ? "AM" : "PM";
            int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            prompt.append(String.format("%d:00 %s", displayHour, period));
            if (i < peakHours.size() - 1) prompt.append(", ");
        }
        prompt.append("\n");
        
        prompt.append("LOW ENERGY HOURS (best for LOW energy tasks): ");
        for (int i = 0; i < lowHours.size(); i++) {
            int hour = lowHours.get(i);
            String period = hour < 12 ? "AM" : "PM";
            int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            prompt.append(String.format("%d:00 %s", displayHour, period));
            if (i < lowHours.size() - 1) prompt.append(", ");
        }
        prompt.append("\n\n");
        
        // User tasks with energy requirements
        prompt.append("TASKS TO SCHEDULE (with energy intensity):\n");
        for (int i = 0; i < userTasks.size(); i++) {
            String taskName = userTasks.get(i);
            String energyLevel = "MEDIUM"; // Default
            if (i < tasksWithEnergy.size()) {
                com.personaleenergy.app.ui.schedule.TaskWithEnergy task = tasksWithEnergy.get(i);
                switch (task.getEnergyLevel()) {
                    case HIGH:
                        energyLevel = "HIGH";
                        break;
                    case MEDIUM:
                        energyLevel = "MEDIUM";
                        break;
                    case LOW:
                        energyLevel = "LOW";
                        break;
                }
            }
            prompt.append(String.format("%d. %s [Energy: %s]\n", i + 1, taskName, energyLevel));
        }
        prompt.append("\n");
        
        // Existing events
        if (existingEvents != null && !existingEvents.isEmpty()) {
            prompt.append("EXISTING CALENDAR EVENTS (cannot be moved):\n");
            for (String event : existingEvents) {
                prompt.append("- ").append(event).append("\n");
            }
            prompt.append("\n");
        }
        
        // Instructions - CRITICAL: Must use exact format
        prompt.append("CRITICAL OUTPUT FORMAT REQUIREMENTS:\n");
        prompt.append("You MUST return ONLY a schedule in this EXACT format. Do NOT include any explanations, introductions, or additional text.\n");
        prompt.append("Each line must be in this format: HH:MM AM/PM - HH:MM AM/PM: EXACT_TASK_NAME\n");
        prompt.append("Use ONLY the exact task names from the TASKS TO SCHEDULE list above. Do NOT create new tasks or add descriptions.\n\n");
        
        prompt.append("SCHEDULING INSTRUCTIONS (based on energy predictions and sleep patterns):\n");
        prompt.append("1. Schedule ONLY the tasks listed in 'TASKS TO SCHEDULE' above - use their EXACT names\n");
        prompt.append("2. CRITICAL: NEVER schedule tasks between 11:00 PM and 6:00 AM - this is the sleep window\n");
        prompt.append("3. MATCH TASK ENERGY TO PREDICTED ENERGY:\n");
        prompt.append("   - Tasks marked [Energy: HIGH] → Schedule during PEAK ENERGY HOURS or hours with HIGH predicted energy\n");
        prompt.append("   - Tasks marked [Energy: MEDIUM] → Schedule during MEDIUM predicted energy hours\n");
        prompt.append("   - Tasks marked [Energy: LOW] → Schedule during LOW ENERGY HOURS or hours with LOW predicted energy\n");
        prompt.append("4. Use the hour-by-hour ENERGY PREDICTIONS above to find the best time slots for each task\n");
        prompt.append("5. CRITICAL: Existing calendar events are IMMUTABLE - NEVER schedule tasks at the same time as existing events\n");
        prompt.append("6. If a task would conflict with an existing event, schedule it at a different time\n");
        prompt.append("7. Each task should be scheduled for approximately 1 hour\n");
        prompt.append("8. Do NOT add breaks, meal times, or any tasks not in the TASKS TO SCHEDULE list\n");
        prompt.append("9. Do NOT include any explanatory text, reasoning, or motivational tips in the output\n");
        prompt.append("10. Prioritize matching task energy requirements to predicted energy levels for optimal performance\n");
        prompt.append("11. All tasks must be scheduled during awake hours only (respect sleep patterns)\n\n");
        
        prompt.append("OUTPUT FORMAT (example):\n");
        prompt.append("9:00 AM - 10:00 AM: Task Name 1\n");
        prompt.append("10:00 AM - 11:00 AM: Task Name 2\n");
        prompt.append("11:00 AM - 12:00 PM: Task Name 3\n\n");
        
        prompt.append("Now generate the schedule using ONLY the exact format above with ONLY the tasks from the TASKS TO SCHEDULE list:");
        
        return prompt.toString();
    }
    
    /**
     * Build prompt with sleep pattern information
     */
    public void generateScheduleWithSleep(
            List<EnergyPrediction> energyPredictions,
            List<String> userTasks,
            List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> tasksWithEnergy,
            List<String> existingEvents,
            String sleepPatternInfo,
            ScheduleCallback callback) {
        
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "Gemini API key not configured");
            callback.onError(new Exception("Gemini API key not configured"));
            return;
        }
        
        // Build prompt with sleep info
        String prompt = buildSchedulePrompt(energyPredictions, userTasks, tasksWithEnergy, existingEvents, sleepPatternInfo);
        
        // Make API call
        makeGeminiRequest(prompt, callback);
    }
    
    /**
     * Make API request to Google Gemini using REST API
     * Supports both API key and service account authentication
     */
    private void makeGeminiRequest(String prompt, ScheduleCallback callback) {
        // Check if we're using service account authentication
        if (serviceAccountCredentials != null) {
            makeGeminiRequestWithServiceAccount(prompt, callback);
            return;
        }
        
        // Fallback to API key authentication
        // Get the actual API key to use - use instance field first, then try BuildConfig directly
        String keyToUse = apiKey;
        
        // If instance field is null/empty, try BuildConfig directly
        if (keyToUse == null || keyToUse.trim().isEmpty()) {
            keyToUse = com.flowstate.app.BuildConfig.GEMINI_API_KEY;
            if (keyToUse != null) {
                keyToUse = keyToUse.trim();
                Log.w(TAG, "⚠️ Using BuildConfig key directly (instance field was null/empty)");
            }
        } else {
            keyToUse = keyToUse.trim();
        }
        
        // Final validation - if still null/empty, fail
        if (keyToUse == null || keyToUse.isEmpty()) {
            Log.e(TAG, "❌ API key is null or empty when making request!");
            Log.e(TAG, "Instance apiKey field: " + (apiKey != null ? "not null (length: " + apiKey.length() + ")" : "null"));
            Log.e(TAG, "Config.GEMINI_API_KEY: " + (Config.GEMINI_API_KEY != null 
                ? "not null (length: " + Config.GEMINI_API_KEY.length() + ")" 
                : "null"));
            Log.e(TAG, "BuildConfig.GEMINI_API_KEY: " + (com.flowstate.app.BuildConfig.GEMINI_API_KEY != null 
                ? "not null (length: " + com.flowstate.app.BuildConfig.GEMINI_API_KEY.length() + ")" 
                : "null"));
            Log.e(TAG, "⚠️ CRITICAL: API key not found in any source. Check:");
            Log.e(TAG, "   1. GEMINI_API_KEY is set in local.properties");
            Log.e(TAG, "   2. App has been rebuilt after adding the key");
            Log.e(TAG, "   3. No typos in the property name");
            Log.e(TAG, "   4. OR use service account: place service_account_key.json in app/src/main/assets/");
            callback.onError(new Exception("Gemini API key is null or empty. Please check local.properties and rebuild the app, or use service account authentication."));
            return;
        }
        
        Log.d(TAG, "✅ Using API key authentication");
        Log.d(TAG, "✅ API key length: " + keyToUse.length());
        Log.d(TAG, "✅ API key starts with: " + (keyToUse.length() > 4 ? keyToUse.substring(0, 4) : keyToUse));
        
        try {
            // Build Gemini API request format
            JsonObject requestBody = new JsonObject();
            
            // Contents array with parts
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            
            requestBody.add("contents", contents);
            
            // Add generation config
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.7);
            // Increase maxOutputTokens to 8192 for longer schedule responses
            // Gemini 2.5 Flash supports up to 65,536 output tokens
            generationConfig.addProperty("maxOutputTokens", 8192);
            requestBody.add("generationConfig", generationConfig);
            
            // For Gemini API, the key should be passed as-is in the query parameter
            // Only encode if it contains characters that need encoding
            // Most API keys are alphanumeric with dashes/underscores which don't need encoding
            String encodedApiKey;
            try {
                // Try encoding - if it changes the length, it had special chars
                String testEncoded = java.net.URLEncoder.encode(keyToUse, "UTF-8");
                if (testEncoded.equals(keyToUse)) {
                    // No encoding needed - use as-is
                    encodedApiKey = keyToUse;
                    Log.d(TAG, "API key doesn't need URL encoding");
                } else {
                    // Encoding changed something - use encoded version
                    encodedApiKey = testEncoded;
                    Log.d(TAG, "API key was URL encoded (length changed from " + keyToUse.length() + " to " + encodedApiKey.length() + ")");
                }
            } catch (Exception e) {
                Log.w(TAG, "Error encoding API key, using as-is", e);
                encodedApiKey = keyToUse;
            }
            
            // Build URL with API key as query parameter (Gemini API requires key as query param)
            String url = GEMINI_API_BASE_URL + "/" + MODEL_NAME + ":generateContent?key=" + encodedApiKey;
            
            // Log the actual URL structure (with key masked) for debugging
            String urlForLogging = url.replace(encodedApiKey, "***KEY_MASKED***");
            Log.d(TAG, "Full request URL: " + urlForLogging);
            
            RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
            );
            
            Request.Builder requestBuilder = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json");
            
            // Note: Gemini API ONLY accepts key as query parameter, NOT as Bearer token or header
            // So we only add it to the URL, not as a header
            
            Request request = requestBuilder.post(body).build();
            
            Log.d(TAG, "Making Gemini API request to: " + GEMINI_API_BASE_URL + "/" + MODEL_NAME);
            Log.d(TAG, "Using model: " + MODEL_NAME);
            Log.d(TAG, "Request body size: " + requestBody.toString().length() + " characters");
            Log.d(TAG, "API key length: " + keyToUse.length());
            Log.d(TAG, "API key starts with: " + (keyToUse.length() > 4 ? keyToUse.substring(0, 4) : keyToUse));
            Log.d(TAG, "API key ends with: " + (keyToUse.length() > 4 ? "..." + keyToUse.substring(keyToUse.length() - 4) : keyToUse));
            Log.d(TAG, "Full URL (masked): " + url.replace(keyToUse, "***MASKED***"));
            Log.d(TAG, "Encoded key length: " + encodedApiKey.length());
            Log.d(TAG, "URL contains 'key=': " + url.contains("key="));
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e(TAG, "Gemini API request failed", e);
                    callback.onError(e);
                }
                
                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    Log.d(TAG, "Gemini API response code: " + response.code());
                    
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "❌ Gemini API error: " + response.code() + " - " + errorBody);
                        
                        // Check for specific error types
                        if (response.code() == 400) {
                            // Parse error details
                            try {
                                JsonObject errorJson = gson.fromJson(errorBody, JsonObject.class);
                                if (errorJson.has("error")) {
                                    JsonObject error = errorJson.getAsJsonObject("error");
                                    String message = error.has("message") ? error.get("message").getAsString() : "Unknown error";
                                    Log.e(TAG, "⚠️ Bad Request (400): " + message);
                                    
                                    if (message.contains("API Key not found") || message.contains("API_KEY_INVALID")) {
                                        Log.e(TAG, "⚠️ TROUBLESHOOTING:");
                                        Log.e(TAG, "   1. Verify the API key is correct in local.properties");
                                        Log.e(TAG, "   2. Go to https://aistudio.google.com/app/apikey and verify the key is active");
                                        Log.e(TAG, "   3. Ensure 'Generative Language API' is enabled in Google Cloud Console");
                                        Log.e(TAG, "   4. Check if the API key has restrictions (IP, referrer, etc.) that might block requests");
                                        Log.e(TAG, "   5. Try creating a new API key if this one is expired or invalid");
                                    }
                                }
                            } catch (Exception e) {
                                Log.e(TAG, "Could not parse error response", e);
                            }
                        } else if (response.code() == 401) {
                            Log.e(TAG, "⚠️ Authentication failed - API key may be invalid or expired");
                        } else if (response.code() == 403) {
                            Log.e(TAG, "⚠️ Permission denied - API key may not have access to Gemini API");
                            Log.e(TAG, "⚠️ Ensure 'Generative Language API' is enabled in Google Cloud Console");
                        } else if (response.code() == 429) {
                            Log.e(TAG, "⚠️ Rate limit exceeded - check your Gemini account quota");
                        } else if (response.code() == 404) {
                            Log.e(TAG, "⚠️ Model not found - check if model name is correct");
                        }
                        
                        callback.onError(new Exception("Gemini API error: " + response.code() + " - " + errorBody));
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    Log.d(TAG, "✅ Gemini API response received successfully");
                    Log.d(TAG, "Response body length: " + responseBody.length() + " characters");
                    
                    try {
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                        if (candidates != null && candidates.size() > 0) {
                            JsonObject candidate = candidates.get(0).getAsJsonObject();
                            JsonObject content = candidate.getAsJsonObject("content");
                            if (content != null) {
                                JsonArray parts = content.getAsJsonArray("parts");
                                if (parts != null && parts.size() > 0) {
                                    JsonObject part = parts.get(0).getAsJsonObject();
                                    String text = part.get("text").getAsString();
                                    
                                    Log.d(TAG, "Schedule generated successfully");
                                    Log.d(TAG, "Response preview: " + (text.length() > 200 ? text.substring(0, 200) + "..." : text));
                                    callback.onSuccess(text);
                                } else {
                                    callback.onError(new Exception("No text in Gemini response parts"));
                                }
                            } else {
                                callback.onError(new Exception("No content in Gemini response"));
                            }
                        } else {
                            callback.onError(new Exception("No candidates in Gemini response"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Gemini response", e);
                        Log.e(TAG, "Response body: " + responseBody);
                        callback.onError(e);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating Gemini request", e);
            callback.onError(e);
        }
    }
    
    /**
     * Make API request to Google Gemini using service account authentication
     */
    private void makeGeminiRequestWithServiceAccount(String prompt, ScheduleCallback callback) {
        try {
            Log.d(TAG, "✅ Using service account authentication");
            
            // Refresh credentials to get a valid access token
            serviceAccountCredentials.refreshIfExpired();
            String accessToken = serviceAccountCredentials.getAccessToken().getTokenValue();
            
            if (accessToken == null || accessToken.isEmpty()) {
                Log.e(TAG, "❌ Failed to get access token from service account");
                callback.onError(new Exception("Failed to get access token from service account"));
                return;
            }
            
            Log.d(TAG, "✅ Access token obtained (length: " + accessToken.length() + ")");
            
            // Build Gemini API request format
            JsonObject requestBody = new JsonObject();
            
            // Contents array with parts
            JsonArray contents = new JsonArray();
            JsonObject content = new JsonObject();
            JsonArray parts = new JsonArray();
            JsonObject part = new JsonObject();
            part.addProperty("text", prompt);
            parts.add(part);
            content.add("parts", parts);
            contents.add(content);
            
            requestBody.add("contents", contents);
            
            // Add generation config
            JsonObject generationConfig = new JsonObject();
            generationConfig.addProperty("temperature", 0.7);
            // Increase maxOutputTokens to 8192 for longer schedule responses
            // Gemini 2.5 Flash supports up to 65,536 output tokens
            generationConfig.addProperty("maxOutputTokens", 8192);
            requestBody.add("generationConfig", generationConfig);
            
            // Build URL WITHOUT API key (using Bearer token instead)
            String url = GEMINI_API_BASE_URL + "/" + MODEL_NAME + ":generateContent";
            
            RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + accessToken)
                .post(body)
                .build();
            
            Log.d(TAG, "Making Gemini API request with service account to: " + url);
            Log.d(TAG, "Request body size: " + requestBody.toString().length() + " characters");
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e(TAG, "Gemini API request failed (service account)", e);
                    callback.onError(e);
                }
                
                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    Log.d(TAG, "Gemini API response code (service account): " + response.code());
                    
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "❌ Gemini API error (service account): " + response.code() + " - " + errorBody);
                        
                        if (response.code() == 401 || response.code() == 403) {
                            Log.e(TAG, "⚠️ Authentication failed - service account may not have proper permissions");
                            Log.e(TAG, "⚠️ Ensure the service account has 'Generative Language API User' role");
                        }
                        
                        callback.onError(new Exception("Gemini API error: " + response.code() + " - " + errorBody));
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    Log.d(TAG, "✅ Gemini API response received successfully (service account)");
                    Log.d(TAG, "Response body length: " + responseBody.length() + " characters");
                    
                    try {
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        
                        // Log the full response structure for debugging
                        Log.d(TAG, "Full Gemini response structure: " + jsonResponse.toString());
                        
                        JsonArray candidates = jsonResponse.getAsJsonArray("candidates");
                        if (candidates == null || candidates.size() == 0) {
                            Log.e(TAG, "No candidates in response. Full response: " + responseBody);
                            callback.onError(new Exception("No candidates in Gemini response. Response: " + responseBody.substring(0, Math.min(500, responseBody.length()))));
                            return;
                        }
                        
                        JsonObject candidate = candidates.get(0).getAsJsonObject();
                        Log.d(TAG, "Candidate object: " + candidate.toString());
                        
                        // Check for finishReason - if it's blocked or filtered, that's the issue
                        String finishReason = null;
                        if (candidate.has("finishReason")) {
                            finishReason = candidate.get("finishReason").getAsString();
                            Log.d(TAG, "Finish reason: " + finishReason);
                            if (finishReason.equals("MAX_TOKENS")) {
                                Log.w(TAG, "⚠️ Response was truncated due to token limit. Consider increasing maxOutputTokens or simplifying the prompt.");
                            } else if (!finishReason.equals("STOP")) {
                                Log.w(TAG, "Unexpected finish reason: " + finishReason);
                            }
                        }
                        
                        JsonObject content = candidate.getAsJsonObject("content");
                        if (content == null) {
                            Log.e(TAG, "No content in candidate. Candidate: " + candidate.toString());
                            callback.onError(new Exception("No content in Gemini response candidate"));
                            return;
                        }
                        
                        JsonArray parts = content.getAsJsonArray("parts");
                        if (parts == null || parts.size() == 0) {
                            Log.e(TAG, "No parts in content. Content: " + content.toString());
                            // If finishReason is MAX_TOKENS, the response was cut off before any content
                            if ("MAX_TOKENS".equals(finishReason)) {
                                callback.onError(new Exception("Response was truncated due to token limit. The prompt may be too long or maxOutputTokens too low."));
                            } else {
                                callback.onError(new Exception("No parts in Gemini response content"));
                            }
                            return;
                        }
                        
                        // Try to get text from the first part
                        JsonObject part = parts.get(0).getAsJsonObject();
                        Log.d(TAG, "First part: " + part.toString());
                        
                        if (!part.has("text")) {
                            Log.e(TAG, "Part does not have 'text' field. Part: " + part.toString());
                            callback.onError(new Exception("No 'text' field in Gemini response part. Part: " + part.toString()));
                            return;
                        }
                        
                        String text = part.get("text").getAsString();
                        
                        if (text == null || text.trim().isEmpty()) {
                            Log.e(TAG, "Text is null or empty. Part: " + part.toString());
                            callback.onError(new Exception("Text is null or empty in Gemini response"));
                            return;
                        }
                        
                        Log.d(TAG, "Schedule generated successfully (service account)");
                        Log.d(TAG, "Response preview: " + (text.length() > 200 ? text.substring(0, 200) + "..." : text));
                        callback.onSuccess(text);
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing Gemini response (service account)", e);
                        Log.e(TAG, "Response body: " + responseBody);
                        callback.onError(e);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating Gemini request with service account", e);
            callback.onError(e);
        }
    }
    
    private List<Integer> findPeakEnergyHours(List<EnergyPrediction> predictions) {
        Map<Integer, Double> hourScores = new HashMap<>();
        for (EnergyPrediction pred : predictions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pred.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            
            double score = 0.0;
            switch (pred.getPredictedLevel()) {
                case HIGH:
                    score = 3.0 * pred.getConfidence();
                    break;
                case MEDIUM:
                    score = 2.0 * pred.getConfidence();
                    break;
                case LOW:
                    score = 1.0 * pred.getConfidence();
                    break;
            }
            
            hourScores.put(hour, hourScores.getOrDefault(hour, 0.0) + score);
        }
        
        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(hourScores.entrySet());
        sorted.sort((a, b) -> Double.compare(b.getValue(), a.getValue()));
        
        List<Integer> peakHours = new ArrayList<>();
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            peakHours.add(sorted.get(i).getKey());
        }
        
        return peakHours;
    }
    
    private List<Integer> findLowEnergyHours(List<EnergyPrediction> predictions) {
        Map<Integer, Double> hourScores = new HashMap<>();
        for (EnergyPrediction pred : predictions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pred.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            
            double score = 0.0;
            switch (pred.getPredictedLevel()) {
                case HIGH:
                    score = 3.0 * pred.getConfidence();
                    break;
                case MEDIUM:
                    score = 2.0 * pred.getConfidence();
                    break;
                case LOW:
                    score = 1.0 * pred.getConfidence();
                    break;
            }
            
            hourScores.put(hour, hourScores.getOrDefault(hour, 0.0) + score);
        }
        
        List<Map.Entry<Integer, Double>> sorted = new ArrayList<>(hourScores.entrySet());
        sorted.sort((a, b) -> Double.compare(a.getValue(), b.getValue()));
        
        List<Integer> lowHours = new ArrayList<>();
        for (int i = 0; i < Math.min(3, sorted.size()); i++) {
            lowHours.add(sorted.get(i).getKey());
        }
        
        return lowHours;
    }
    
    public interface ScheduleCallback {
        void onSuccess(String schedule);
        void onError(Exception error);
    }
}
