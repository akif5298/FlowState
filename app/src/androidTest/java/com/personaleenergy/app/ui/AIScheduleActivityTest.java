package com.personaleenergy.app.ui.schedule;

import android.content.Intent;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.rule.ActivityTestRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

/**
 * Instrumented tests for AIScheduleActivity
 */
@RunWith(AndroidJUnit4.class)
public class AIScheduleActivityTest {
    
    @Rule
    public ActivityTestRule<AIScheduleActivity> activityRule =
        new ActivityTestRule<>(AIScheduleActivity.class, true, false);
    
    @Test
    public void testActivityLaunches() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AIScheduleActivity.class);
        ActivityScenario<AIScheduleActivity> scenario = ActivityScenario.launch(intent);
        
        scenario.onActivity(activity -> {
            assertNotNull(activity);
        });
    }
    
    @Test
    public void testActivityInitialization() {
        Intent intent = new Intent(ApplicationProvider.getApplicationContext(), AIScheduleActivity.class);
        ActivityScenario<AIScheduleActivity> scenario = ActivityScenario.launch(intent);
        
        scenario.onActivity(activity -> {
            // Wait for initialization
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                // Ignore
            }
            
            assertNotNull(activity);
        });
    }
}

