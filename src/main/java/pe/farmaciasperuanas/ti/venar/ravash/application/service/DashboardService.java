package pe.farmaciasperuanas.ti.venar.ravash.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.farmaciasperuanas.ti.venar.ravash.domain.dto.DashboardDTO;
import pe.farmaciasperuanas.ti.venar.ravash.domain.dto.ProductDTO;
import pe.farmaciasperuanas.ti.venar.ravash.domain.dto.StockPredictionDTO;
import pe.farmaciasperuanas.ti.venar.ravash.domain.dto.AlertSummary;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.AlertRepository;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.InventoryRepository;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.ProductRepository;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final AlertRepository alertRepository;
    private final ProductService productService;
    private final PredictionService predictionService;

    public Mono<DashboardDTO> getDashboardData() {
        log.info("Generating dashboard data...");

        return Mono.zip(
                getGeneralMetrics(),
                getAlertMetrics(),
                getCriticalProducts(),
                getPredictions()
        ).map(tuple -> {
            var metrics = tuple.getT1();
            var alerts = tuple.getT2();
            var critical = tuple.getT3();
            var predictions = tuple.getT4();

            return DashboardDTO.builder()
                    .totalProducts(metrics.totalProducts)
                    .totalWarehouses(metrics.totalWarehouses)
                    .totalInventoryValue(metrics.totalValue)
                    .averageStockHealth(metrics.avgHealth)
                    .criticalAlerts(alerts.critical)
                    .warningAlerts(alerts.warning)
                    .recentAlerts(alerts.recent)
                    .lowStockProducts(critical.lowStock)
                    .overstockProducts(critical.overstock)
                    .expiringProducts(critical.expiring)
                    .salesTrend(generateSalesTrend())
                    .stockMovements(generateStockMovements())
                    .urgentPredictions(predictions)
                    .build();
        });
    }

    public Mono<DashboardDTO> getDashboardByWarehouse(Long warehouseId) {
        // Similar implementation but filtered by warehouse
        return getDashboardData(); // Simplified for prototype
    }

    private Mono<GeneralMetrics> getGeneralMetrics() {
        return Mono.zip(
                productRepository.count(),
                Mono.just(3L), // Hardcoded warehouses for prototype
                inventoryRepository.calculateTotalInventoryValue()
                        .defaultIfEmpty(0.0),
                calculateAverageStockHealth()
        ).map(tuple -> new GeneralMetrics(
                tuple.getT1().intValue(),
                tuple.getT2().intValue(),
                BigDecimal.valueOf(tuple.getT3()),
                tuple.getT4()
        ));
    }

    private Mono<AlertMetrics> getAlertMetrics() {
        return Mono.zip(
                alertRepository.countByResolvedFalseAndSeverity("HIGH")
                        .defaultIfEmpty(0L),
                alertRepository.countByResolvedFalseAndSeverity("MEDIUM")
                        .defaultIfEmpty(0L),
                alertRepository.findRecentAlerts(LocalDateTime.now().minusDays(1))
                        .take(5)
                        .map(alert -> AlertSummary.builder()
                                .id(alert.getId())
                                .type(alert.getType())
                                .severity(alert.getSeverity())
                                .message(alert.getMessage())
                                .createdAt(alert.getCreatedAt())
                                .build())
                        .collectList()
        ).map(tuple -> new AlertMetrics(
                tuple.getT1().intValue(),
                tuple.getT2().intValue(),
                tuple.getT3()
        ));
    }

    private Mono<CriticalProducts> getCriticalProducts() {
        return Mono.zip(
                productService.getLowStockProducts().take(5).collectList(),
                productService.getOverstockProducts().take(5).collectList(),
                productRepository.findExpiringProducts()
                        .flatMap(p -> productService.getProduct(p.getId()))
                        .take(5)
                        .collectList()
        ).map(tuple -> new CriticalProducts(
                tuple.getT1(),
                tuple.getT2(),
                tuple.getT3()
        ));
    }

    private Mono<java.util.List<StockPredictionDTO>> getPredictions() {
        return predictionService.getUrgentPredictions()
                .take(10)
                .collectList();
    }

    private Mono<Double> calculateAverageStockHealth() {
        return productService.getAllProducts()
                .map(ProductDTO::getStockHealth)
                .filter(health -> health != null)
                .collectList()
                .map(healthList -> {
                    if (healthList.isEmpty()) return 75.0;
                    return healthList.stream()
                            .mapToDouble(Double::doubleValue)
                            .average()
                            .orElse(75.0);
                });
    }

    private Map<String, Double> generateSalesTrend() {
        // Simulación de tendencia de ventas últimos 7 días
        Map<String, Double> trend = new HashMap<>();
        LocalDateTime now = LocalDateTime.now();

        for (int i = 6; i >= 0; i--) {
            String date = now.minusDays(i).toLocalDate().toString();
            // Simular ventas con variación
            double baseSales = 50000 + (Math.random() * 20000);
            trend.put(date, baseSales);
        }

        return trend;
    }

    private Map<String, Integer> generateStockMovements() {
        // Simulación de movimientos de stock
        Map<String, Integer> movements = new HashMap<>();
        movements.put("entradas", 150 + (int)(Math.random() * 50));
        movements.put("salidas", 120 + (int)(Math.random() * 80));
        movements.put("ajustes", 5 + (int)(Math.random() * 10));
        movements.put("devoluciones", (int)(Math.random() * 20));

        return movements;
    }

    // Clases internas para agrupar datos
    private record GeneralMetrics(
            Integer totalProducts,
            Integer totalWarehouses,
            BigDecimal totalValue,
            Double avgHealth
    ) {}

    private record AlertMetrics(
            Integer critical,
            Integer warning,
            java.util.List<AlertSummary> recent
    ) {}

    private record CriticalProducts(
            java.util.List<ProductDTO> lowStock,
            java.util.List<ProductDTO> overstock,
            java.util.List<ProductDTO> expiring
    ) {}
}