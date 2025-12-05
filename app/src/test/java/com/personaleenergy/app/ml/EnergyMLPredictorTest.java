package com.personaleenergy.app.ml;

import android.content.Context;
import com.flowstate.app.data.models.BiometricData;
import com.flowstate.app.data.models.EnergyLevel;
import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.data.models.ReactionTimeData;
import com.flowstate.app.data.models.TypingSpeedData;
import org.junit.Before;
import org.junit.Ignore;
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
 * Unit tests for EnergyMLPredictor
 * Uses Robolectric to provide Android context
 * 
 * NOTE: These tests are skipped on Windows due to Robolectric's POSIX permissions issue.
 * Run tests on Linux/Mac or use WSL (Windows Subsystem for Linux) for full coverage.
 * Model tests (BiometricDataTest, etc.) work fine on Windows.
 */
@RunWith(RobolectricTestRunner.class)
@Config(manifest = Config.NONE, sdk = 28)
public class EnergyMLPredictorTest {
    
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
    private EnergyMLPredictor predictor;
    private List<BiometricData> testBiometrics;
    private List<TypingSpeedData> testTypingData;
    private List<ReactionTimeData> testReactionData;
    private List<EnergyPrediction> testHistoricalPredictions;
    
    @Before
    public void setUp() {
        // Skip on Windows - Robolectric fails during initialization
        org.junit.Assume.assumeFalse(
            "Skipping Robolectric tests on Windows due to POSIX permissions issue. " +
            "Use Linux/Mac or WSL for full test coverage.",
            IS_WINDOWS
        );
        
        context = RuntimeEnvironment.getApplication();
        predictor = new EnergyMLPredictor(context);
        setupTestData();
    }
    
    private void setupTestData() {
        testBiometrics = new ArrayList<>();
        Date now = new Date();
        for (int i = 0; i < 5; i++) {
            Date timestamp = new Date(now.getTime() - (i * 24 * 60 * 60 * 1000L));
            BiometricData data = new BiometricData(timestamp);
            data.setHeartRate(70 + i * 2);
            data.setSleepMinutes(420 + i * 30);
            data.setSleepQuality(0.75 + i * 0.05);
            testBiometrics.add(data);
        }
        
        testTypingData = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Date timestamp = new Date(now.getTime() - (i * 24 * 60 * 60 * 1000L));
            TypingSpeedData data = new TypingSpeedData(timestamp, 50 + i * 5, 95.0, "Test text");
            testTypingData.add(data);
        }
        
        testReactionData = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Date timestamp = new Date(now.getTime() - (i * 24 * 60 * 60 * 1000L));
            ReactionTimeData data = new ReactionTimeData(timestamp, 250 + i * 20);
            testReactionData.add(data);
        }
        
        testHistoricalPredictions = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            Date timestamp = new Date(now.getTime() - (i * 24 * 60 * 60 * 1000L));
            EnergyPrediction pred = new EnergyPrediction(
                timestamp,
                EnergyLevel.HIGH,
                0.8,
                null,
                null
            );
            testHistoricalPredictions.add(pred);
        }
    }
    
    @Test
    public void testPredictorInitialization() {
        org.junit.Assume.assumeNotNull(predictor);
        assertNotNull(predictor);
    }
    
    @Test
    public void testPredictWithValidData() {
        org.junit.Assume.assumeNotNull(predictor);
        Date targetTime = new Date();
        EnergyPrediction prediction = predictor.predict(
            testBiometrics,
            testTypingData,
            testReactionData,
            testHistoricalPredictions,
            targetTime
        );
        
        assertNotNull(prediction);
        assertNotNull(prediction.getTimestamp());
        assertNotNull(prediction.getPredictedLevel());
        assertTrue(prediction.getConfidence() >= 0.0 && prediction.getConfidence() <= 1.0);
        assertTrue(prediction.getPredictedLevel() == EnergyLevel.HIGH ||
                   prediction.getPredictedLevel() == EnergyLevel.MEDIUM ||
                   prediction.getPredictedLevel() == EnergyLevel.LOW);
    }
    
    @Test
    public void testPredictWithEmptyData() {
        org.junit.Assume.assumeNotNull(predictor);
        Date targetTime = new Date();
        EnergyPrediction prediction = predictor.predict(
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            new ArrayList<>(),
            targetTime
        );
        
        assertNotNull(prediction);
        assertNotNull(prediction.getPredictedLevel());
    }
    
    @Test
    public void testPredictWithNullData() {
        org.junit.Assume.assumeNotNull(predictor);
        Date targetTime = new Date();
        EnergyPrediction prediction = predictor.predict(
            null,
            null,
            null,
            null,
            targetTime
        );
        
        assertNotNull(prediction);
        assertNotNull(prediction.getPredictedLevel());
    }
    
    @Test
    public void testPredictWithPartialData() {
        org.junit.Assume.assumeNotNull(predictor);
        Date targetTime = new Date();
        
        // Test with only biometrics
        EnergyPrediction pred1 = predictor.predict(
            testBiometrics,
            null,
            null,
            null,
            targetTime
        );
        assertNotNull(pred1);
        
        // Test with only typing data
        EnergyPrediction pred2 = predictor.predict(
            null,
            testTypingData,
            null,
            null,
            targetTime
        );
        assertNotNull(pred2);
        
        // Test with only reaction data
        EnergyPrediction pred3 = predictor.predict(
            null,
            null,
            testReactionData,
            null,
            targetTime
        );
        assertNotNull(pred3);
    }
    
    @Test
    public void testPredictDifferentTimesOfDay() {
        org.junit.Assume.assumeNotNull(predictor);
        Calendar cal = Calendar.getInstance();
        
        // Morning (8 AM)
        cal.set(Calendar.HOUR_OF_DAY, 8);
        cal.set(Calendar.MINUTE, 0);
        Date morning = cal.getTime();
        EnergyPrediction morningPred = predictor.predict(
            testBiometrics, testTypingData, testReactionData, testHistoricalPredictions, morning
        );
        assertNotNull(morningPred);
        
        // Afternoon (2 PM)
        cal.set(Calendar.HOUR_OF_DAY, 14);
        Date afternoon = cal.getTime();
        EnergyPrediction afternoonPred = predictor.predict(
            testBiometrics, testTypingData, testReactionData, testHistoricalPredictions, afternoon
        );
        assertNotNull(afternoonPred);
        
        // Evening (8 PM)
        cal.set(Calendar.HOUR_OF_DAY, 20);
        Date evening = cal.getTime();
        EnergyPrediction eveningPred = predictor.predict(
            testBiometrics, testTypingData, testReactionData, testHistoricalPredictions, evening
        );
        assertNotNull(eveningPred);
    }
    
    @Test
    public void testExtractFeatures() {
        org.junit.Assume.assumeNotNull(predictor);
        Date targetTime = new Date();
        float[] features = predictor.extractFeatures(
            testBiometrics,
            testTypingData,
            testReactionData,
            testHistoricalPredictions,
            targetTime
        );
        
        assertNotNull(features);
        assertTrue(features.length > 0);
    }
    
    @Test
    public void testClose() {
        org.junit.Assume.assumeNotNull(predictor);
        // Test that close doesn't throw exception
        try {
            predictor.close();
        } catch (Exception e) {
            fail("close() should not throw exception: " + e.getMessage());
        }
    }
}

