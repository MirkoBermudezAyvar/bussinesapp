package pe.farmaciasperuanas.ti.venar.ravash.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.PredictionResult;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.Product;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MLIntegrationService {

    private final WebClient.Builder webClientBuilder;

    @Value("${ml.service.url:http://localhost:5000}")
    private String mlServiceUrl;

    /**
     * Llama al servicio Python ML para obtener predicciones reales
     */
    public Mono<PredictionResult> getPredictionFromML(Product product, int currentStock) {

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("product_id", product.getId());

        Map<String, Object> productData = new HashMap<>();
        productData.put("avg_daily_sales", product.getAverageDailySales() != null ? product.getAverageDailySales() : 10);
        productData.put("sales_variability", product.getSalesVariability() != null ? product.getSalesVariability() : 0.2);
        productData.put("lead_time_days", product.getLeadTimeDays() != null ? product.getLeadTimeDays() : 7);
        productData.put("current_stock", currentStock);
        productData.put("sale_price", product.getSalePrice());
        productData.put("cost_price", product.getCostPrice());

        requestBody.put("product_data", productData);
        requestBody.put("days_ahead", 30);

        return webClientBuilder.build()
                .post()
                .uri(mlServiceUrl + "/predict")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(MLPredictionResponse.class)
                .map(response -> mapToPredictionResult(response, product))
                .doOnSuccess(result -> log.info("ML prediction received for product {}: {} units for 7 days",
                        product.getSku(), result.getPredictedDemandNext7Days()))
                .doOnError(error -> log.error("Error calling ML service: {}", error.getMessage()))
                .onErrorResume(error -> {
                    log.warn("Falling back to basic prediction for product {}", product.getSku());
                    return Mono.empty(); // El servicio principal usará la predicción simulada
                });
    }

    /**
     * Entrena el modelo ML con datos históricos
     */
    public Mono<Void> trainModel(Long productId, List<Map<String, Object>> historicalData) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("product_id", productId);
        requestBody.put("historical_data", historicalData);

        return webClientBuilder.build()
                .post()
                .uri(mlServiceUrl + "/train")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnSuccess(response -> log.info("Model trained successfully for product {}", productId))
                .then();
    }

    /**
     * Detecta anomalías en las ventas recientes
     */
    public Mono<Map> detectAnomalies(Long productId, List<Double> recentSales) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("product_id", productId);
        requestBody.put("recent_sales", recentSales);

        return webClientBuilder.build()
                .post()
                .uri(mlServiceUrl + "/anomaly")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(error -> log.error("Error detecting anomalies: {}", error.getMessage()))
                .onErrorReturn(new HashMap<>());
    }

    /**
     * Análisis de estacionalidad
     */
    public Mono<Map> getSeasonalAnalysis(List<Map<String, Object>> salesData) {
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("sales_data", salesData);

        return webClientBuilder.build()
                .post()
                .uri(mlServiceUrl + "/seasonal_analysis")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(Map.class)
                .doOnError(error -> log.error("Error in seasonal analysis: {}", error.getMessage()))
                .onErrorReturn(new HashMap<>());
    }

    private PredictionResult mapToPredictionResult(MLPredictionResponse response, Product product) {
        Map<String, Object> summary = response.getSummary();
        Map<String, Object> recommendations = response.getRecommendations();
        Map<String, Object> metrics = response.getModel_metrics();

        return PredictionResult.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .predictedDemandNext7Days((Double) summary.get("predicted_demand_7_days"))
                .predictedDemandNext30Days((Double) summary.get("predicted_demand_30_days"))
                .confidenceLevel((Double) summary.get("confidence_level"))
                .recommendedOrderQuantity((Integer) recommendations.get("recommended_order_quantity"))
                .recommendedOrderDate(LocalDate.parse((String) recommendations.get("recommended_order_date")))
                .estimatedStockoutRisk(calculateStockoutRisk(summary))
                .demandTrend(determineTrend(response.getPredictions()))
                .predictionMethod((String) metrics.get("method"))
                .predictionDate(LocalDate.now())
                .build();
    }

    private double calculateStockoutRisk(Map<String, Object> summary) {
        Integer daysUntilStockout = (Integer) summary.get("days_until_stockout");
        if (daysUntilStockout == null) return 0.0;
        if (daysUntilStockout <= 7) return 90.0;
        if (daysUntilStockout <= 14) return 60.0;
        if (daysUntilStockout <= 30) return 30.0;
        return 10.0;
    }

    private String determineTrend(List<Map<String, Object>> predictions) {
        if (predictions == null || predictions.size() < 7) return "STABLE";

        // Comparar primera semana con última semana
        double firstWeekAvg = predictions.subList(0, 7).stream()
                .mapToDouble(p -> (Double) p.get("predicted_demand"))
                .average()
                .orElse(0);

        double lastWeekAvg = predictions.subList(Math.max(0, predictions.size() - 7), predictions.size()).stream()
                .mapToDouble(p -> (Double) p.get("predicted_demand"))
                .average()
                .orElse(0);

        double changePercent = (lastWeekAvg - firstWeekAvg) / firstWeekAvg * 100;

        if (changePercent > 10) return "INCREASING";
        if (changePercent < -10) return "DECREASING";
        return "STABLE";
    }

    // Clase interna para mapear respuesta del servicio ML
    @lombok.Data
    private static class MLPredictionResponse {
        private Long product_id;
        private List<Map<String, Object>> predictions;
        private Map<String, Object> summary;
        private Map<String, Object> recommendations;
        private Map<String, Object> model_metrics;
    }
}