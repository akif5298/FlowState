package com.personaleenergy.app.ui;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.espresso.matcher.ViewMatchers;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.assertNotNull;

/**
 * Instrumented tests for EnergyPredictionActivity
 */
@RunWith(AndroidJUnit4.class)
public class EnergyPredictionActivityTest {
    
    @Rule
    public ActivityTestRule<EnergyPredictionActivity> activityRule =
        new ActivityTestRule<>(EnergyPredictionActivity.class, true, false);
    
    @Test
    public void testActivityLaunches() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EnergyPredictionActivity.class);
        ActivityScenario<EnergyPredictionActivity> scenario = ActivityScenario.launch(intent);
        
        scenario.onActivity(activity -> {
            assertNotNull(activity);
        });
    }
    
    @Test
    public void testActivityLifecycle() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), EnergyPredictionActivity.class);
        ActivityScenario<EnergyPredictionActivity> scenario = ActivityScenario.launch(intent);
        
        scenario.onActivity(activity -> {
            assertNotNull(activity);
        });
        
        // Test recreation
        scenario.recreate();
        
        scenario.onActivity(activity -> {
            assertNotNull(activity);
        });
    }
}

