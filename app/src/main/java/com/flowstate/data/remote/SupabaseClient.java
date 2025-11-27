package com.flowstate.data.remote;

import android.content.Context;
import com.flowstate.core.Config;
import com.flowstate.core.SecureStore;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

import java.util.concurrent.TimeUnit;

/**
 * Supabase client with two Retrofit instances:
 * - postgrest: for PostgREST API calls (SUPABASE_URL + "/rest/v1/")
 * - rpc: for RPC function calls (SUPABASE_URL + "/rest/v1/rpc/")
 * 
 * OkHttp Interceptor automatically adds headers:
 * - apikey: Supabase anonymous key
 * - Authorization: Bearer <access_token> (if available)
 * - Content-Type: application/json
 */
public class SupabaseClient {
    
    private static final String SUPABASE_URL = Config.SUPABASE_URL;
    private static final String SUPABASE_ANON_KEY = Config.SUPABASE_ANON_KEY;
    
    private static SupabaseClient instance;
    private Retrofit postgrestRetrofit;
    private Retrofit rpcRetrofit;
    private PostgrestApi postgrestApi;
    private RpcApi rpcApi;
    private SecureStore secureStore;
    private Gson gson;
    
    private SupabaseClient(Context context) {
        this.secureStore = SecureStore.getInstance(context);
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
                .create();
        
        // Create OkHttp client with interceptor for headers
        OkHttpClient okHttpClient = createOkHttpClient();
        
        // Create PostgREST Retrofit instance
        this.postgrestRetrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL + "/rest/v1/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        // Create RPC Retrofit instance
        this.rpcRetrofit = new Retrofit.Builder()
                .baseUrl(SUPABASE_URL + "/rest/v1/rpc/")
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();
        
        // Create API interfaces
        this.postgrestApi = postgrestRetrofit.create(PostgrestApi.class);
        this.rpcApi = rpcRetrofit.create(RpcApi.class);
    }
    
    /**
     * Create OkHttp client with interceptor that adds required headers
     */
    private OkHttpClient createOkHttpClient() {
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);
        
        return new OkHttpClient.Builder()
                .addInterceptor(logging)
                .addInterceptor(chain -> {
                    okhttp3.Request original = chain.request();
                    okhttp3.Request.Builder requestBuilder = original.newBuilder()
                            .header("apikey", SUPABASE_ANON_KEY)
                            .header("Content-Type", "application/json");
                    
                    // Add Authorization header if token is available
                    String accessToken = secureStore.getAccessToken();
                    if (accessToken != null && !accessToken.isEmpty()) {
                        requestBuilder.header("Authorization", "Bearer " + accessToken);
                    }
                    
                    return chain.proceed(requestBuilder.build());
                })
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
    }
    
    public static synchronized SupabaseClient getInstance(Context context) {
        if (instance == null) {
            instance = new SupabaseClient(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * Get PostgREST API interface
     */
    public PostgrestApi getPostgrestApi() {
        return postgrestApi;
    }
    
    /**
     * Get RPC API interface
     */
    public RpcApi getRpcApi() {
        return rpcApi;
    }
    
    /**
     * Get access token from secure storage
     */
    public String getAccessToken() {
        return secureStore.getAccessToken();
    }
    
    /**
     * Store tokens securely
     */
    public void setTokens(String accessToken, String refreshToken) {
        secureStore.putToken(accessToken, refreshToken);
    }
    
    /**
     * Clear all tokens
     */
    public void clearTokens() {
        secureStore.clear();
    }
}

