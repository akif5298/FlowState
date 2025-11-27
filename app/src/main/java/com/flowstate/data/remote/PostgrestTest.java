package com.flowstate.data.remote;

import android.content.Context;
import android.util.Log;
import com.flowstate.core.SecureStore;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Simple test utility to verify PostgREST API works
 * 
 * Usage:
 *   PostgrestTest.testGet(context, "profiles", callback);
 */
public class PostgrestTest {
    
    private static final String TAG = "PostgrestTest";
    
    /**
     * Test GET request to a PostgREST table
     * 
     * @param context Application context
     * @param table Table name (e.g., "profiles")
     * @param callback Callback to handle result
     */
    public static void testGet(Context context, String table, TestCallback callback) {
        // Check if authenticated
        SecureStore secureStore = SecureStore.getInstance(context);
        if (!secureStore.hasAccessToken()) {
            Log.e(TAG, "No access token available. Please authenticate first.");
            if (callback != null) {
                callback.onError("No access token. Please authenticate first.");
            }
            return;
        }
        
        // Get Supabase client
        SupabaseClient client = SupabaseClient.getInstance(context);
        PostgrestApi api = client.getPostgrestApi();
        
        // Create empty query params (or add filters)
        Map<String, String> queryParams = new HashMap<>();
        
        // Make GET request
        Call<List<Map<String, Object>>> call = api.get(table, queryParams);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, 
                                 Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    List<Map<String, Object>> data = response.body();
                    Log.d(TAG, "GET " + table + " successful. Status: " + response.code());
                    Log.d(TAG, "Received " + (data != null ? data.size() : 0) + " records");
                    if (callback != null) {
                        callback.onSuccess(response.code(), data);
                    }
                } else {
                    String errorMsg = "GET " + table + " failed. Status: " + response.code();
                    Log.e(TAG, errorMsg);
                    try {
                        if (response.errorBody() != null) {
                            String errorBody = response.errorBody().string();
                            Log.e(TAG, "Error body: " + errorBody);
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error reading error body", e);
                    }
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e(TAG, "GET " + table + " failed", t);
                if (callback != null) {
                    callback.onError(errorMsg);
                }
            }
        });
    }
    
    /**
     * Test GET request with query parameters
     * 
     * Example:
     *   Map<String, String> params = new HashMap<>();
     *   params.put("id", "eq." + userId);
     *   PostgrestTest.testGetWithParams(context, "profiles", params, callback);
     */
    public static void testGetWithParams(Context context, String table, 
                                        Map<String, String> queryParams, 
                                        TestCallback callback) {
        SecureStore secureStore = SecureStore.getInstance(context);
        if (!secureStore.hasAccessToken()) {
            Log.e(TAG, "No access token available. Please authenticate first.");
            if (callback != null) {
                callback.onError("No access token. Please authenticate first.");
            }
            return;
        }
        
        SupabaseClient client = SupabaseClient.getInstance(context);
        PostgrestApi api = client.getPostgrestApi();
        
        Call<List<Map<String, Object>>> call = api.get(table, queryParams);
        call.enqueue(new Callback<List<Map<String, Object>>>() {
            @Override
            public void onResponse(Call<List<Map<String, Object>>> call, 
                                 Response<List<Map<String, Object>>> response) {
                if (response.isSuccessful()) {
                    List<Map<String, Object>> data = response.body();
                    Log.d(TAG, "GET " + table + " successful. Status: " + response.code());
                    if (callback != null) {
                        callback.onSuccess(response.code(), data);
                    }
                } else {
                    String errorMsg = "GET " + table + " failed. Status: " + response.code();
                    Log.e(TAG, errorMsg);
                    if (callback != null) {
                        callback.onError(errorMsg);
                    }
                }
            }
            
            @Override
            public void onFailure(Call<List<Map<String, Object>>> call, Throwable t) {
                String errorMsg = "Network error: " + t.getMessage();
                Log.e(TAG, "GET " + table + " failed", t);
                if (callback != null) {
                    callback.onError(errorMsg);
                }
            }
        });
    }
    
    /**
     * Callback interface for test results
     */
    public interface TestCallback {
        void onSuccess(int statusCode, List<Map<String, Object>> data);
        void onError(String error);
    }
}

