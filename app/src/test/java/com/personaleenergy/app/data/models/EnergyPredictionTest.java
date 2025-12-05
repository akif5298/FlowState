package com.personaleenergy.app.data.models;

import com.flowstate.app.data.models.EnergyPrediction;
import com.flowstate.app.data.models.EnergyLevel;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Unit tests for EnergyPrediction model
 */
public class EnergyPredictionTest {
    
    private Date testTimestamp;
    private EnergyPrediction prediction;
    
    @Before
    public void setUp() {
        testTimestamp = new Date();
    }
    
    @Test
    public void testConstructor() {
        Map<String, Double> biometricFactors = new HashMap<>();
        biometricFactors.put("sleep_quality", 0.85);
        biometricFactors.put("heart_rate", 72.0);
        
        Map<String, Double> cognitiveFactors = new HashMap<>();
        cognitiveFactors.put("typing_speed", 60.0);
        cognitiveFactors.put("reaction_time", 250.0);
        
        prediction = new EnergyPrediction(
            testTimestamp,
            EnergyLevel.HIGH,
            0.92,
            biometricFactors,
            cognitiveFactors
        );
        
        assertEquals(testTimestamp, prediction.getTimestamp());
        assertEquals(EnergyLevel.HIGH, prediction.getPredictedLevel());
        assertEquals(0.92, prediction.getConfidence(), 0.001);
        assertNotNull(prediction.getBiometricFactors());
        assertNotNull(prediction.getCognitiveFactors());
    }
    
    @Test
    public void testSettersAndGetters() {
        prediction = new EnergyPrediction(
            testTimestamp,
            EnergyLevel.MEDIUM,
            0.75,
            null,
            null
        );
        
        Date newTimestamp = new Date(System.currentTimeMillis() + 1000);
        prediction.setTimestamp(newTimestamp);
        prediction.setPredictedLevel(EnergyLevel.LOW);
        prediction.setConfidence(0.65);
        
        Map<String, Double> factors = new HashMap<>();
        factors.put("test", 1.0);
        prediction.setBiometricFactors(factors);
        prediction.setCognitiveFactors(factors);
        
        assertEquals(newTimestamp, prediction.getTimestamp());
        assertEquals(EnergyLevel.LOW, prediction.getPredictedLevel());
        assertEquals(0.65, prediction.getConfidence(), 0.001);
        assertEquals(factors, prediction.getBiometricFactors());
        assertEquals(factors, prediction.getCognitiveFactors());
    }
    
    @Test
    public void testAllEnergyLevels() {
        for (EnergyLevel level : EnergyLevel.values()) {
            prediction = new EnergyPrediction(
                testTimestamp,
                level,
                0.8,
                null,
                null
            );
            assertEquals(level, prediction.getPredictedLevel());
        }
    }
    
    @Test
    public void testConfidenceRange() {
        // Test valid confidence values
        prediction = new EnergyPrediction(
            testTimestamp,
            EnergyLevel.HIGH,
            0.0,
            null,
            null
        );
        assertEquals(0.0, prediction.getConfidence(), 0.001);
        
        prediction.setConfidence(1.0);
        assertEquals(1.0, prediction.getConfidence(), 0.001);
        
        prediction.setConfidence(0.5);
        assertEquals(0.5, prediction.getConfidence(), 0.001);
    }
    
    @Test
    public void testNullFactors() {
        prediction = new EnergyPrediction(
            testTimestamp,
            EnergyLevel.MEDIUM,
            0.7,
            null,
            null
        );
        
        assertNull(prediction.getBiometricFactors());
        assertNull(prediction.getCognitiveFactors());
    }
}

