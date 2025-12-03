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
 * Client for FlowState Remote Model (Railway).
 * Base URL comes from Config.REMOTE_MODEL_URL.
 */
public class RemoteModelClient {

    private static RemoteModelClient instance;
    private final RemoteModelApi api;
    private final SecureStore secureStore;

    private RemoteModelClient(Context context) {
        this.secureStore = SecureStore.getInstance(context);

        Gson gson = new GsonBuilder().create();

        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BODY);

        OkHttpClient okHttpClient = new OkHttpClient.Builder()
                .addInterceptor(logging)
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(60, TimeUnit.SECONDS)
                .writeTimeout(60, TimeUnit.SECONDS)
                .build();

        String baseUrl = Config.REMOTE_MODEL_URL;
        if (!baseUrl.endsWith("/")) {
            baseUrl = baseUrl + "/"; // Retrofit requires trailing slash on baseUrl
        }

        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create(gson))
                .build();

        this.api = retrofit.create(RemoteModelApi.class);
    }

    public static synchronized RemoteModelClient getInstance(Context context) {
        if (instance == null) {
            instance = new RemoteModelClient(context.getApplicationContext());
        }
        return instance;
    }

    public RemoteModelApi getApi() {
        return api;
    }

    /**
     * Optional: Provide bearer token if REMOTE_API_KEY is set
     */
    public String getAuthorizationHeader() {
        String apiKey = Config.REMOTE_MODEL_API_KEY;
        if (apiKey != null && !apiKey.isEmpty()) {
            return "Bearer " + apiKey;
        }
        return null;
    }
}
