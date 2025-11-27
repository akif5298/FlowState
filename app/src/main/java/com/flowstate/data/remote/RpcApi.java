package com.flowstate.data.remote;

import com.google.gson.JsonObject;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.POST;

/**
 * RPC API interface for Supabase stored procedures/functions
 */
public interface RpcApi {
    
    /**
     * Upsert heart rate data
     */
    @POST("upsert_heart_rate")
    Call<Void> upsertHeartRate(@Body JsonObject body);
    
    /**
     * Upsert sleep sessions data
     */
    @POST("upsert_sleep_sessions")
    Call<Void> upsertSleep(@Body JsonObject body);
    
    /**
     * Upsert typing speed test data
     */
    @POST("upsert_typing_tests")
    Call<Void> upsertTyping(@Body JsonObject body);
    
    /**
     * Upsert reaction time test data
     */
    @POST("upsert_reaction_tests")
    Call<Void> upsertReaction(@Body JsonObject body);
    
    /**
     * Upsert energy predictions data
     */
    @POST("upsert_energy_predictions")
    Call<Void> upsertPredictions(@Body JsonObject body);
}

