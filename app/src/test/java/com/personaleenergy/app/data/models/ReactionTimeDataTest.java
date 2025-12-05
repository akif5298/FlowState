package com.personaleenergy.app.data.models;

import com.flowstate.app.data.models.ReactionTimeData;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import java.util.Date;

/**
 * Unit tests for ReactionTimeData model
 */
public class ReactionTimeDataTest {
    
    private Date testTimestamp;
    
    @Before
    public void setUp() {
        testTimestamp = new Date();
    }
    
    @Test
    public void testConstructorWithBasicFields() {
        ReactionTimeData data = new ReactionTimeData(testTimestamp, 250);
        
        assertEquals(testTimestamp, data.getTimestamp());
        assertEquals(250, data.getReactionTimeMs());
        assertEquals("visual", data.getTestType()); // default
        assertEquals(Integer.valueOf(1), data.getAttempts()); // default
    }
    
    @Test
    public void testConstructorWithAllFields() {
        ReactionTimeData data = new ReactionTimeData(
            testTimestamp, 250, "audio", 5, 245.5
        );
        
        assertEquals(testTimestamp, data.getTimestamp());
        assertEquals(250, data.getReactionTimeMs());
        assertEquals("audio", data.getTestType());
        assertEquals(Integer.valueOf(5), data.getAttempts());
        assertEquals(Double.valueOf(245.5), data.getAverageReactionTimeMs());
    }
    
    @Test
    public void testConstructorWithNullTestType() {
        ReactionTimeData data = new ReactionTimeData(
            testTimestamp, 250, null, 3, 240.0
        );
        
        assertEquals("visual", data.getTestType()); // should default to "visual"
    }
    
    @Test
    public void testSettersAndGetters() {
        ReactionTimeData data = new ReactionTimeData(testTimestamp, 250);
        
        data.setReactionTimeMs(300);
        data.setTestType("tactile");
        data.setAttempts(10);
        data.setAverageReactionTimeMs(295.0);
        
        assertEquals(300, data.getReactionTimeMs());
        assertEquals("tactile", data.getTestType());
        assertEquals(Integer.valueOf(10), data.getAttempts());
        assertEquals(Double.valueOf(295.0), data.getAverageReactionTimeMs());
    }
}

