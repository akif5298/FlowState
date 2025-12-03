package com.flowstate.app;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;
import com.flowstate.app.supabase.SupabaseClient;

public class EnergyPredictorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Supabase client
        SupabaseClient.getInstance(this);
        
        // Apply dark mode preference on app startup
        SharedPreferences prefs = getSharedPreferences("app_prefs", MODE_PRIVATE);
        boolean darkMode = prefs.getBoolean("dark_mode", false);
        if (darkMode) {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
        } else {
            AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
        }
    }
}

