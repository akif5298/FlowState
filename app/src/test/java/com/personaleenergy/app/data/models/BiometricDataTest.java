package com.personaleenergy.app.data.models;

import com.flowstate.app.data.models.BiometricData;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * Unit tests for BiometricData model
 */
public class BiometricDataTest {
    
    private Date testTimestamp;
    private BiometricData biometricData;
    
    @Before
    public void setUp() {
        testTimestamp = new Date();
        biometricData = new BiometricData(testTimestamp);
    }
    
    @Test
    public void testConstructorWithTimestamp() {
        assertNotNull(biometricData);
        assertEquals(testTimestamp, biometricData.getTimestamp());
        assertNull(biometricData.getHeartRate());
        assertNull(biometricData.getSleepMinutes());
        assertNull(biometricData.getSleepQuality());
        assertNull(biometricData.getSkinTemperature());
    }
    
    @Test
    public void testConstructorWithAllFields() {
        Integer heartRate = 72;
        Integer sleepMinutes = 480;
        Double sleepQuality = 0.85;
        Double skinTemperature = 36.5;
        
        BiometricData data = new BiometricData(
            testTimestamp, heartRate, sleepMinutes, sleepQuality, skinTemperature
        );
        
        assertEquals(testTimestamp, data.getTimestamp());
        assertEquals(heartRate, data.getHeartRate());
        assertEquals(sleepMinutes, data.getSleepMinutes());
        assertEquals(sleepQuality, data.getSleepQuality());
        assertEquals(skinTemperature, data.getSkinTemperature());
    }
    
    @Test
    public void testSettersAndGetters() {
        Integer heartRate = 75;
        Integer sleepMinutes = 420;
        Double sleepQuality = 0.9;
        Double skinTemperature = 37.0;
        
        biometricData.setHeartRate(heartRate);
        biometricData.setSleepMinutes(sleepMinutes);
        biometricData.setSleepQuality(sleepQuality);
        biometricData.setSkinTemperature(skinTemperature);
        
        assertEquals(heartRate, biometricData.getHeartRate());
        assertEquals(sleepMinutes, biometricData.getSleepMinutes());
        assertEquals(sleepQuality, biometricData.getSleepQuality());
        assertEquals(skinTemperature, biometricData.getSkinTemperature());
    }
    
    @Test
    public void testTimestampUpdate() {
        Date newTimestamp = new Date(System.currentTimeMillis() + 1000);
        biometricData.setTimestamp(newTimestamp);
        assertEquals(newTimestamp, biometricData.getTimestamp());
    }
    
    @Test
    public void testNullValues() {
        biometricData.setHeartRate(null);
        biometricData.setSleepMinutes(null);
        biometricData.setSleepQuality(null);
        biometricData.setSkinTemperature(null);
        
        assertNull(biometricData.getHeartRate());
        assertNull(biometricData.getSleepMinutes());
        assertNull(biometricData.getSleepQuality());
        assertNull(biometricData.getSkinTemperature());
    }
}

