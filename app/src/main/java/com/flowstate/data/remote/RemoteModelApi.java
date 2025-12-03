package com.flowstate.data.remote;

import java.util.List;
import java.util.Map;

import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.Header;
import retrofit2.http.POST;

/**
 * Retrofit API for the FlowState backend hosted on Railway.
 * Exposes the `/predict` endpoint.
 */
public interface RemoteModelApi {

    /**
     * Legacy single-series format: {"history": [..], "forecast_horizon": N}
     */
    @POST("/predict")
    Call<RemotePredictResponse> predictLegacy(
            @Body RemoteLegacyPredictRequest body,
            @Header("Authorization") String bearerTokenOptional
    );

    /**
     * Multivariate format:
     * {"past_values": {feature: [...], ...}, "timestamps": [...], "forecast_horizon": N}
     */
    @POST("/predict")
    Call<RemotePredictResponse> predictMultivariate(
            @Body RemoteMultivariatePredictRequest body,
            @Header("Authorization") String bearerTokenOptional
    );
}
