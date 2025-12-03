package com.flowstate.app.data.models;

import java.util.Date;

/**
 * Data model for reaction time test results
 */
public class ReactionTimeData {
    private Date timestamp;
    private Integer reactionTimeMs;

    public ReactionTimeData() {}

    public ReactionTimeData(Date timestamp, Integer reactionTimeMs) {
        this.timestamp = timestamp;
        this.reactionTimeMs = reactionTimeMs;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public Integer getReactionTimeMs() {
        return reactionTimeMs;
    }

    public void setReactionTimeMs(Integer reactionTimeMs) {
        this.reactionTimeMs = reactionTimeMs;
    }
}
