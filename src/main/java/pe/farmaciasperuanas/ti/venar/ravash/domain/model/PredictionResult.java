package pe.farmaciasperuanas.ti.venar.ravash.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PredictionResult {

    private Long productId;
    private String productName;
    private String sku;

    // Predicciones
    private Double predictedDemandNext7Days;
    private Double predictedDemandNext30Days;
    private Double confidenceLevel; // 0-100%

    // Recomendaciones
    private Integer recommendedOrderQuantity;
    private LocalDate recommendedOrderDate;
    private Double estimatedStockoutRisk; // 0-100%

    // Análisis
    private String demandTrend; // INCREASING, STABLE, DECREASING
    private Map<String, Double> seasonalFactors;
    private String predictionMethod; // ARIMA, ML, MOVING_AVERAGE

    private LocalDate predictionDate;
}