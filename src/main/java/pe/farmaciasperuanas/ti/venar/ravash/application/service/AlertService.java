package pe.farmaciasperuanas.ti.venar.ravash.application.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.Alert;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.Inventory;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.Product;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.AlertRepository;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.InventoryRepository;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AlertService {

    private final AlertRepository alertRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final PredictionService predictionService;

    public Flux<Alert> getActiveAlerts() {
        return alertRepository.findByResolvedFalseOrderByCreatedAtDesc();
    }

    public Flux<Alert> getAlertsByProduct(Long productId) {
        return alertRepository.findByProductIdAndResolvedFalse(productId);
    }

    public Flux<Alert> getCriticalAlerts() {
        return alertRepository.findBySeverityAndResolvedFalse("HIGH");
    }

    public Mono<Alert> createAlert(Alert alert) {
        alert.setCreatedAt(LocalDateTime.now());
        alert.setResolved(false);
        return alertRepository.save(alert)
                .doOnSuccess(saved -> log.info("Alert created: {} - {}", saved.getType(), saved.getMessage()));
    }

    public Mono<Void> resolveAlert(Long alertId, String resolvedBy) {
        return alertRepository.resolveAlert(alertId, resolvedBy)
                .doOnSuccess(v -> log.info("Alert {} resolved by {}", alertId, resolvedBy));
    }

    @Scheduled(fixedDelay = 300000) // Cada 5 minutos
    public void checkAndGenerateAlerts() {
        log.info("Running scheduled alert check...");

        // Verificar productos con stock bajo
        checkLowStockProducts()
                .then(checkOverstockProducts())
                .then(checkExpiringProducts())
                .then(checkAnomalies())
                .subscribe(
                        null,
                        error -> log.error("Error in alert generation", error),
                        () -> log.info("Alert check completed")
                );
    }

    private Mono<Void> checkLowStockProducts() {
        return productRepository.findLowStockProducts()
                .flatMap(product -> inventoryRepository.findByProductId(product.getId())
                        .collectList()
                        .flatMap(inventories -> {
                            int totalStock = inventories.stream()
                                    .mapToInt(i -> i.getCurrentStock())
                                    .sum();

                            if (totalStock < product.getMinStock()) {
                                return createLowStockAlert(product, totalStock);
                            }
                            return Mono.empty();
                        }))
                .then();
    }

    private Mono<Void> checkOverstockProducts() {
        return productRepository.findOverstockProducts()
                .flatMap(product -> inventoryRepository.findByProductId(product.getId())
                        .collectList()
                        .flatMap(inventories -> {
                            int totalStock = inventories.stream()
                                    .mapToInt(i -> i.getCurrentStock())
                                    .sum();

                            if (product.getMaxStock() != null && totalStock > product.getMaxStock()) {
                                return createOverstockAlert(product, totalStock);
                            }
                            return Mono.empty();
                        }))
                .then();
    }

    private Mono<Void> checkExpiringProducts() {
        return inventoryRepository.findAll()
                .filter(inv -> inv.getExpirationDate() != null)
                .filter(inv -> inv.getExpirationDate().isBefore(LocalDateTime.now().plusDays(7)))
                .flatMap(inventory -> productRepository.findById(inventory.getProductId())
                        .flatMap(product -> createExpirationAlert(product, inventory)))
                .then();
    }

    private Mono<Void> checkAnomalies() {
        // Detectar anomalías en patrones de consumo
        return productRepository.findByActiveTrue()
                .flatMap(product -> predictionService.predictStockForProduct(product.getId())
                        .filter(prediction -> prediction.getConfidenceLevel() < 60) // Baja confianza indica anomalía
                        .flatMap(prediction -> createAnomalyAlert(product, prediction)))
                .then();
    }

    private Mono<Alert> createLowStockAlert(Product product, int currentStock) {
        Alert alert = Alert.builder()
                .productId(product.getId())
                .type("LOW_STOCK")
                .severity(currentStock == 0 ? "HIGH" : "MEDIUM")
                .message(String.format("Stock bajo para %s (SKU: %s). Stock actual: %d, Mínimo: %d",
                        product.getName(), product.getSku(), currentStock, product.getMinStock()))
                .recommendation("Realizar pedido inmediato al proveedor")
                .build();

        return createAlert(alert);
    }

    private Mono<Alert> createOverstockAlert(Product product, int currentStock) {
        Alert alert = Alert.builder()
                .productId(product.getId())
                .type("OVERSTOCK")
                .severity("LOW")
                .message(String.format("Exceso de inventario para %s (SKU: %s). Stock actual: %d, Máximo: %d",
                        product.getName(), product.getSku(), currentStock, product.getMaxStock()))
                .recommendation("Considerar promociones o descuentos para reducir inventario")
                .build();

        return createAlert(alert);
    }

    private Mono<Alert> createExpirationAlert(Product product, Inventory inventory) {
        long daysUntilExpiration = java.time.Duration.between(
                LocalDateTime.now(), inventory.getExpirationDate()).toDays();

        Alert alert = Alert.builder()
                .productId(product.getId())
                .warehouseId(inventory.getWarehouseId())
                .type("EXPIRING")
                .severity(daysUntilExpiration <= 3 ? "HIGH" : "MEDIUM")
                .message(String.format("Producto próximo a vencer: %s (Lote: %s) en %d días",
                        product.getName(), inventory.getBatchNumber(), daysUntilExpiration))
                .recommendation("Priorizar venta o considerar devolución al proveedor")
                .build();

        return createAlert(alert);
    }

    private Mono<Alert> createAnomalyAlert(Product product, Object prediction) {
        Alert alert = Alert.builder()
                .productId(product.getId())
                .type("ANOMALY")
                .severity("MEDIUM")
                .message(String.format("Patrón de consumo anómalo detectado para %s (SKU: %s)",
                        product.getName(), product.getSku()))
                .recommendation("Revisar histórico de ventas y verificar datos")
                .build();

        return createAlert(alert);
    }
}

