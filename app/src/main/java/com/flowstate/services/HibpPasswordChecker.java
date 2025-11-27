package com.flowstate.services;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

/**
 * Service to check passwords against HaveIBeenPwned (HIBP) API
 * via a backend service endpoint.
 * 
 * Usage:
 *   HibpPasswordChecker checker = new HibpPasswordChecker("https://your-hibp-service.com");
 *   checker.checkPassword("user-password", new HibpPasswordChecker.Callback() {
 *       @Override
 *       public void onResult(boolean pwned, int count) {
 *           if (pwned) {
 *               // Show error: password has been compromised
 *           } else {
 *               // Proceed with signup
 *           }
 *       }
 *       
 *       @Override
 *       public void onError(Exception error) {
 *           // Handle error (network, service unavailable, etc.)
 *       }
 *   });
 */
public class HibpPasswordChecker {
    
    private static final String TAG = "HibpPasswordChecker";
    private final String baseUrl;
    private final Handler mainHandler;
    private final Gson gson;
    
    // Default to a placeholder - should be configured via Config or environment
    private static final String DEFAULT_HIBP_SERVICE_URL = "https://your-hibp-service.com";
    
    public interface Callback {
        void onResult(boolean pwned, int count);
        void onError(Exception error);
    }
    
    public static class CheckRequest {
        @SerializedName("password")
        public String password;
        
        @SerializedName("sha1")
        public String sha1;
        
        public CheckRequest(String password) {
            this.password = password;
        }
        
        public CheckRequest withSha1(String sha1) {
            this.sha1 = sha1;
            return this;
        }
    }
    
    public static class CheckResponse {
        @SerializedName("pwned")
        public boolean pwned;
        
        @SerializedName("count")
        public int count;
    }
    
    public HibpPasswordChecker(String baseUrl) {
        this.baseUrl = baseUrl != null && !baseUrl.isEmpty() ? baseUrl : DEFAULT_HIBP_SERVICE_URL;
        this.mainHandler = new Handler(Looper.getMainLooper());
        this.gson = new Gson();
    }
    
    /**
     * Check if a password has been pwned
     * @param password Plain-text password to check
     * @param callback Callback for result or error
     */
    public void checkPassword(String password, Callback callback) {
        if (password == null || password.isEmpty()) {
            mainHandler.post(() -> callback.onError(new IllegalArgumentException("Password cannot be null or empty")));
            return;
        }
        
        // Run on background thread
        new Thread(() -> {
            try {
                CheckRequest request = new CheckRequest(password);
                CheckResponse response = performCheck(request);
                
                mainHandler.post(() -> callback.onResult(response.pwned, response.count));
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking password", e);
                mainHandler.post(() -> callback.onError(e));
            }
        }).start();
    }
    
    /**
     * Check if a SHA-1 hash has been pwned
     * @param sha1Hash Uppercase SHA-1 hash (40 hex characters)
     * @param callback Callback for result or error
     */
    public void checkSha1(String sha1Hash, Callback callback) {
        if (sha1Hash == null || sha1Hash.isEmpty() || !sha1Hash.matches("^[0-9A-F]{40}$")) {
            mainHandler.post(() -> callback.onError(new IllegalArgumentException("Invalid SHA-1 hash format")));
            return;
        }
        
        // Run on background thread
        new Thread(() -> {
            try {
                CheckRequest request = new CheckRequest(null).withSha1(sha1Hash);
                CheckResponse response = performCheck(request);
                
                mainHandler.post(() -> callback.onResult(response.pwned, response.count));
                
            } catch (Exception e) {
                Log.e(TAG, "Error checking SHA-1 hash", e);
                mainHandler.post(() -> callback.onError(e));
            }
        }).start();
    }
    
    private CheckResponse performCheck(CheckRequest request) throws Exception {
        String urlString = baseUrl + "/hibp/check";
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        
        try {
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Accept", "application/json");
            connection.setDoOutput(true);
            connection.setConnectTimeout(10000); // 10 seconds
            connection.setReadTimeout(10000); // 10 seconds
            
            // Write request body
            String jsonRequest = gson.toJson(request);
            try (OutputStream os = connection.getOutputStream()) {
                byte[] input = jsonRequest.getBytes(StandardCharsets.UTF_8);
                os.write(input, 0, input.length);
            }
            
            // Read response
            int responseCode = connection.getResponseCode();
            
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(
                        new InputStreamReader(connection.getInputStream(), StandardCharsets.UTF_8))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    CheckResponse checkResponse = gson.fromJson(response.toString(), CheckResponse.class);
                    return checkResponse;
                }
            } else if (responseCode == HttpURLConnection.HTTP_BAD_REQUEST) {
                throw new Exception("Invalid request: " + responseCode);
            } else if (responseCode == HttpURLConnection.HTTP_BAD_GATEWAY) {
                throw new Exception("HIBP service unavailable: " + responseCode);
            } else {
                throw new Exception("Unexpected response code: " + responseCode);
            }
            
        } finally {
            connection.disconnect();
        }
    }
}

