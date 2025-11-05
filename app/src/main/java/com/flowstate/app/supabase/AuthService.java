package com.flowstate.app.supabase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import com.flowstate.app.supabase.api.SupabaseAuthApi;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;

/**
 * Service for handling authentication with Supabase (Java implementation)
 */
public class AuthService {
    
    private SupabaseClient supabaseClient;
    private SupabaseAuthApi authApi;
    private Handler mainHandler;
    
    public AuthService(Context context) {
        this.supabaseClient = SupabaseClient.getInstance(context);
        this.authApi = supabaseClient.getAuthApi();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * Sign up a new user
     */
    public void signUp(String email, String password, String username, String firstName, String lastName, AuthCallback callback) {
        SupabaseAuthApi.SignUpRequest request = new SupabaseAuthApi.SignUpRequest(email, password, username);
        
        authApi.signUp(request).enqueue(new Callback<SupabaseAuthApi.AuthResponse>() {
            @Override
            public void onResponse(Call<SupabaseAuthApi.AuthResponse> call, 
                                 Response<SupabaseAuthApi.AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SupabaseAuthApi.AuthResponse authResponse = response.body();
                    supabaseClient.setAccessToken(authResponse.access_token);
                    supabaseClient.setRefreshToken(authResponse.refresh_token);
                    if (authResponse.user != null) {
                        supabaseClient.setUserId(authResponse.user.id);
                        // Create profile if username is provided
                        // Pass the access token directly to avoid timing issues with SharedPreferences
                        if (username != null && authResponse.user.id != null && authResponse.access_token != null) {
                            android.util.Log.d("AuthService", "User created successfully. User ID: " + authResponse.user.id);
                            android.util.Log.d("AuthService", "Token available: " + (authResponse.access_token != null && !authResponse.access_token.isEmpty()));
                            createProfile(authResponse.user.id, username, email, firstName, lastName, authResponse.access_token, callback);
                        } else {
                            android.util.Log.w("AuthService", "Missing required fields for profile creation. Username: " + username + ", UserId: " + authResponse.user.id);
                            mainHandler.post(() -> callback.onSuccess(authResponse.user));
                        }
                    } else {
                        mainHandler.post(() -> callback.onSuccess(null));
                    }
                } else {
                    String errorMessage = parseErrorMessage(response, "Sign up failed");
                    mainHandler.post(() -> callback.onError(new Exception(errorMessage)));
                }
            }
            
            @Override
            public void onFailure(Call<SupabaseAuthApi.AuthResponse> call, Throwable t) {
                String errorMessage = parseNetworkError(t);
                mainHandler.post(() -> callback.onError(new Exception(errorMessage)));
            }
        });
    }
    
    /**
     * Sign in an existing user
     */
    public void signIn(String email, String password, AuthCallback callback) {
        SupabaseAuthApi.SignInRequest request = new SupabaseAuthApi.SignInRequest(email, password);
        
        authApi.signIn(request).enqueue(new Callback<SupabaseAuthApi.AuthResponse>() {
            @Override
            public void onResponse(Call<SupabaseAuthApi.AuthResponse> call, 
                                 Response<SupabaseAuthApi.AuthResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    SupabaseAuthApi.AuthResponse authResponse = response.body();
                    supabaseClient.setAccessToken(authResponse.access_token);
                    supabaseClient.setRefreshToken(authResponse.refresh_token);
                    if (authResponse.user != null) {
                        supabaseClient.setUserId(authResponse.user.id);
                    }
                    mainHandler.post(() -> callback.onSuccess(authResponse.user));
                } else {
                    String errorMessage = parseErrorMessage(response, "Sign in failed");
                    mainHandler.post(() -> callback.onError(new Exception(errorMessage)));
                }
            }
            
            @Override
            public void onFailure(Call<SupabaseAuthApi.AuthResponse> call, Throwable t) {
                String errorMessage = parseNetworkError(t);
                mainHandler.post(() -> callback.onError(new Exception(errorMessage)));
            }
        });
    }
    
    /**
     * Sign out the current user
     */
    public void signOut(AuthCallback callback) {
        String accessToken = supabaseClient.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            supabaseClient.clearAuth();
            mainHandler.post(() -> callback.onSuccess(null));
            return;
        }
        
        authApi.signOut("Bearer " + accessToken).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                supabaseClient.clearAuth();
                mainHandler.post(() -> callback.onSuccess(null));
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                // Clear auth even if API call fails
                supabaseClient.clearAuth();
                mainHandler.post(() -> callback.onError(t));
            }
        });
    }
    
    /**
     * Get current authenticated user
     */
    public void getCurrentUser(AuthCallback callback) {
        String accessToken = supabaseClient.getAccessToken();
        if (accessToken == null || accessToken.isEmpty()) {
            mainHandler.post(() -> callback.onSuccess(null));
            return;
        }
        
        authApi.getCurrentUser("Bearer " + accessToken).enqueue(new Callback<SupabaseAuthApi.UserResponse>() {
            @Override
            public void onResponse(Call<SupabaseAuthApi.UserResponse> call, 
                                 Response<SupabaseAuthApi.UserResponse> response) {
                if (response.isSuccessful() && response.body() != null) {
                    mainHandler.post(() -> callback.onSuccess(response.body()));
                } else {
                    mainHandler.post(() -> callback.onSuccess(null));
                }
            }
            
            @Override
            public void onFailure(Call<SupabaseAuthApi.UserResponse> call, Throwable t) {
                mainHandler.post(() -> callback.onSuccess(null));
            }
        });
    }
    
    /**
     * Reset password for a user
     */
    public void resetPassword(String email, AuthCallback callback) {
        SupabaseAuthApi.PasswordResetRequest request = new SupabaseAuthApi.PasswordResetRequest(email);
        
        authApi.resetPassword(request).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    mainHandler.post(() -> callback.onSuccess(null));
                } else {
                    String errorMessage = parseErrorMessage(response, "Failed to send reset email");
                    mainHandler.post(() -> callback.onError(new Exception(errorMessage)));
                }
            }
            
            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                String errorMessage = parseNetworkError(t);
                mainHandler.post(() -> callback.onError(new Exception(errorMessage)));
            }
        });
    }
    
    /**
     * Check if user is authenticated
     */
    public boolean isAuthenticated() {
        return supabaseClient.isAuthenticated();
    }
    
    /**
     * Create or update user profile in Supabase profiles table
     */
    private void createProfile(String userId, String username, String email, String firstName, String lastName, String accessToken, AuthCallback callback) {
        // Get timezone (default to UTC)
        java.util.TimeZone timeZone = java.util.TimeZone.getDefault();
        String timezone = timeZone.getID();
        
        // Concatenate first and last name
        String fullName = (firstName != null ? firstName : "") + 
                         (firstName != null && lastName != null ? " " : "") + 
                         (lastName != null ? lastName : "");
        fullName = fullName.trim();
        
        // Create profile data map
        java.util.Map<String, Object> profileData = new java.util.HashMap<>();
        profileData.put("id", userId);
        profileData.put("username", username);
        profileData.put("email", email);
        profileData.put("full_name", fullName.isEmpty() ? null : fullName);
        profileData.put("timezone", timezone);
        
        // Get PostgREST API
        com.flowstate.app.supabase.api.SupabasePostgrestApi postgrestApi = supabaseClient.getPostgrestApi();
        String authorization = "Bearer " + accessToken;
        String apikey = supabaseClient.getSupabaseAnonKey();
        
        android.util.Log.d("AuthService", "Creating profile for user: " + userId);
        android.util.Log.d("AuthService", "Profile data: " + profileData.toString());
        
        // Insert profile into Supabase
        postgrestApi.insertProfile(authorization, apikey, "return=representation", profileData)
                .enqueue(new retrofit2.Callback<Void>() {
                    @Override
                    public void onResponse(retrofit2.Call<Void> call, retrofit2.Response<Void> response) {
                        if (response.isSuccessful()) {
                            android.util.Log.d("AuthService", "Profile created successfully for user: " + userId);
                            // Call the original callback with success
                            mainHandler.post(() -> {
                                // Get user from stored data
                                com.flowstate.app.supabase.api.SupabaseAuthApi.UserResponse user = new com.flowstate.app.supabase.api.SupabaseAuthApi.UserResponse();
                                user.id = userId;
                                user.email = email;
                                callback.onSuccess(user);
                            });
                        } else {
                            // Profile creation failed, but auth succeeded
                            android.util.Log.e("AuthService", "Failed to create profile. HTTP Code: " + response.code());
                            String errorMsg = "Account created but profile setup failed. Please update your profile later.";
                            try {
                                if (response.errorBody() != null) {
                                    String errorBody = response.errorBody().string();
                                    android.util.Log.e("AuthService", "Profile error response: " + errorBody);
                                    errorMsg += " Error: " + errorBody;
                                }
                            } catch (IOException e) {
                                android.util.Log.e("AuthService", "Error reading error body: " + e.getMessage());
                                e.printStackTrace();
                            }
                            // Still call success since user was created
                            mainHandler.post(() -> {
                                com.flowstate.app.supabase.api.SupabaseAuthApi.UserResponse user = new com.flowstate.app.supabase.api.SupabaseAuthApi.UserResponse();
                                user.id = userId;
                                user.email = email;
                                callback.onSuccess(user);
                            });
                        }
                    }
                    
                    @Override
                    public void onFailure(retrofit2.Call<Void> call, Throwable t) {
                        // Profile creation failed, but auth succeeded
                        android.util.Log.e("AuthService", "Profile creation network error: " + t.getMessage());
                        if (t.getCause() != null) {
                            android.util.Log.e("AuthService", "Cause: " + t.getCause().getMessage());
                        }
                        t.printStackTrace();
                        // Still call success since user was created
                        mainHandler.post(() -> {
                            com.flowstate.app.supabase.api.SupabaseAuthApi.UserResponse user = new com.flowstate.app.supabase.api.SupabaseAuthApi.UserResponse();
                            user.id = userId;
                            user.email = email;
                            callback.onSuccess(user);
                        });
                    }
                });
    }
    
    /**
     * Parse error message from Supabase response and convert to plain English
     */
    private String parseErrorMessage(Response<?> response, String defaultMessage) {
        try {
            if (response.errorBody() != null) {
                String errorBody = response.errorBody().string();
                if (errorBody != null && !errorBody.isEmpty()) {
                    // Try to parse as JSON
                    try {
                        JsonObject errorJson = JsonParser.parseString(errorBody).getAsJsonObject();
                        
                        // Check for common error fields
                        if (errorJson.has("error_description")) {
                            String errorDesc = errorJson.get("error_description").getAsString();
                            return convertToPlainEnglish(errorDesc);
                        }
                        
                        if (errorJson.has("message")) {
                            String message = errorJson.get("message").getAsString();
                            return convertToPlainEnglish(message);
                        }
                        
                        if (errorJson.has("error")) {
                            String error = errorJson.get("error").getAsString();
                            return convertToPlainEnglish(error);
                        }
                        
                        // If we have a msg field
                        if (errorJson.has("msg")) {
                            String msg = errorJson.get("msg").getAsString();
                            return convertToPlainEnglish(msg);
                        }
                    } catch (Exception e) {
                        // Not JSON, try to extract useful info from plain text
                        return convertToPlainEnglish(errorBody);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        
        // Check HTTP status codes for common issues
        int code = response.code();
        if (code == 400) {
            return "Invalid email or password. Please check your credentials.";
        } else if (code == 401) {
            return "Invalid email or password. Please try again.";
        } else if (code == 403) {
            return "Access denied. Please contact support.";
        } else if (code == 404) {
            return "Service not found. Please try again later.";
        } else if (code == 422) {
            return "Invalid information provided. Please check your input.";
        } else if (code == 429) {
            return "Too many attempts. Please wait a moment and try again.";
        } else if (code >= 500) {
            return "Server error. Please try again later.";
        }
        
        return defaultMessage;
    }
    
    /**
     * Convert technical error messages to plain English
     */
    private String convertToPlainEnglish(String error) {
        if (error == null) {
            return "An error occurred. Please try again.";
        }
        
        String lowerError = error.toLowerCase();
        
        // Common Supabase error patterns
        if (lowerError.contains("invalid login credentials") || 
            lowerError.contains("invalid_grant") ||
            lowerError.contains("invalid credentials")) {
            return "Invalid email or password. Please check your credentials and try again.";
        }
        
        if (lowerError.contains("user already registered") || 
            lowerError.contains("already registered") ||
            lowerError.contains("email already exists")) {
            return "This email is already registered. Please sign in or use a different email.";
        }
        
        if (lowerError.contains("invalid email") || 
            lowerError.contains("email format")) {
            return "Please enter a valid email address.";
        }
        
        if (lowerError.contains("password") && lowerError.contains("weak")) {
            return "Password is too weak. Please use a stronger password.";
        }
        
        if (lowerError.contains("password") && lowerError.contains("short")) {
            return "Password is too short. Please use at least 6 characters.";
        }
        
        if (lowerError.contains("network") || 
            lowerError.contains("connection") ||
            lowerError.contains("timeout")) {
            return "Connection problem. Please check your internet and try again.";
        }
        
        if (lowerError.contains("email not confirmed") ||
            lowerError.contains("email confirmation")) {
            return "Please check your email and confirm your account before signing in.";
        }
        
        // If we can't match it, return a sanitized version
        // Remove technical details but keep the core message
        String sanitized = error.replaceAll("\\{[^}]*\\}", "")
                                 .replaceAll("\\[.*?\\]", "")
                                 .trim();
        
        if (sanitized.length() > 100) {
            sanitized = sanitized.substring(0, 100) + "...";
        }
        
        return sanitized.isEmpty() ? "An error occurred. Please try again." : sanitized;
    }
    
    /**
     * Parse network errors and convert to plain English
     */
    private String parseNetworkError(Throwable t) {
        if (t == null) {
            return "Network error. Please check your connection and try again.";
        }
        
        String message = t.getMessage();
        if (message == null) {
            return "Network error. Please check your connection and try again.";
        }
        
        String lowerMessage = message.toLowerCase();
        
        if (lowerMessage.contains("failed to connect") ||
            lowerMessage.contains("connection refused") ||
            lowerMessage.contains("unable to resolve host")) {
            return "Cannot connect to server. Please check your internet connection.";
        }
        
        if (lowerMessage.contains("timeout") || lowerMessage.contains("timed out")) {
            return "Connection timed out. Please try again.";
        }
        
        if (lowerMessage.contains("ssl") || lowerMessage.contains("certificate")) {
            return "Security error. Please contact support.";
        }
        
        return "Network error. Please check your connection and try again.";
    }
    
    /**
     * Callback interface for authentication operations
     */
    public interface AuthCallback {
        void onSuccess(SupabaseAuthApi.UserResponse user);
        void onError(Throwable error);
    }
}

