package com.increff.pos.model.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public class PredictionData {

    private final List<ProductPrediction> predictions;

    @Getter
    @AllArgsConstructor
    public static class ProductPrediction {

        private final Integer productId;
        private final String productName;
        private final int currentStock;
        private final double avgDailySales;
        private final double predictedDemand7Days;
        private final double daysOfStockRemaining;
        /** URGENT (<5 days stock), LOW (5-14 days), OK (>14 days) */
        private final String restockStatus;
    }
}