package com.personaleenergy.app.data.models;

import java.util.Date;

public class ReactionTimeData {
    private Date timestamp;
    private int reactionTimeMs;

    public ReactionTimeData(Date timestamp, int reactionTimeMs) {
        this.timestamp = timestamp;
        this.reactionTimeMs = reactionTimeMs;
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
}
