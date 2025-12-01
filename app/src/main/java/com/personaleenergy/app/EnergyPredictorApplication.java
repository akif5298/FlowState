package com.personaleenergy.app;

import android.app.Application;
import android.util.Log;
import androidx.work.Configuration;

public class EnergyPredictorApplication extends Application implements Configuration.Provider {

    @Override
    public void onCreate() {
        super.onCreate();
    }

    @Override
    public Configuration getWorkManagerConfiguration() {
        return new Configuration.Builder()
                .setMinimumLoggingLevel(Log.INFO)
                .build();
    }
}
