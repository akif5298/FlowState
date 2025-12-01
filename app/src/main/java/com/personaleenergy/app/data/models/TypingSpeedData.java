package com.personaleenergy.app.data.models;

import java.util.Date;

/**
 * Represents cognitive performance data
 */
public class TypingSpeedData {
    private Date timestamp;
    private int wordsPerMinute;
    private double accuracy; // percentage 0-100
    private String sampleText;

    public TypingSpeedData(Date timestamp, int wordsPerMinute, double accuracy, String sampleText) {
        this.timestamp = timestamp;
        this.wordsPerMinute = wordsPerMinute;
        this.accuracy = accuracy;
        this.sampleText = sampleText;
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
}
