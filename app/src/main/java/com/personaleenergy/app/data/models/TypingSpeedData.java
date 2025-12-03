package com.flowstate.app.data.models;

import java.util.Date;

/**
 * Represents cognitive performance data
 * Maps to typing_speed_tests table in Supabase
 */
public class TypingSpeedData {
    private Date timestamp;
    private int wordsPerMinute;
    private double accuracy; // percentage 0-100
    private String sampleText;
    private Integer totalCharacters; // total_characters
    private Integer errors; // errors count
    private Integer durationSeconds; // duration_seconds

    public TypingSpeedData(Date timestamp, int wordsPerMinute, double accuracy, String sampleText) {
        this.timestamp = timestamp;
        this.wordsPerMinute = wordsPerMinute;
        this.accuracy = accuracy;
        this.sampleText = sampleText;
    }
    
    public TypingSpeedData(Date timestamp, int wordsPerMinute, double accuracy, String sampleText,
                          Integer totalCharacters, Integer errors, Integer durationSeconds) {
        this.timestamp = timestamp;
        this.wordsPerMinute = wordsPerMinute;
        this.accuracy = accuracy;
        this.sampleText = sampleText;
        this.totalCharacters = totalCharacters;
        this.errors = errors;
        this.durationSeconds = durationSeconds;
    }

    // Getters and Setters
    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getWordsPerMinute() {
        return wordsPerMinute;
    }

    public void setWordsPerMinute(int wordsPerMinute) {
        this.wordsPerMinute = wordsPerMinute;
    }

    public double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(double accuracy) {
        this.accuracy = accuracy;
    }

    public String getSampleText() {
        return sampleText;
    }

    public void setSampleText(String sampleText) {
        this.sampleText = sampleText;
    }
    
    public Integer getTotalCharacters() {
        return totalCharacters;
    }
    
    public void setTotalCharacters(Integer totalCharacters) {
        this.totalCharacters = totalCharacters;
    }
    
    public Integer getErrors() {
        return errors;
    }
    
    public void setErrors(Integer errors) {
        this.errors = errors;
    }
    
    public Integer getDurationSeconds() {
        return durationSeconds;
    }
    
    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }
}

