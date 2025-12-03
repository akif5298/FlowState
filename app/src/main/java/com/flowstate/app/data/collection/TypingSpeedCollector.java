package com.flowstate.app.data.collection;

import com.flowstate.app.data.models.TypingSpeedData;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

public class TypingSpeedCollector {
    private static final List<String> WORD_LIST = Arrays.asList(
            "the", "be", "of", "and", "a", "to", "in", "he", "have", "it", "that", "for", "they", "i", "with", "as", "not", "on", "she", "at", "by", "this", "we", "you", "do", "but", "from", "or", "which", "one", "would", "all", "will", "there", "say", "who", "make", "when", "can", "more", "if", "no", "man", "out", "other", "so", "what", "time", "up", "go", "about", "than", "into", "could", "state", "only", "new", "year", "some", "take", "come", "these", "know", "see", "use", "get", "like", "then", "first", "any", "work", "now", "may", "such", "give", "over", "think", "most", "even", "find", "day", "also", "after", "way", "many", "must", "look", "before", "great", "back", "through", "long", "where", "much", "should", "well", "people", "down", "own", "just", "because", "good", "each", "those", "feel", "seem", "how", "high", "too", "place", "little", "world", "very", "still", "nation", "hand", "old", "life", "tell", "write", "become", "here", "show", "house", "both", "between", "need", "mean", "call", "develop", "under", "last", "right", "move", "thing", "general", "school", "never", "same", "another", "begin", "while", "number", "part", "turn", "real", "leave", "might", "want", "point", "form", "off", "child", "few", "small", "since", "against", "ask", "late", "home", "interest", "large", "person", "end", "open", "public", "follow", "during", "present", "without", "again", "hold", "govern", "around", "possible", "head", "consider", "word", "program", "problem", "however", "lead", "system", "set", "order", "eye", "plan", "run", "keep", "face", "fact", "group", "play", "stand", "increase", "early", "course", "change", "help", "line"
    );

    private long startTime;
    private long endTime;
    private String typedText;
    private String currentSampleText;

    public String prepareTest() {
        // Generate random words
        Random random = new Random();
        StringBuilder sb = new StringBuilder();
        int wordsToGenerate = 150; // Enough for fast typists (approx 150 WPM capacity)
        for (int i = 0; i < wordsToGenerate; i++) {
            sb.append(WORD_LIST.get(random.nextInt(WORD_LIST.size())));
            if (i < wordsToGenerate - 1) {
                sb.append(" ");
            }
        }
        currentSampleText = sb.toString();
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
        if (timeInMinutes <= 0) timeInMinutes = 0.0001; // Avoid division by zero
        
        int totalChars = typedText.length();
        int errors = calculateErrors(currentSampleText, typedText);
        
        // Net WPM = ((TotalChars / 5) - Errors) / Minutes
        double grossWPM = (totalChars / 5.0) / timeInMinutes;
        double errorRate = errors / timeInMinutes;
        int netWPM = (int) Math.max(0, grossWPM - errorRate);
        
        // If accuracy is too low, WPM should be penalized heavily
        // Standard typing tests calculate Net WPM this way
        
        double accuracy = calculateAccuracy(currentSampleText, typedText);

        return new TypingSpeedData(new Date(), netWPM, accuracy, currentSampleText);
    }

    private int calculateErrors(String original, String typed) {
        // Reuse the logic from activity if possible or duplicate it here for consistency
        // Ideally this logic should be in one place (Collector)
        int errors = 0;
        int minLength = Math.min(original.length(), typed.length());
        for (int i = 0; i < minLength; i++) {
            if (original.charAt(i) != typed.charAt(i)) {
                errors++;
            }
        }
        if (typed.length() > original.length()) errors += (typed.length() - original.length());
        if (original.length() > typed.length()) errors += (original.length() - typed.length());
        return errors;
    }

    private double calculateAccuracy(String original, String typed) {
        if (original.isEmpty() || typed.isEmpty()) return 0.0;
        
        // Character-based accuracy (more precise than word-based)
        int minLen = Math.min(original.length(), typed.length());
        int correctChars = 0;
        
        for (int i = 0; i < minLen; i++) {
            if (original.charAt(i) == typed.charAt(i)) {
                correctChars++;
            }
        }
        
        return original.length() == 0 ? 0.0 : (correctChars * 100.0 / original.length());
    }

    public boolean isTestComplete(String typedText) {
        // Not really used in time-based test, but keeping for compatibility
        return typedText.length() >= currentSampleText.length();
    }
}
