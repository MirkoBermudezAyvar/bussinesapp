package pe.farmaciasperuanas.ti.venar.ravash.domain.dto;

import lombok.Data;
import lombok.Builder;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class DashboardDTO {

    // Métricas generales
    private Integer totalProducts;
    private Integer totalWarehouses;
    private BigDecimal totalInventoryValue;
    private Double averageStockHealth;

    // Alertas
    private Integer criticalAlerts;
    private Integer warningAlerts;
    private List<AlertSummary> recentAlerts;

    // Productos críticos
    private List<ProductDTO> lowStockProducts;
    private List<ProductDTO> overstockProducts;
    private List<ProductDTO> expiringProducts;

    // Tendencias
    private Map<String, Double> salesTrend; // últimos 7 días
    private Map<String, Integer> stockMovements; // entradas vs salidas

    // Predicciones destacadas
    private List<StockPredictionDTO> urgentPredictions;
}