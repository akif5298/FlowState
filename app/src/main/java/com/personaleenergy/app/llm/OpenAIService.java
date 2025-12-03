package com.personaleenergy.app.llm;

import android.util.Log;
import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.data.models.EnergyLevel;
import com.flowstate.core.Config;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import okhttp3.*;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * OpenAI API Service for generating personalized schedules
 * Uses ChatGPT (GPT-4) to create tailored daily schedules based on energy predictions
 */
public class OpenAIService {
    
    private static final String TAG = "OpenAIService";
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";
    private static final String MODEL = "gpt-4o-mini"; // Fast and cost-effective
    
    private final OkHttpClient httpClient;
    private final Gson gson;
    private final String apiKey;
    
    public OpenAIService() {
        this.apiKey = Config.OPENAI_API_KEY;
        this.gson = new GsonBuilder().setPrettyPrinting().create();
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build();
        
        // Debug logging for API key
        if (apiKey == null || apiKey.isEmpty()) {
            Log.w(TAG, "⚠️ OpenAI API key is NULL or EMPTY - API calls will fail");
            Log.w(TAG, "⚠️ Make sure OPENAI_API_KEY is set in local.properties and rebuild the app");
        } else {
            // Log first 10 and last 4 characters for debugging (don't log full key for security)
            String maskedKey = apiKey.length() > 14 
                ? apiKey.substring(0, 10) + "..." + apiKey.substring(apiKey.length() - 4)
                : "***";
            Log.d(TAG, "✅ OpenAI API key loaded: " + maskedKey);
            
            // Validate API key format (should start with sk-)
            if (!apiKey.startsWith("sk-")) {
                Log.w(TAG, "⚠️ WARNING: API key doesn't start with 'sk-' - may be invalid");
            } else {
                Log.d(TAG, "✅ API key format looks valid (starts with 'sk-')");
            }
        }
    }
    
    /**
     * Generate personalized schedule using OpenAI ChatGPT
     */
    public void generateSchedule(
            List<EnergyPrediction> energyPredictions,
            List<String> userTasks,
            List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> tasksWithEnergy,
            List<String> existingEvents,
            ScheduleCallback callback) {
        
        if (apiKey == null || apiKey.isEmpty()) {
            Log.e(TAG, "OpenAI API key not configured");
            callback.onError(new Exception("OpenAI API key not configured. Please add OPENAI_API_KEY to local.properties"));
            return;
        }
        
        // Build prompt for ChatGPT
        String prompt = buildSchedulePrompt(energyPredictions, userTasks, tasksWithEnergy, existingEvents);
        
        // Make API call
        makeOpenAIRequest(prompt, callback);
    }
    
    /**
     * Build comprehensive prompt for ChatGPT
     */
    private String buildSchedulePrompt(
            List<EnergyPrediction> energyPredictions,
            List<String> userTasks,
            List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> tasksWithEnergy,
            List<String> existingEvents) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI productivity coach helping create an optimized daily schedule.\n\n");
        
        // Add sleep pattern information if available
        String sleepInfo = extractSleepPatternInfo();
        if (sleepInfo != null && !sleepInfo.isEmpty()) {
            prompt.append("SLEEP PATTERNS:\n");
            prompt.append(sleepInfo);
            prompt.append("\nIMPORTANT: Align the schedule start time with the user's typical wake-up time. ");
            prompt.append("Do not schedule tasks before the user usually wakes up.\n\n");
        }
        
        // Energy predictions analysis
        prompt.append("ENERGY PREDICTIONS FOR TODAY:\n");
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        
        Map<Integer, List<EnergyPrediction>> byHour = new HashMap<>();
        for (EnergyPrediction pred : energyPredictions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pred.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            byHour.computeIfAbsent(hour, k -> new ArrayList<>()).add(pred);
        }
        
        for (int hour = 0; hour < 24; hour++) {
            if (byHour.containsKey(hour)) {
                List<EnergyPrediction> preds = byHour.get(hour);
                EnergyPrediction latest = preds.get(preds.size() - 1);
                String period = hour < 12 ? "AM" : "PM";
                int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
                String timeStr = String.format("%d:00 %s", displayHour, period);
                prompt.append(String.format("- %s: %s energy (confidence: %.0f%%)\n", 
                    timeStr, latest.getPredictedLevel(), latest.getConfidence() * 100));
            }
        }
        
        // Find peak energy hours
        List<Integer> peakHours = findPeakEnergyHours(energyPredictions);
        List<Integer> lowHours = findLowEnergyHours(energyPredictions);
        
        prompt.append("\nPEAK ENERGY HOURS: ");
        for (int i = 0; i < peakHours.size(); i++) {
            int hour = peakHours.get(i);
            String period = hour < 12 ? "AM" : "PM";
            int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            prompt.append(String.format("%d:00 %s", displayHour, period));
            if (i < peakHours.size() - 1) prompt.append(", ");
        }
        prompt.append("\n");
        
        prompt.append("LOW ENERGY HOURS: ");
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
        
        // Instructions
        prompt.append("INSTRUCTIONS:\n");
        prompt.append("1. Create a detailed hourly schedule for today\n");
        prompt.append("2. Schedule high-energy tasks (coding, writing, creative work, problem-solving) during PEAK ENERGY HOURS\n");
        prompt.append("3. Schedule low-energy tasks (emails, meetings, reading, organizing) during LOW ENERGY HOURS\n");
        prompt.append("4. Include breaks and meal times\n");
        prompt.append("5. CRITICAL: Existing calendar events are IMMUTABLE - NEVER schedule tasks at the same time as existing events. " +
                     "Existing events cannot be moved or changed. Work around them.\n");
        prompt.append("6. If a task would conflict with an existing event, schedule it at a different time\n");
        prompt.append("7. Provide reasoning for each scheduled task based on energy levels\n");
        prompt.append("8. Format the schedule clearly with times and activities\n");
        prompt.append("9. Include motivational tips based on the energy predictions\n\n");
        
        prompt.append("Generate the schedule now (remember: existing events are immutable and cannot be scheduled over):");
        
        return prompt.toString();
    }
    
    /**
     * Extract sleep pattern information from biometric data
     * This helps the AI know when users typically wake up
     */
    private String extractSleepPatternInfo() {
        // This will be called from SmartCalendarAI with sleep data
        // For now, return null - will be enhanced when called with sleep data
        return null;
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
            Log.e(TAG, "OpenAI API key not configured");
            callback.onError(new Exception("OpenAI API key not configured"));
            return;
        }
        
        // Build prompt with sleep info
        String prompt = buildSchedulePromptWithSleep(energyPredictions, userTasks, tasksWithEnergy, existingEvents, sleepPatternInfo);
        
        // Make API call
        makeOpenAIRequest(prompt, callback);
    }
    
    /**
     * Build prompt with sleep pattern information
     */
    private String buildSchedulePromptWithSleep(
            List<EnergyPrediction> energyPredictions,
            List<String> userTasks,
            List<com.personaleenergy.app.ui.schedule.TaskWithEnergy> tasksWithEnergy,
            List<String> existingEvents,
            String sleepPatternInfo) {
        
        StringBuilder prompt = new StringBuilder();
        prompt.append("You are an AI productivity coach helping create an optimized daily schedule.\n\n");
        
        // Add sleep pattern information
        if (sleepPatternInfo != null && !sleepPatternInfo.isEmpty()) {
            prompt.append("SLEEP PATTERNS:\n");
            prompt.append(sleepPatternInfo);
            prompt.append("\nIMPORTANT: Align the schedule start time with the user's typical wake-up time. ");
            prompt.append("Do not schedule tasks before the user usually wakes up.\n\n");
        }
        
        // Energy predictions analysis
        prompt.append("ENERGY PREDICTIONS FOR TODAY:\n");
        SimpleDateFormat timeFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
        
        Map<Integer, List<EnergyPrediction>> byHour = new HashMap<>();
        for (EnergyPrediction pred : energyPredictions) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(pred.getTimestamp());
            int hour = cal.get(Calendar.HOUR_OF_DAY);
            byHour.computeIfAbsent(hour, k -> new ArrayList<>()).add(pred);
        }
        
        for (int hour = 0; hour < 24; hour++) {
            if (byHour.containsKey(hour)) {
                List<EnergyPrediction> preds = byHour.get(hour);
                EnergyPrediction latest = preds.get(preds.size() - 1);
                String period = hour < 12 ? "AM" : "PM";
                int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
                String timeStr = String.format("%d:00 %s", displayHour, period);
                prompt.append(String.format("- %s: %s energy (confidence: %.0f%%)\n", 
                    timeStr, latest.getPredictedLevel(), latest.getConfidence() * 100));
            }
        }
        
        // Find peak energy hours
        List<Integer> peakHours = findPeakEnergyHours(energyPredictions);
        List<Integer> lowHours = findLowEnergyHours(energyPredictions);
        
        prompt.append("\nPEAK ENERGY HOURS: ");
        for (int i = 0; i < peakHours.size(); i++) {
            int hour = peakHours.get(i);
            String period = hour < 12 ? "AM" : "PM";
            int displayHour = hour > 12 ? hour - 12 : (hour == 0 ? 12 : hour);
            prompt.append(String.format("%d:00 %s", displayHour, period));
            if (i < peakHours.size() - 1) prompt.append(", ");
        }
        prompt.append("\n");
        
        prompt.append("LOW ENERGY HOURS: ");
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
        
        // Instructions
        prompt.append("INSTRUCTIONS:\n");
        prompt.append("1. Create a detailed hourly schedule for today\n");
        prompt.append("2. Schedule high-energy tasks (coding, writing, creative work, problem-solving) during PEAK ENERGY HOURS\n");
        prompt.append("3. Schedule low-energy tasks (emails, meetings, reading, organizing) during LOW ENERGY HOURS\n");
        prompt.append("4. Include breaks and meal times\n");
        prompt.append("5. CRITICAL: Existing calendar events are IMMUTABLE - NEVER schedule tasks at the same time as existing events. " +
                     "Existing events cannot be moved or changed. Work around them.\n");
        prompt.append("6. If a task would conflict with an existing event, schedule it at a different time\n");
        prompt.append("7. Provide reasoning for each scheduled task based on energy levels\n");
        prompt.append("8. Format the schedule clearly with times and activities\n");
        prompt.append("9. Include motivational tips based on the energy predictions\n\n");
        
        prompt.append("Generate the schedule now (remember: existing events are immutable and cannot be scheduled over):");
        
        return prompt.toString();
    }
    
    /**
     * Make API request to OpenAI
     */
    private void makeOpenAIRequest(String prompt, ScheduleCallback callback) {
        try {
            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", MODEL);
            
            JsonArray messages = new JsonArray();
            JsonObject systemMessage = new JsonObject();
            systemMessage.addProperty("role", "system");
            systemMessage.addProperty("content", "You are a helpful AI productivity coach that creates optimized daily schedules based on energy predictions. Always provide clear, actionable schedules with specific times and activities.");
            messages.add(systemMessage);
            
            JsonObject userMessage = new JsonObject();
            userMessage.addProperty("role", "user");
            userMessage.addProperty("content", prompt);
            messages.add(userMessage);
            
            requestBody.add("messages", messages);
            requestBody.addProperty("temperature", 0.7);
            requestBody.addProperty("max_tokens", 2000);
            
            RequestBody body = RequestBody.create(
                requestBody.toString(),
                MediaType.parse("application/json")
            );
            
            Request request = new Request.Builder()
                .url(OPENAI_API_URL)
                .addHeader("Authorization", "Bearer " + apiKey)
                .addHeader("Content-Type", "application/json")
                .post(body)
                .build();
            
            Log.d(TAG, "Making OpenAI API request to: " + OPENAI_API_URL);
            Log.d(TAG, "Using model: " + MODEL);
            Log.d(TAG, "Request body size: " + requestBody.toString().length() + " characters");
            
            httpClient.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(okhttp3.Call call, IOException e) {
                    Log.e(TAG, "OpenAI API request failed", e);
                    callback.onError(e);
                }
                
                @Override
                public void onResponse(okhttp3.Call call, Response response) throws IOException {
                    Log.d(TAG, "OpenAI API response code: " + response.code());
                    
                    if (!response.isSuccessful()) {
                        String errorBody = response.body() != null ? response.body().string() : "Unknown error";
                        Log.e(TAG, "❌ OpenAI API error: " + response.code() + " - " + errorBody);
                        
                        // Check for specific error types
                        if (response.code() == 401) {
                            Log.e(TAG, "⚠️ Authentication failed - API key may be invalid or expired");
                        } else if (response.code() == 429) {
                            Log.e(TAG, "⚠️ Rate limit exceeded - check your OpenAI account quota");
                        }
                        
                        callback.onError(new Exception("OpenAI API error: " + response.code() + " - " + errorBody));
                        return;
                    }
                    
                    String responseBody = response.body().string();
                    Log.d(TAG, "✅ OpenAI API response received successfully");
                    Log.d(TAG, "Response body length: " + responseBody.length() + " characters");
                    
                    try {
                        JsonObject jsonResponse = gson.fromJson(responseBody, JsonObject.class);
                        JsonArray choices = jsonResponse.getAsJsonArray("choices");
                        if (choices != null && choices.size() > 0) {
                            JsonObject choice = choices.get(0).getAsJsonObject();
                            JsonObject message = choice.getAsJsonObject("message");
                            String content = message.get("content").getAsString();
                            
                            Log.d(TAG, "Schedule generated successfully");
                            callback.onSuccess(content);
                        } else {
                            callback.onError(new Exception("No response from OpenAI"));
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error parsing OpenAI response", e);
                        callback.onError(e);
                    }
                }
            });
            
        } catch (Exception e) {
            Log.e(TAG, "Error creating OpenAI request", e);
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

