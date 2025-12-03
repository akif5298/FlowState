package com.flowstate.app.calendar;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.calendar.CalendarScopes;

import java.util.Collections;

/**
 * Manager for Google Calendar authentication
 */
public class GoogleCalendarAuthManager {
    
    private static final String TAG = "GoogleCalendarAuth";
    private static final String PREFS_NAME = "google_calendar_prefs";
    private static final String KEY_ACCOUNT_NAME = "account_name";
    private static final String KEY_IS_CONNECTED = "is_connected";
    
    private Context context;
    private GoogleSignInClient signInClient;
    private SharedPreferences prefs;
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final NetHttpTransport HTTP_TRANSPORT = new NetHttpTransport();
    
    public GoogleCalendarAuthManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        
        // Configure Google Sign-In for Calendar API
        GoogleSignInOptions.Builder builder = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .requestScopes(new Scope(CalendarScopes.CALENDAR_READONLY));
        
        // Only request server auth code if you have a server client ID
        // For now, we'll skip it to avoid crashes
        // If you have a server client ID, uncomment and add it:
        // .requestServerAuthCode("YOUR_SERVER_CLIENT_ID", false)
        
        GoogleSignInOptions signInOptions = builder.build();
        
        this.signInClient = GoogleSignIn.getClient(context, signInOptions);
    }
    
    /**
     * Check if user is signed in to Google Calendar
     */
    public boolean isSignedIn() {
        GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(context);
        if (account == null) {
            return false;
        }
        
        // Check if account has calendar scope
        boolean hasScope = account.getGrantedScopes() != null && 
                          account.getGrantedScopes().contains(CalendarScopes.CALENDAR_READONLY);
        
        return hasScope && prefs.getBoolean(KEY_IS_CONNECTED, false);
    }
    
    /**
     * Get the sign-in client for starting the OAuth flow
     */
    public GoogleSignInClient getSignInClient() {
        return signInClient;
    }
    
    /**
     * Get the current signed-in account
     */
    public GoogleSignInAccount getSignedInAccount() {
        return GoogleSignIn.getLastSignedInAccount(context);
    }
    
    /**
     * Create a Credential from the signed-in account
     * Note: For full Calendar API functionality, you'll need to exchange the server auth code
     * for access/refresh tokens. This is a simplified version that works with Google Sign-In.
     */
    public Credential getCredential() {
        GoogleSignInAccount account = getSignedInAccount();
        if (account == null) {
            return null;
        }
        
        try {
            // Create a basic credential using BearerToken access method
            // Note: For full Calendar API functionality, you need to:
            // 1. Get the server auth code from account.getServerAuthCode()
            // 2. Exchange it for access/refresh tokens using your backend
            // 3. Set those tokens in the credential using credential.setAccessToken()
            
            Credential credential = new Credential(
                com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod());
            
            // For now, this is a placeholder credential
            // In production, you'd set the access token here:
            // credential.setAccessToken(accessToken);
            
            Log.d(TAG, "Credential placeholder created for account: " + account.getEmail());
            String serverAuthCode = account.getServerAuthCode();
            Log.d(TAG, "Server auth code available: " + (serverAuthCode != null));
            
            return credential;
        } catch (Exception e) {
            Log.e(TAG, "Error creating credential", e);
            // Return a minimal credential as fallback
            return new Credential(
                com.google.api.client.auth.oauth2.BearerToken.authorizationHeaderAccessMethod());
        }
    }
    
    /**
     * Get the server auth code from the signed-in account
     * This can be exchanged for access/refresh tokens on your backend
     */
    public String getServerAuthCode() {
        GoogleSignInAccount account = getSignedInAccount();
        return account != null ? account.getServerAuthCode() : null;
    }
    
    /**
     * Get the account email for the connected account
     */
    public String getAccountEmail() {
        GoogleSignInAccount account = getSignedInAccount();
        return account != null ? account.getEmail() : null;
    }
    
    /**
     * Save connection status
     */
    public void saveConnectionStatus(GoogleSignInAccount account) {
        if (account != null) {
            prefs.edit()
                    .putString(KEY_ACCOUNT_NAME, account.getEmail())
                    .putBoolean(KEY_IS_CONNECTED, true)
                    .apply();
            Log.d(TAG, "Calendar connection saved for: " + account.getEmail());
        }
    }
    
    /**
     * Disconnect and clear saved connection
     */
    public void disconnect() {
        prefs.edit()
                .remove(KEY_ACCOUNT_NAME)
                .putBoolean(KEY_IS_CONNECTED, false)
                .apply();
        
        signInClient.signOut().addOnCompleteListener(task -> {
            Log.d(TAG, "Google Calendar disconnected");
        });
    }
    
    /**
     * Get the connected account email
     */
    public String getConnectedAccountEmail() {
        return prefs.getString(KEY_ACCOUNT_NAME, null);
    }
}


