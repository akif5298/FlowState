package com.flowstate.data.remote;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.Path;
import retrofit2.http.QueryMap;

import java.util.List;
import java.util.Map;

/**
 * PostgREST API interface
 * 
 * Generic GET by table using @Path and @QueryMap for flexible querying
 */
public interface PostgrestApi {
    
    /**
     * Generic GET by table name
     * 
     * @param table Table name (e.g., "profiles", "heart_rate_readings")
     * @param queryParams Query parameters (e.g., id=eq.uuid, order=timestamp.desc)
     * @return List of maps representing table rows
     * 
     * Example usage:
     *   Map<String, String> params = new HashMap<>();
     *   params.put("id", "eq." + userId);
     *   postgrestApi.get("profiles", params);
     */
    @GET("{table}")
    Call<List<Map<String, Object>>> get(
        @Path("table") String table,
        @QueryMap Map<String, String> queryParams
    );
    
    /**
     * Generic GET by table name without query parameters
     * 
     * @param table Table name
     * @return List of maps representing table rows
     */
    @GET("{table}")
    Call<List<Map<String, Object>>> get(@Path("table") String table);
    
    /**
     * Generic DELETE by table name with query parameters
     * 
     * @param table Table name
     * @param queryParams Query parameters (e.g., user_id=eq.uuid)
     * @return Void response
     * 
     * Example usage:
     *   Map<String, String> params = new HashMap<>();
     *   params.put("user_id", "eq." + userId);
     *   postgrestApi.delete("profiles", params);
     */
    @DELETE("{table}")
    Call<Void> delete(
        @Path("table") String table,
        @QueryMap Map<String, String> queryParams
    );
    
    /**
     * Generic POST to insert data into a table
     * 
     * @param table Table name (e.g., "heart_rate_readings", "sleep_sessions")
     * @param data Single record as Map
     * @return Void response
     * 
     * Example usage:
     *   Map<String, Object> data = new HashMap<>();
     *   data.put("user_id", userId);
     *   data.put("timestamp", timestamp);
     *   data.put("heart_rate_bpm", bpm);
     *   postgrestApi.post("heart_rate_readings", data);
     */
    @POST("{table}")
    Call<Void> post(
        @Path("table") String table,
        @Body Map<String, Object> data
    );
    
    /**
     * Generic POST to insert multiple records into a table (batch insert)
     * 
     * @param table Table name
     * @param data List of records as Maps
     * @return Void response
     * 
     * Example usage:
     *   List<Map<String, Object>> records = new ArrayList<>();
     *   // Add records to list
     *   postgrestApi.postBatch("heart_rate_readings", records);
     */
    @POST("{table}")
    Call<Void> postBatch(
        @Path("table") String table,
        @Body List<Map<String, Object>> data
    );
}

