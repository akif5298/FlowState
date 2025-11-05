package com.flowstate.app;

import android.app.Application;
import com.flowstate.app.supabase.SupabaseClient;

public class EnergyPredictorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Supabase client
        SupabaseClient.getInstance(this);
    }
}

