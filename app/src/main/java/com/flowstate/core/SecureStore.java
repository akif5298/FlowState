package com.flowstate.core;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import android.util.Log;

import java.io.IOException;
import java.security.GeneralSecurityException;

/**
 * Secure storage for sensitive data using EncryptedSharedPreferences
 * 
 * This class provides encrypted storage for authentication tokens and other sensitive data.
 * Uses Android's EncryptedSharedPreferences for secure on-device storage.
 */
public class SecureStore {
    
    private static final String PREFS_NAME = "flowstate_secure_prefs";
    private static final String KEY_ACCESS_TOKEN = "access_token";
    private static final String KEY_REFRESH_TOKEN = "refresh_token";
    
    private static SecureStore instance;
    private android.content.SharedPreferences encryptedPrefs;
    
    private SecureStore(Context context) {
        try {
            // Create or retrieve the master key
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            
            // Create encrypted shared preferences
            encryptedPrefs = EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException | IOException e) {
            Log.e("SecureStore", "Failed to create encrypted preferences", e);
            // Fallback to regular SharedPreferences if encryption fails
            // This should not happen in normal circumstances
            encryptedPrefs = context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
        }
    }
    
    /**
     * Get singleton instance of SecureStore
     * 
     * @param context Application context
     * @return SecureStore instance
     */
    public static synchronized SecureStore getInstance(Context context) {
        if (instance == null) {
            instance = new SecureStore(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Store access and refresh tokens securely
     * 
     * @param accessToken Access token from authentication
     * @param refreshToken Refresh token from authentication
     */
    public void putToken(String accessToken, String refreshToken) {
        encryptedPrefs.edit()
                .putString(KEY_ACCESS_TOKEN, accessToken)
                .putString(KEY_REFRESH_TOKEN, refreshToken)
                .apply();
    }
    
    /**
     * Get the stored access token
     * 
     * @return Access token, or null if not stored
     */
    public String getAccessToken() {
        return encryptedPrefs.getString(KEY_ACCESS_TOKEN, null);
    }
    
    /**
     * Get the stored refresh token
     * 
     * @return Refresh token, or null if not stored
     */
    public String getRefreshToken() {
        return encryptedPrefs.getString(KEY_REFRESH_TOKEN, null);
    }
    
    /**
     * Clear all stored tokens
     */
    public void clear() {
        encryptedPrefs.edit()
                .remove(KEY_ACCESS_TOKEN)
                .remove(KEY_REFRESH_TOKEN)
                .apply();
    }
    
    /**
     * Check if an access token is stored
     * 
     * @return true if access token exists, false otherwise
     */
    public boolean hasAccessToken() {
        String token = getAccessToken();
        return token != null && !token.isEmpty();
    }
}

