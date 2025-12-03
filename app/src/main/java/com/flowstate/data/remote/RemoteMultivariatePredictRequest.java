package com.flowstate.data.remote;

import java.util.List;
import java.util.Map;

/** New multivariate request format. */
public class RemoteMultivariatePredictRequest {
    public Map<String, List<Double>> past_values;
    public List<String> timestamps; // optional
    public Integer forecast_horizon; // optional

    public RemoteMultivariatePredictRequest(Map<String, List<Double>> pastValues,
                                            List<String> timestamps,
                                            Integer horizon) {
        this.past_values = pastValues;
        this.timestamps = timestamps;
        this.forecast_horizon = horizon;
    }
}
