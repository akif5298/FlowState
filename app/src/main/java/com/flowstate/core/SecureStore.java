package com.flowstate.core;

import android.content.Context;
import androidx.security.crypto.EncryptedSharedPreferences;
import androidx.security.crypto.MasterKey;
import android.util.Log;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import javax.crypto.AEADBadTagException;

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
        encryptedPrefs = createEncryptedPreferences(context);
    }
    
    /**
     * Create encrypted preferences, handling keystore corruption gracefully
     */
    private android.content.SharedPreferences createEncryptedPreferences(Context context) {
        try {
            // Create or retrieve the master key
            MasterKey masterKey = new MasterKey.Builder(context)
                    .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                    .build();
            
            // Create encrypted shared preferences
            return EncryptedSharedPreferences.create(
                    context,
                    PREFS_NAME,
                    masterKey,
                    EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                    EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            );
        } catch (GeneralSecurityException e) {
            // Check if this is a keystore corruption issue (AEADBadTagException or KeyStoreException)
            boolean isKeystoreCorruption = e instanceof AEADBadTagException || 
                    (e.getCause() != null && e.getCause().getClass().getName().contains("KeyStoreException"));
            
            if (isKeystoreCorruption) {
                Log.w("SecureStore", "Keystore corruption detected, attempting to recover by deleting corrupted preferences", e);
                
                // Try to delete the corrupted encrypted preferences file
                try {
                    File prefsFile = new File(context.getFilesDir().getParent(), "shared_prefs/" + PREFS_NAME + ".xml");
                    if (prefsFile.exists()) {
                        boolean deleted = prefsFile.delete();
                        Log.d("SecureStore", "Deleted corrupted preferences file: " + deleted);
                    }
                    
                    // Also try deleting the master key file if it exists
                    File masterKeyFile = new File(context.getFilesDir().getParent(), "shared_prefs/__androidx_security_crypto_encrypted_prefs_key_keyset__.xml");
                    if (masterKeyFile.exists()) {
                        boolean deleted = masterKeyFile.delete();
                        Log.d("SecureStore", "Deleted master key file: " + deleted);
                    }
                    
                    // Try creating encrypted preferences again with fresh files
                    try {
                        MasterKey masterKey = new MasterKey.Builder(context)
                                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                                .build();
                        
                        return EncryptedSharedPreferences.create(
                                context,
                                PREFS_NAME,
                                masterKey,
                                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
                        );
                    } catch (GeneralSecurityException | IOException retryException) {
                        Log.e("SecureStore", "Failed to recreate encrypted preferences after cleanup", retryException);
                        // Fall through to fallback
                    }
                } catch (Exception cleanupException) {
                    Log.e("SecureStore", "Error during cleanup of corrupted preferences", cleanupException);
                    // Fall through to fallback
                }
            } else {
                Log.e("SecureStore", "Failed to create encrypted preferences (non-corruption error)", e);
            }
            
            // Fallback to regular SharedPreferences if encryption fails
            Log.w("SecureStore", "Falling back to regular SharedPreferences (not encrypted)");
            return context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
        } catch (IOException e) {
            Log.e("SecureStore", "Failed to create encrypted preferences (IO error)", e);
            // Fallback to regular SharedPreferences if encryption fails
            Log.w("SecureStore", "Falling back to regular SharedPreferences (not encrypted)");
            return context.getSharedPreferences(PREFS_NAME + "_fallback", Context.MODE_PRIVATE);
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

