package com.personaleenergy.app.supabase.repository;

import android.content.Context;
import com.personaleenergy.app.data.models.ReactionTimeData;
import com.personaleenergy.app.supabase.SupabaseClient;
import com.personaleenergy.app.supabase.api.SupabasePostgrestApi;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

public class ReactionTimeRepository {

    private final SupabasePostgrestApi postgrestApi;
    private final SupabaseClient supabaseClient;
    private final SimpleDateFormat dateFormat;

    public ReactionTimeRepository(Context context) {
        supabaseClient = SupabaseClient.getInstance(context);
        postgrestApi = supabaseClient.getPostgrestApi();
        dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US);
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void insertReactionTimeData(ReactionTimeData data, String userId, GeneralCallback callback) {
        String bearerToken = "Bearer " + supabaseClient.getAccessToken();
        Map<String, Object> body = new HashMap<>();
        body.put("user_id", userId);
        body.put("timestamp", dateFormat.format(data.getTimestamp()));
        body.put("reaction_time_ms", data.getReactionTimeMs());

        postgrestApi.insertReactionTime(bearerToken, supabaseClient.getSupabaseAnonKey(), "return=representation", body).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Call<Void> call, Response<Void> response) {
                if (response.isSuccessful()) {
                    callback.onSuccess();
                } else {
                    try {
                        callback.onFailure(new Exception("Failed to insert reaction time: " + response.errorBody().string()));
                    } catch (IOException e) {
                        callback.onFailure(e);
                    }
                }
            }

            @Override
            public void onFailure(Call<Void> call, Throwable t) {
                callback.onFailure(t);
            }
        });
    }

    public interface GeneralCallback {
        void onSuccess();
        void onFailure(Throwable t);
    }
}
