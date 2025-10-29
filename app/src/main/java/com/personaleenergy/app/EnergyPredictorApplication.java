package com.personaleenergy.app;

import android.app.Application;
import com.google.firebase.FirebaseApp;

public class EnergyPredictorApplication extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        FirebaseApp.initializeApp(this);
    }
}

