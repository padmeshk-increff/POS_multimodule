package com.increff.pos.flow;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.increff.pos.api.InventoryApi;
import com.increff.pos.api.OrderItemApi;
import com.increff.pos.api.ProductApi;
import com.increff.pos.commons.exception.ApiException;
import com.increff.pos.entity.Inventory;
import com.increff.pos.entity.Product;
import com.increff.pos.model.data.AiInsightData;
import com.increff.pos.model.data.PredictionData;
import com.increff.pos.model.data.SummaryData;
import com.increff.pos.model.result.ProductQuantityResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional(rollbackFor = ApiException.class)
public class AiFlow {

    @Value("${groq.api.key}")
    private String groqApiKey;

    @Value("${groq.api.url}")
    private String groqApiUrl;

    @Value("${groq.model}")
    private String groqModel;

    @Value("${ai.predict.script.path}")
    private String predictScriptPath;

    @Autowired private ReportFlow reportFlow;
    @Autowired private OrderItemApi orderItemApi;
    @Autowired private InventoryApi inventoryApi;
    @Autowired private ProductApi productApi;
    @Autowired private RestTemplate restTemplate;
    @Autowired private ObjectMapper objectMapper;

    public AiInsightData getInsights() throws ApiException {
        SummaryData summary = reportFlow.getSummaryData();
        String prompt = buildInsightPrompt(summary);
        String rawResponse = callGroqApi(prompt);
        return parseInsightResponse(rawResponse);
    }

    public PredictionData getPredictions() throws ApiException {
        List<ProductQuantityResult> salesData = orderItemApi.getAllTimeTopSellingProducts(null);

        if (salesData.isEmpty()) {
            return new PredictionData(Collections.emptyList());
        }

        // Calculate the actual span of days from the first ever order to today
        ZonedDateTime firstOrderDate = orderItemApi.getFirstOrderDate();
        long totalDays = firstOrderDate != null
                ? ChronoUnit.DAYS.between(firstOrderDate.toLocalDate(), LocalDate.now()) + 1
                : 1;

        List<Integer> productIds = salesData.stream()
                .map(ProductQuantityResult::getProductId)
                .collect(Collectors.toList());

        List<Product> products = productApi.getByIds(productIds);
        Map<Integer, String> nameMap = products.stream()
                .collect(Collectors.toMap(Product::getId, Product::getName));

        List<Inventory> inventories = inventoryApi.getByProductIds(productIds);
        Map<Integer, Integer> stockMap = inventories.stream()
                .collect(Collectors.toMap(Inventory::getProductId, Inventory::getQuantity));

        List<Map<String, Object>> scriptInput = new ArrayList<>();
        for (ProductQuantityResult s : salesData) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("productId", s.getProductId());
            item.put("productName", nameMap.getOrDefault(s.getProductId(), "Unknown"));
            item.put("totalSoldAllTime", s.getTotalQuantity());
            item.put("totalDays", totalDays);
            item.put("currentStock", stockMap.getOrDefault(s.getProductId(), 0));
            scriptInput.add(item);
        }

        return runPredictScript(scriptInput);
    }

    private String buildInsightPrompt(SummaryData summary) {
        StringBuilder sb = new StringBuilder();
        sb.append("Today's POS performance:\n");
        sb.append(String.format("- Revenue: %.2f (%.1f%% vs yesterday)\n",
                summary.getTodaySales().getCurrent(), summary.getTodaySales().getChangePercent()));
        sb.append(String.format("- Orders: %.0f (%.1f%% vs yesterday)\n",
                summary.getTodayOrders().getCurrent(), summary.getTodayOrders().getChangePercent()));
        sb.append(String.format("- Avg Order Value: %.2f (%.1f%% vs yesterday)\n",
                summary.getAverageOrderValue().getCurrent(), summary.getAverageOrderValue().getChangePercent()));

        sb.append("- Top Products: ");
        for (SummaryData.ProductSalesData p : summary.getTopSellingProducts()) {
            sb.append(p.getProductName()).append(" (qty:").append(p.getQuantitySold()).append("), ");
        }
        sb.append("\n- Low Stock: ");
        List<SummaryData.LowStockAlertData> alerts = summary.getLowStockAlerts();
        if (alerts.isEmpty()) {
            sb.append("None");
        } else {
            for (SummaryData.LowStockAlertData l : alerts) {
                sb.append(l.getProductName()).append(" (").append(l.getCurrentStock()).append(" left), ");
            }
        }
        sb.append("\n\nGive a 2-3 sentence performance summary and 3 actionable recommendations.");
        sb.append("\nRespond ONLY with this JSON format: {\"summary\":\"...\",\"recommendations\":[\"...\",\"...\",\"...\"]}");
        return sb.toString();
    }

    private String callGroqApi(String userMessage) throws ApiException {
        try {
            Map<String, Object> sysMsg = new LinkedHashMap<>();
            sysMsg.put("role", "system");
            sysMsg.put("content", "You are a concise POS analytics assistant. Always respond with valid JSON only, no markdown.");

            Map<String, Object> userMsg = new LinkedHashMap<>();
            userMsg.put("role", "user");
            userMsg.put("content", userMessage);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", groqModel);
            body.put("messages", Arrays.asList(sysMsg, userMsg));
            body.put("temperature", 0.5);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + groqApiKey);
            headers.set("User-Agent", "Mozilla/5.0 (compatible; POS-App/1.0)");

            HttpEntity<String> entity = new HttpEntity<>(objectMapper.writeValueAsString(body), headers);
            String response = restTemplate.postForObject(groqApiUrl, entity, String.class);

            JsonNode root = objectMapper.readTree(response);
            return root.path("choices").get(0).path("message").path("content").asText();
        } catch (Exception e) {
            throw new ApiException("Groq API call failed: " + e.getMessage());
        }
    }

    private AiInsightData parseInsightResponse(String json) throws ApiException {
        try {
            JsonNode node = objectMapper.readTree(json);
            String summary = node.path("summary").asText();
            List<String> recommendations = new ArrayList<>();
            node.path("recommendations").forEach(r -> recommendations.add(r.asText()));
            return new AiInsightData(summary, recommendations);
        } catch (Exception e) {
            return new AiInsightData(json, Collections.emptyList());
        }
    }

    private PredictionData runPredictScript(List<Map<String, Object>> inputData) throws ApiException {
        try {
            String inputJson = objectMapper.writeValueAsString(inputData);

            ProcessBuilder pb = new ProcessBuilder("python3", predictScriptPath);
            pb.redirectErrorStream(true);
            Process process = pb.start();

            try (OutputStream os = process.getOutputStream()) {
                os.write(inputJson.getBytes("UTF-8"));
            }

            StringBuilder output = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    output.append(line);
                }
            }

            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new ApiException("Prediction script failed: " + output.toString());
            }

            return parsePredictionOutput(output.toString());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ApiException("Failed to run prediction script: " + e.getMessage());
        }
    }

    private PredictionData parsePredictionOutput(String json) throws ApiException {
        try {
            JsonNode root = objectMapper.readTree(json);
            List<PredictionData.ProductPrediction> predictions = new ArrayList<>();
            for (JsonNode node : root.path("predictions")) {
                predictions.add(new PredictionData.ProductPrediction(
                        node.path("productId").asInt(),
                        node.path("productName").asText(),
                        node.path("currentStock").asInt(),
                        node.path("avgDailySales").asDouble(),
                        node.path("predictedDemand7Days").asDouble(),
                        node.path("daysOfStockRemaining").asDouble(),
                        node.path("restockStatus").asText()
                ));
            }
            return new PredictionData(predictions);
        } catch (Exception e) {
            throw new ApiException("Failed to parse prediction output: " + e.getMessage());
        }
    }
}