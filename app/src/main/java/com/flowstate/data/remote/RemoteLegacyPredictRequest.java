package com.flowstate.data.remote;

import java.util.List;

/** Legacy single-series request: {history: [...], forecast_horizon: N} */
public class RemoteLegacyPredictRequest {
    public List<Double> history;
    public Integer forecast_horizon; // optional; defaults to 12 server-side

    public RemoteLegacyPredictRequest(List<Double> history, Integer horizon) {
        this.history = history;
        this.forecast_horizon = horizon;
    }
}
