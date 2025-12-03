package com.flowstate.app.data.models;

import java.util.Date;

/**
 * Represents reaction time test data
 * Maps to reaction_time_tests table in Supabase
 */
public class ReactionTimeData {
    private Date timestamp;
    private int reactionTimeMs;
    private String testType; // 'visual', 'audio', 'tactile'
    private Integer attempts; // number of attempts
    private Double averageReactionTimeMs; // average if multiple attempts

    public ReactionTimeData(Date timestamp, int reactionTimeMs) {
        this.timestamp = timestamp;
        this.reactionTimeMs = reactionTimeMs;
        this.testType = "visual"; // default
        this.attempts = 1;
    }
    
    public ReactionTimeData(Date timestamp, int reactionTimeMs, String testType, 
                          Integer attempts, Double averageReactionTimeMs) {
        this.timestamp = timestamp;
        this.reactionTimeMs = reactionTimeMs;
        this.testType = testType != null ? testType : "visual";
        this.attempts = attempts != null ? attempts : 1;
        this.averageReactionTimeMs = averageReactionTimeMs;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getReactionTimeMs() {
        return reactionTimeMs;
    }

    public void setReactionTimeMs(int reactionTimeMs) {
        this.reactionTimeMs = reactionTimeMs;
    }
    
    public String getTestType() {
        return testType;
    }
    
    public void setTestType(String testType) {
        this.testType = testType;
    }
    
    public Integer getAttempts() {
        return attempts;
    }
    
    public void setAttempts(Integer attempts) {
        this.attempts = attempts;
    }
    
    public Double getAverageReactionTimeMs() {
        return averageReactionTimeMs;
    }
    
    public void setAverageReactionTimeMs(Double averageReactionTimeMs) {
        this.averageReactionTimeMs = averageReactionTimeMs;
    }
}

