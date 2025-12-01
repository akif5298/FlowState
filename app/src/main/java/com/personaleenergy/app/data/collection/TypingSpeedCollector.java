package com.personaleenergy.app.data.collection;

import com.personaleenergy.app.data.models.TypingSpeedData;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class TypingSpeedCollector {
    private static final List<String> SAMPLE_TEXTS = Arrays.asList(
            "The quick brown fox jumps over the lazy dog",
            "Code is poetry made to be read and understood by humans",
            "Technology should augment human intelligence, not replace it",
            "Building software is an art that requires patience and creativity",
            "The best code is simple, clear, and easy to maintain"
    );

    private long startTime;
    private long endTime;
    private String typedText;
    private String currentSampleText;

    public String prepareTest() {
        // Just prepare sample text without starting timer
        Random random = new Random();
        currentSampleText = SAMPLE_TEXTS.get(random.nextInt(SAMPLE_TEXTS.size()));
        return currentSampleText;
    }
    
    public String startTest() {
        // Reset and prepare for new test
        startTime = 0;
        endTime = 0;
        typedText = "";
        return prepareTest();
    }

    public void recordTyping(String text) {
        if (startTime == 0) {
            startTime = System.currentTimeMillis();
        }
        typedText = text;
    }

    public TypingSpeedData finishTest() {
        endTime = System.currentTimeMillis();
        double timeInMinutes = (endTime - startTime) / 60000.0;
        int wordCount = typedText.trim().split("\\s+").length;
        int wordsPerMinute = (int) (wordCount / timeInMinutes);
        double accuracy = calculateAccuracy(currentSampleText, typedText);

        return new TypingSpeedData(new Date(), wordsPerMinute, accuracy, currentSampleText);
    }

    private double calculateAccuracy(String original, String typed) {
        if (original.isEmpty() || typed.isEmpty()) return 0.0;
        
        String[] originalWords = original.trim().split("\\s+");
        String[] typedWords = typed.trim().split("\\s+");
        int minLength = Math.min(originalWords.length, typedWords.length);
        
        int correctWords = 0;
        for (int i = 0; i < minLength; i++) {
            if (originalWords[i].equalsIgnoreCase(typedWords[i])) {
                correctWords++;
            }
        }
        
        return originalWords.length == 0 ? 0.0 : (correctWords * 100.0 / originalWords.length);
    }

    public boolean isTestComplete(String typedText) {
        return typedText.trim().length() >= currentSampleText.trim().length() * 0.8;
    }
}
