package com.personaleenergy.app.domain.features;

import java.time.LocalDateTime;

/**
 * Represents a single row of features for the prediction model.
 */
public class FeatureRow {
    // Dummy fields for placeholder implementation
    public double timeOfDay;
    public double dayOfWeek;
    public double lastNightSleepHours;
    public double avgHr;

    public FeatureRow(LocalDateTime dateTime) {
        // Dummy constructor to populate some basic time-based features
        this.timeOfDay = dateTime.getHour();
        this.dayOfWeek = dateTime.getDayOfWeek().getValue();
    }
}
