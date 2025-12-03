package com.flowstate.data.remote;

import java.util.List;

/** Response shape from the backend. */
public class RemotePredictResponse {
    public String status;
    public String mode; // sample | local | success
    public List<Double> forecast; // main output when present
}
