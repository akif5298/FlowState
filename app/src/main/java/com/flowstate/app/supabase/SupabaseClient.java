package com.flowstate.app.supabase;

import android.content.Context;
import com.flowstate.core.Config;
import com.flowstate.core.SecureStore;
import com.flowstate.app.supabase.api.SupabaseAuthApi;
import com.flowstate.app.supabase.api.SupabasePostgrestApi;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Supabase client for Java using Retrofit
 * 
 * Supabase credentials are loaded from Config class, which reads from BuildConfig
 * Add your credentials to local.properties:
 *   SUPABASE_URL=https://your-project.supabase.co
 *   SUPABASE_ANON_KEY=your-anon-key
 * 
 * You can find these in your Supabase project settings -> API
 */
public class SupabaseClient {
    
    // Credentials are loaded from Config (which reads from BuildConfig)
    private static final String SUPABASE_URL = Config.SUPABASE_URL;
    private static final String SUPABASE_ANON_KEY = Config.SUPABASE_ANON_KEY;
    
    private static final String KEY_USER_ID = "user_id";
    private static final String PREFS_NAME = "flowstate_supabase";
    
    private static SupabaseClient instance;
    private Retrofit retrofit;
    private SupabaseAuthApi authApi;
    private SupabasePostgrestApi postgrestApi;
    private SecureStore secureStore;
    private android.content.SharedPreferences prefs; // For non-sensitive data like user_id
    private Gson gson;
    
    private SupabaseClient(Context context) {
        // Use SecureStore for sensitive tokens, regular SharedPreferences for non-sensitive data
        this.secureStore = SecureStore.getInstance(context);
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new GsonBuilder().setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").create();
        
        // Setup OkHttp with logging
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    // Add default headers
                    okhttp3.Request original = chain.request();
                    okhttp3.Request.Builder requestBuilder = original.newBuilder()
                            .header("apikey", SUPABASE_ANON_KEY)
                            .header("Content-Type", "application/json");
                    
                    // Add auth token if available and not already present
                    // (Some API methods pass headers explicitly)
                    if (original.header("Authorization") == null) {
                        String accessToken = getAccessToken();
                        if (accessToken != null && !accessToken.isEmpty()) {
                            requestBuilder.header("Authorization", "Bearer " + accessToken);
                        }
                    }
                    
                    return chain.proceed(requestBuilder.build());
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        // Create Retrofit instance
        this.retrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        this.authApi = retrofit.create(SupabaseAuthApi.class);
        this.postgrestApi = retrofit.create(SupabasePostgrestApi.class);
    }
    
    public static synchronized SupabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseClient(context);
        }
        return instance;
    }
    
    public SupabaseAuthApi getAuthApi() {
        return authApi;
    }
    
    public SupabasePostgrestApi getPostgrestApi() {
        return postgrestApi;
    }
    
    public String getAccessToken() {
        return secureStore.getAccessToken();
    }
    
    public void setAccessToken(String accessToken) {
        // Store access token securely
        String refreshToken = secureStore.getRefreshToken();
        secureStore.putToken(accessToken, refreshToken != null ? refreshToken : "");
    }
    
    public String getRefreshToken() {
        return secureStore.getRefreshToken();
    }
    
    public void setRefreshToken(String refreshToken) {
        // Store refresh token securely
        String accessToken = secureStore.getAccessToken();
        secureStore.putToken(accessToken != null ? accessToken : "", refreshToken);
    }
    
    /**
     * Store both tokens together (more efficient)
     */
    public void setTokens(String accessToken, String refreshToken) {
        secureStore.putToken(accessToken, refreshToken);
    }
    
    public String getUserId() {
        return prefs.getString(KEY_USER_ID, null);
    }
    
    public void setUserId(String userId) {
        prefs.edit().putString(KEY_USER_ID, userId).apply();
    }
    
    public void clearAuth() {
        // Clear secure tokens
        secureStore.clear();
        // Clear non-sensitive data
        prefs.edit()
                .remove(KEY_USER_ID)
                .apply();
    }
    
    public boolean isAuthenticated() {
        return secureStore.hasAccessToken();
    }
    
    public String getSupabaseUrl() {
        return SUPABASE_URL;
    }
    
    public String getSupabaseAnonKey() {
        return SUPABASE_ANON_KEY;
    }
}

