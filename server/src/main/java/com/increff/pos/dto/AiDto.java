package com.increff.pos.dto;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.flow.AiFlow;
import com.increff.pos.model.data.AiInsightData;
import com.increff.pos.model.data.PredictionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class AiDto {

    @Autowired
    private AiFlow aiFlow;

    public AiInsightData getInsights() throws ApiException {
        return aiFlow.getInsights();
    }

    public PredictionData getPredictions() throws ApiException {
        return aiFlow.getPredictions();
    }
}