package com.flowstate.app.data.models;

import java.util.Date;

/**
 * Data model for typing speed test results
 */
public class TypingSpeedData {
    private Date timestamp;
    private Integer wordsPerMinute;
    private Double accuracy;
    private String sampleText;

    public TypingSpeedData() {}

    public TypingSpeedData(Date timestamp, Integer wordsPerMinute, Double accuracy, String sampleText) {
        this.timestamp = timestamp;
        this.wordsPerMinute = wordsPerMinute;
        this.accuracy = accuracy;
        this.sampleText = sampleText;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getWordsPerMinute() {
        return wordsPerMinute;
    }

    public void setWordsPerMinute(Integer wordsPerMinute) {
        this.wordsPerMinute = wordsPerMinute;
    }

    public Double getAccuracy() {
        return accuracy;
    }

    public void setAccuracy(Double accuracy) {
        this.accuracy = accuracy;
    }

    public String getSampleText() {
        return sampleText;
    }

    public void setSampleText(String sampleText) {
        this.sampleText = sampleText;
    }
}
