package com.personaleenergy.app.data.models;

import com.flowstate.app.data.models.TypingSpeedData;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * Unit tests for TypingSpeedData model
 */
public class TypingSpeedDataTest {
    
    private Date testTimestamp;
    
    @Before
    public void setUp() {
        testTimestamp = new Date();
    }
    
    @Test
    public void testConstructorWithBasicFields() {
        TypingSpeedData data = new TypingSpeedData(
            testTimestamp, 60, 95.5, "Sample text"
        );
        
        assertEquals(testTimestamp, data.getTimestamp());
        assertEquals(60, data.getWordsPerMinute());
        assertEquals(95.5, data.getAccuracy(), 0.001);
        assertEquals("Sample text", data.getSampleText());
    }
    
    @Test
    public void testConstructorWithAllFields() {
        TypingSpeedData data = new TypingSpeedData(
            testTimestamp, 60, 95.5, "Sample text", 100, 5, 120
        );
        
        assertEquals(testTimestamp, data.getTimestamp());
        assertEquals(60, data.getWordsPerMinute());
        assertEquals(95.5, data.getAccuracy(), 0.001);
        assertEquals("Sample text", data.getSampleText());
        assertEquals(Integer.valueOf(100), data.getTotalCharacters());
        assertEquals(Integer.valueOf(5), data.getErrors());
        assertEquals(Integer.valueOf(120), data.getDurationSeconds());
    }
    
    @Test
    public void testSettersAndGetters() {
        TypingSpeedData data = new TypingSpeedData(testTimestamp, 50, 90.0, "Test");
        
        data.setWordsPerMinute(70);
        data.setAccuracy(98.0);
        data.setSampleText("New text");
        data.setTotalCharacters(200);
        data.setErrors(2);
        data.setDurationSeconds(180);
        
        assertEquals(70, data.getWordsPerMinute());
        assertEquals(98.0, data.getAccuracy(), 0.001);
        assertEquals("New text", data.getSampleText());
        assertEquals(Integer.valueOf(200), data.getTotalCharacters());
        assertEquals(Integer.valueOf(2), data.getErrors());
        assertEquals(Integer.valueOf(180), data.getDurationSeconds());
    }
}

