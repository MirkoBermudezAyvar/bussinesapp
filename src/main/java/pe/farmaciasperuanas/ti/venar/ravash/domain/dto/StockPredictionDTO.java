package pe.farmaciasperuanas.ti.venar.ravash.domain.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDate;

@Data
@Builder
public class StockPredictionDTO {

    private Long productId;
    private String productName;
    private String sku;

    private Integer currentStock;
    private Double predictedDemand7Days;
    private Double predictedDemand30Days;

    private LocalDate stockoutDate; // fecha estimada de quiebre de stock
    private Integer daysUntilStockout;

    private Integer recommendedOrderQuantity;
    private LocalDate recommendedOrderDate;

    private Double confidenceLevel;
    private String urgency; // IMMEDIATE, HIGH, MEDIUM, LOW
}