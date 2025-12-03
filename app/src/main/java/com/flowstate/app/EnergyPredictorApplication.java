package com.flowstate.app;

import android.app.Application;
import com.flowstate.data.local.AppDb;

public class EnergyPredictorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Initialize Room Database
        AppDb.getInstance(this);
    }
}

