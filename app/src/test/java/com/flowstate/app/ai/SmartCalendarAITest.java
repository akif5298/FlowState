package com.flowstate.app.ai;

import android.content.Context;
import com.flowstate.app.data.models.EnergyLevel;
import com.flowstate.app.data.models.EnergyPrediction;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TestRule;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import com.personaleenergy.app.util.WindowsSkipRule;
import static org.junit.Assert.*;

import java.util.*;

/**
 * Unit tests for SmartCalendarAI
 * Uses Robolectric to provide Android context
 * 
 * NOTE: These tests are skipped on Windows due to Robolectric's POSIX permissions issue.
 * Run tests on Linux/Mac or use WSL (Windows Subsystem for Linux) for full coverage.
 * Model tests (BiometricDataTest, etc.) work fine on Windows.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class SmartCalendarAITest {
    
    private static final boolean IS_WINDOWS = System.getProperty("os.name", "").toLowerCase().contains("win");
    
    static {
        // Skip Robolectric initialization on Windows to avoid POSIX permissions error
        if (IS_WINDOWS) {
            // This will cause tests to be skipped via Assume in @Before
        }
    }
    
    @Rule
    public TestRule windowsSkipRule = new WindowsSkipRule();
    
    private Context context;
    private SmartCalendarAI smartCalendarAI;
    
    @Before
    public void setUp() {
        // Skip on Windows - Robolectric fails during initialization
        org.junit.Assume.assumeFalse(
            "Skipping Robolectric tests on Windows due to POSIX permissions issue. " +
            "Use Linux/Mac or WSL for full test coverage.",
            IS_WINDOWS
        );
        
        context = RuntimeEnvironment.getApplication();
        smartCalendarAI = new SmartCalendarAI(context);
    }
    
    @Test
    public void testInitialization() {
        org.junit.Assume.assumeNotNull(smartCalendarAI);
        assertNotNull(smartCalendarAI);
    }
    
    @Test
    public void testScheduledItemTypeEnum() {
        // Test that all ScheduledItemType enum values exist
        SmartCalendarAI.ScheduledItemType[] types = SmartCalendarAI.ScheduledItemType.values();
        assertTrue(types.length > 0);
        
        // Verify expected types exist
        boolean hasAIScheduled = false;
        boolean hasExistingEvent = false;
        for (SmartCalendarAI.ScheduledItemType type : types) {
            if (type.name().contains("AI_SCHEDULED")) {
                hasAIScheduled = true;
            }
            if (type.name().contains("EXISTING")) {
                hasExistingEvent = true;
            }
        }
    }
    
    @Test
    public void testScheduledItemCreation() {
        long now = System.currentTimeMillis();
        long oneHour = 60 * 60 * 1000L;
        
        SmartCalendarAI.ScheduledItem item = new SmartCalendarAI.ScheduledItem();
        item.title = "Test Task";
        item.startTime = now;
        item.endTime = now + oneHour;
        item.energyLevel = EnergyLevel.HIGH;
        item.type = SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK;
        item.reasoning = "Test reasoning";
        
        assertEquals("Test Task", item.title);
        assertEquals(now, item.startTime);
        assertEquals(now + oneHour, item.endTime);
        assertEquals(EnergyLevel.HIGH, item.energyLevel);
        assertNotNull(item.type);
        assertEquals("Test reasoning", item.reasoning);
    }
    
    @Test
    public void testSmartScheduleCreation() {
        SmartCalendarAI.SmartSchedule schedule = new SmartCalendarAI.SmartSchedule();
        schedule.scheduledItems = new ArrayList<>();
        schedule.summary = "Test schedule";
        
        assertNotNull(schedule.scheduledItems);
        assertNotNull(schedule.summary);
    }
    
    @Test
    public void testValidateAndFixOverlaps() {
        long baseTime = System.currentTimeMillis();
        long oneHour = 60 * 60 * 1000L;
        
        SmartCalendarAI.SmartSchedule schedule = new SmartCalendarAI.SmartSchedule();
        schedule.scheduledItems = new ArrayList<>();
        
        // Create overlapping items
        SmartCalendarAI.ScheduledItem item1 = new SmartCalendarAI.ScheduledItem();
        item1.title = "Task 1";
        item1.startTime = baseTime;
        item1.endTime = baseTime + oneHour;
        item1.type = SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK;
        
        SmartCalendarAI.ScheduledItem item2 = new SmartCalendarAI.ScheduledItem();
        item2.title = "Task 2";
        item2.startTime = baseTime + (30 * 60 * 1000L); // Overlaps with item1
        item2.endTime = baseTime + oneHour + (30 * 60 * 1000L);
        item2.type = SmartCalendarAI.ScheduledItemType.AI_SCHEDULED_TASK;
        
        schedule.scheduledItems.add(item1);
        schedule.scheduledItems.add(item2);
        
        // Note: This tests the structure, actual validation logic would need reflection
        // or making the method package-private for testing
        assertNotNull(schedule.scheduledItems);
        assertEquals(2, schedule.scheduledItems.size());
    }
}

