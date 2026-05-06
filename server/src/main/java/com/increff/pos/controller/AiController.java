package com.increff.pos.controller;

import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.dto.AiDto;
import com.increff.pos.model.data.AiInsightData;
import com.increff.pos.model.data.PredictionData;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ai")
public class AiController {

    @Autowired
    private AiDto aiDto;

    /**
     * Returns AI-generated insights about today's sales performance.
     * Calls the Groq LLM API with today's KPIs and returns a summary + recommendations.
     */
    @RequestMapping(value = "/insights", method = RequestMethod.GET)
    public AiInsightData getInsights() throws ApiException {
        return aiDto.getInsights();
    }

    /**
     * Returns demand predictions for all products sold in the last 30 days.
     * Runs the Python ML script to compute avg daily sales, predicted 7-day demand,
     * and days of stock remaining for each product.
     */
    @RequestMapping(value = "/predict", method = RequestMethod.GET)
    public PredictionData getPredictions() throws ApiException {
        return aiDto.getPredictions();
    }
}