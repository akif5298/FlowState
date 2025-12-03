package com.flowstate.app.supabase;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.flowstate.app.supabase.api.SupabaseAuthApi;
import java.util.UUID;

/**
 * Service for handling authentication (Offline Mode)
 * Bypasses Supabase and simulates local authentication.
 */
public class AuthService {
    
    private Handler mainHandler;
    private static final String PREFS_NAME = "flowstate_prefs";
    private static final String KEY_IS_LOGGED_IN = "is_logged_in";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_ID = "user_id";
    private Context context;
    
    public AuthService(Context context) {
        this.context = context;
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    public void signUp(String email, String password, String username, String firstName, String lastName, AuthCallback callback) {
        // Simulate successful sign up locally
        mainHandler.postDelayed(() -> {
            String userId = UUID.randomUUID().toString();
            saveUserSession(userId, email);
            
            SupabaseAuthApi.UserResponse mockUser = new SupabaseAuthApi.UserResponse();
            mockUser.id = userId;
            mockUser.email = email;
            
            callback.onSuccess(mockUser);
        }, 1000);
    }
    
    public void signIn(String email, String password, AuthCallback callback) {
        // Simulate successful sign in locally
        mainHandler.postDelayed(() -> {
            String userId = UUID.randomUUID().toString(); // In real app, would fetch existing ID
            saveUserSession(userId, email);
            
            SupabaseAuthApi.UserResponse mockUser = new SupabaseAuthApi.UserResponse();
            mockUser.id = userId;
            mockUser.email = email;
            
            callback.onSuccess(mockUser);
        }, 1000);
    }
    
    public void signOut(AuthCallback callback) {
        clearUserSession();
        mainHandler.post(() -> callback.onSuccess(null));
    }
    
    public void getCurrentUser(AuthCallback callback) {
        if (isAuthenticated()) {
             SupabaseAuthApi.UserResponse mockUser = new SupabaseAuthApi.UserResponse();
             mockUser.id = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USER_ID, "offline-user");
             mockUser.email = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_USER_EMAIL, "offline@example.com");
             mainHandler.post(() -> callback.onSuccess(mockUser));
        } else {
             mainHandler.post(() -> callback.onSuccess(null));
        }
    }
    
    public void resetPassword(String email, AuthCallback callback) {
        mainHandler.post(() -> callback.onSuccess(null));
    }
    
    public boolean isAuthenticated() {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_IS_LOGGED_IN, false);
    }
    
    private void saveUserSession(String userId, String email) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_IS_LOGGED_IN, true)
                .putString(KEY_USER_ID, userId)
                .putString(KEY_USER_EMAIL, email)
                .apply();
    }
    
    private void clearUserSession() {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .clear()
                .apply();
    }

    public interface AuthCallback {
        void onSuccess(SupabaseAuthApi.UserResponse user);
        void onError(Throwable error);
    }
}

