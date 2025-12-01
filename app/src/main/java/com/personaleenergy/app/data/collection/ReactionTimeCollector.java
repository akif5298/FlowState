package com.personaleenergy.app.data.collection;

import com.personaleenergy.app.data.models.ReactionTimeData;
import java.util.Date;
import java.util.Random;

public class ReactionTimeCollector {
    private long startTime;
    private boolean isWaiting;

    public void waitForColorChange(ColorChangeCallback callback, long delayMs) {
        isWaiting = true;
        Random random = new Random();
        long randomDelay = random.nextInt((int) delayMs) + 2000; // 2-5 seconds
        
        new Thread(() -> {
            try {
                Thread.sleep(randomDelay);
                startTime = System.currentTimeMillis();
                isWaiting = false;
                callback.onColorChange();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    public ReactionTimeData recordReaction() {
        long endTime = System.currentTimeMillis();
        int reactionTime = (int) (endTime - startTime);
        return new ReactionTimeData(new Date(), reactionTime);
    }

    public boolean isWaitingForColorChange() {
        return isWaiting;
    }

    public interface ColorChangeCallback {
        void onColorChange();
    }
}
