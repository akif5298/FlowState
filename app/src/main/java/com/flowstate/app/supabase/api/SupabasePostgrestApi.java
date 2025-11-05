package com.flowstate.app.supabase.api;

import retrofit2.Call;
import retrofit2.http.*;

import java.util.List;
import java.util.Map;

/**
 * Retrofit interface for Supabase PostgREST API (Database queries)
 */
public interface SupabasePostgrestApi {
    
    // Profiles
    @GET("/rest/v1/profiles")
    Call<List<Map<String, Object>>> getProfile(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("id") String userId
    );
    
    @POST("/rest/v1/profiles")
    Call<Void> insertProfile(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> profile
    );
    
    @PATCH("/rest/v1/profiles")
    Call<Void> updateProfile(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("id") String userId,
        @Body Map<String, Object> profile
    );
    
    // Heart Rate Readings
    @POST("/rest/v1/heart_rate_readings")
    Call<Void> insertHeartRateReading(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/heart_rate_readings")
    Call<List<Map<String, Object>>> getHeartRateReadings(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @QueryMap Map<String, String> queryParams
    );
    
    @POST("/rest/v1/heart_rate_readings")
    Call<Void> upsertHeartRateReading(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    // Sleep Sessions
    @POST("/rest/v1/sleep_sessions")
    Call<Void> insertSleepSession(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/sleep_sessions")
    Call<List<Map<String, Object>>> getSleepSessions(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("user_id") String userId,
        @Query("sleep_start") String sleepStartGte,
        @Query("order") String order
    );
    
    // Temperature Readings
    @POST("/rest/v1/temperature_readings")
    Call<Void> insertTemperatureReading(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/temperature_readings")
    Call<List<Map<String, Object>>> getTemperatureReadings(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("user_id") String userId,
        @Query("timestamp") String timestampGte,
        @Query("order") String order
    );
    
    // Typing Speed Tests
    @POST("/rest/v1/typing_speed_tests")
    Call<Void> insertTypingSpeedTest(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/typing_speed_tests")
    Call<List<Map<String, Object>>> getTypingSpeedTests(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @QueryMap Map<String, String> queryParams
    );
    
    // Reaction Time Tests
    @POST("/rest/v1/reaction_time_tests")
    Call<Void> insertReactionTimeTest(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/reaction_time_tests")
    Call<List<Map<String, Object>>> getReactionTimeTests(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @QueryMap Map<String, String> queryParams
    );
    
    // Energy Predictions
    @POST("/rest/v1/energy_predictions")
    Call<Void> insertEnergyPrediction(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/energy_predictions")
    Call<List<Map<String, Object>>> getEnergyPredictions(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @QueryMap Map<String, String> queryParams
    );
    
    // Productivity Suggestions
    @POST("/rest/v1/productivity_suggestions")
    Call<Void> insertProductivitySuggestion(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/productivity_suggestions")
    Call<List<Map<String, Object>>> getProductivitySuggestions(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("user_id") String userId,
        @Query("time_slot_start") String timeSlotStartGte,
        @Query("order") String order
    );
    
    @PATCH("/rest/v1/productivity_suggestions")
    Call<Void> updateProductivitySuggestion(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("id") String id,
        @Body Map<String, Object> data
    );
    
    // AI Schedules
    @POST("/rest/v1/ai_schedules")
    Call<Void> insertAISchedule(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/ai_schedules")
    Call<List<Map<String, Object>>> getAISchedules(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("user_id") String userId,
        @Query("schedule_date") String scheduleDate,
        @Query("order") String order
    );
    
    // Scheduled Tasks
    @POST("/rest/v1/scheduled_tasks")
    Call<Void> insertScheduledTask(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/scheduled_tasks")
    Call<List<Map<String, Object>>> getScheduledTasks(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("schedule_id") String scheduleId,
        @Query("order") String order
    );
    
    @PATCH("/rest/v1/scheduled_tasks")
    Call<Void> updateScheduledTask(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("id") String id,
        @Body Map<String, Object> data
    );
    
    // Weekly Insights
    @POST("/rest/v1/weekly_insights")
    Call<Void> insertWeeklyInsight(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/weekly_insights")
    Call<List<Map<String, Object>>> getWeeklyInsights(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("user_id") String userId,
        @Query("week_start_date") String weekStartDate,
        @Query("order") String order
    );
    
    // Daily Summaries
    @POST("/rest/v1/daily_summaries")
    Call<Void> insertDailySummary(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Header("Prefer") String prefer,
        @Body Map<String, Object> data
    );
    
    @GET("/rest/v1/daily_summaries")
    Call<List<Map<String, Object>>> getDailySummaries(
        @Header("Authorization") String authorization,
        @Header("apikey") String apikey,
        @Query("user_id") String userId,
        @Query("summary_date") String summaryDate,
        @Query("order") String order
    );
}

