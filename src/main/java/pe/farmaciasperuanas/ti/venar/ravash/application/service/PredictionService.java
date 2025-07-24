package pe.farmaciasperuanas.ti.venar.ravash.application.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.farmaciasperuanas.ti.venar.ravash.domain.dto.StockPredictionDTO;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.PredictionResult;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.Product;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.InventoryRepository;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class PredictionService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    // Simulación de predicción con IA (en producción esto se conectaría a un servicio Python con ML)
    public Mono<StockPredictionDTO> predictStockForProduct(Long productId) {
        return productRepository.findById(productId)
                .zipWith(inventoryRepository.findByProductId(productId).collectList())
                .map(tuple -> {
                    Product product = tuple.getT1();
                    var inventories = tuple.getT2();

                    // Calcular stock actual total
                    int currentStock = inventories.stream()
                            .mapToInt(i -> i.getCurrentStock())
                            .sum();

                    // Simular predicción basada en datos históricos
                    PredictionResult prediction = simulatePrediction(product, currentStock);

                    return buildStockPredictionDTO(product, currentStock, prediction);
                });
    }

    public Flux<StockPredictionDTO> getUrgentPredictions() {
        return productRepository.findByActiveTrue()
                .flatMap(product -> predictStockForProduct(product.getId()))
                .filter(prediction -> "IMMEDIATE".equals(prediction.getUrgency()) ||
                        "HIGH".equals(prediction.getUrgency()))
                .sort((a, b) -> {
                    if (a.getDaysUntilStockout() == null) return 1;
                    if (b.getDaysUntilStockout() == null) return -1;
                    return a.getDaysUntilStockout().compareTo(b.getDaysUntilStockout());
                });
    }

    public Mono<PredictionResult> generateAdvancedPrediction(Long productId) {
        return productRepository.findById(productId)
                .map(product -> {
                    // Simulación de análisis avanzado
                    PredictionResult result = PredictionResult.builder()
                            .productId(product.getId())
                            .productName(product.getName())
                            .sku(product.getSku())
                            .predictedDemandNext7Days(calculateDemand(product, 7))
                            .predictedDemandNext30Days(calculateDemand(product, 30))
                            .confidenceLevel(85.0 + ThreadLocalRandom.current().nextDouble(10))
                            .recommendedOrderQuantity(calculateOrderQuantity(product))
                            .recommendedOrderDate(calculateOrderDate(product))
                            .estimatedStockoutRisk(calculateStockoutRisk(product))
                            .demandTrend(analyzeTrend(product))
                            .seasonalFactors(getSeasonalFactors(product))
                            .predictionMethod("HYBRID_ML_ARIMA")
                            .predictionDate(LocalDate.now())
                            .build();

                    log.info("Generated prediction for product {}: {} units for next 7 days",
                            product.getSku(), result.getPredictedDemandNext7Days());

                    return result;
                });
    }

    private PredictionResult simulatePrediction(Product product, int currentStock) {
        // Simulación basada en parámetros del producto
        double avgDailySales = product.getAverageDailySales() != null ?
                product.getAverageDailySales() : 10.0;

        double variability = product.getSalesVariability() != null ?
                product.getSalesVariability() : 0.2;

        // Agregar variabilidad aleatoria para simular demanda real
        double randomFactor = 1 + (ThreadLocalRandom.current().nextDouble() - 0.5) * variability;

        double demand7Days = avgDailySales * 7 * randomFactor;
        double demand30Days = avgDailySales * 30 * randomFactor *
                (1 + getSeasonalityFactor(product));

        return PredictionResult.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .predictedDemandNext7Days(demand7Days)
                .predictedDemandNext30Days(demand30Days)
                .confidenceLevel(calculateConfidence(product))
                .recommendedOrderQuantity(calculateOrderQuantity(product))
                .recommendedOrderDate(calculateOrderDate(product))
                .estimatedStockoutRisk(calculateStockoutRisk(currentStock, demand7Days))
                .demandTrend(analyzeTrend(product))
                .predictionMethod("MOVING_AVERAGE_ADJUSTED")
                .predictionDate(LocalDate.now())
                .build();
    }

    private StockPredictionDTO buildStockPredictionDTO(Product product, int currentStock,
                                                       PredictionResult prediction) {
        Integer daysUntilStockout = null;
        LocalDate stockoutDate = null;

        if (prediction.getPredictedDemandNext30Days() > 0) {
            double dailyDemand = prediction.getPredictedDemandNext30Days() / 30;
            if (dailyDemand > 0) {
                daysUntilStockout = (int) (currentStock / dailyDemand);
                if (daysUntilStockout < 365) {
                    stockoutDate = LocalDate.now().plusDays(daysUntilStockout);
                }
            }
        }

        String urgency = determineUrgency(daysUntilStockout, product.getLeadTimeDays());

        return StockPredictionDTO.builder()
                .productId(product.getId())
                .productName(product.getName())
                .sku(product.getSku())
                .currentStock(currentStock)
                .predictedDemand7Days(prediction.getPredictedDemandNext7Days())
                .predictedDemand30Days(prediction.getPredictedDemandNext30Days())
                .stockoutDate(stockoutDate)
                .daysUntilStockout(daysUntilStockout)
                .recommendedOrderQuantity(prediction.getRecommendedOrderQuantity())
                .recommendedOrderDate(prediction.getRecommendedOrderDate())
                .confidenceLevel(prediction.getConfidenceLevel())
                .urgency(urgency)
                .build();
    }

    private double calculateDemand(Product product, int days) {
        double baseDemand = product.getAverageDailySales() != null ?
                product.getAverageDailySales() * days : days * 10;

        // Aplicar factores estacionales
        double seasonalFactor = getSeasonalityFactor(product);

        return baseDemand * (1 + seasonalFactor);
    }

    private Integer calculateOrderQuantity(Product product) {
        if (product.getReorderQuantity() != null) {
            return product.getReorderQuantity();
        }

        // Calcular cantidad óptima basada en demanda promedio y lead time
        double avgDailySales = product.getAverageDailySales() != null ?
                product.getAverageDailySales() : 10;
        int leadTime = product.getLeadTimeDays() != null ?
                product.getLeadTimeDays() : 7;

        // EOQ simplificado
        return (int) (avgDailySales * leadTime * 2.5);
    }

    private LocalDate calculateOrderDate(Product product) {
        int leadTime = product.getLeadTimeDays() != null ?
                product.getLeadTimeDays() : 7;

        // Considerar el punto de reorden
        if (product.getReorderPoint() != null) {
            double avgDailySales = product.getAverageDailySales() != null ?
                    product.getAverageDailySales() : 10;
            int daysUntilReorderPoint = (int) (product.getReorderPoint() / avgDailySales);

            return LocalDate.now().plusDays(Math.max(0, daysUntilReorderPoint - leadTime));
        }

        return LocalDate.now();
    }

    private double calculateStockoutRisk(Product product) {
        // Simulación de riesgo basada en variabilidad y stock actual
        double variability = product.getSalesVariability() != null ?
                product.getSalesVariability() : 0.2;

        return Math.min(100, variability * 100 * ThreadLocalRandom.current().nextDouble(1.5));
    }

    private double calculateStockoutRisk(int currentStock, double predictedDemand) {
        if (currentStock <= 0) return 100.0;
        if (predictedDemand <= 0) return 0.0;

        double ratio = currentStock / predictedDemand;
        if (ratio > 2) return 0.0;
        if (ratio > 1) return (2 - ratio) * 20;

        return (1 - ratio) * 100;
    }

    private String analyzeTrend(Product product) {
        // Simulación de análisis de tendencia
        double random = ThreadLocalRandom.current().nextDouble();
        if (random < 0.3) return "DECREASING";
        if (random < 0.7) return "STABLE";
        return "INCREASING";
    }

    private Map<String, Double> getSeasonalFactors(Product product) {
        Map<String, Double> factors = new HashMap<>();

        // Factores simulados por mes
        factors.put("ENERO", 0.9);
        factors.put("FEBRERO", 0.85);
        factors.put("MARZO", 0.95);
        factors.put("ABRIL", 1.0);
        factors.put("MAYO", 1.05);
        factors.put("JUNIO", 1.1);
        factors.put("JULIO", 1.2); // Fiestas Patrias
        factors.put("AGOSTO", 1.05);
        factors.put("SEPTIEMBRE", 0.95);
        factors.put("OCTUBRE", 1.0);
        factors.put("NOVIEMBRE", 1.15);
        factors.put("DICIEMBRE", 1.4); // Navidad

        return factors;
    }

    private double getSeasonalityFactor(Product product) {
        // Factor basado en el mes actual
        LocalDate now = LocalDate.now();
        String monthKey = now.getMonth().toString();

        Map<String, Double> factors = getSeasonalFactors(product);
        return factors.getOrDefault(monthKey.substring(0, 1) + monthKey.substring(1).toLowerCase(), 1.0) - 1.0;
    }

    private double calculateConfidence(Product product) {
        // Confianza basada en la cantidad de datos históricos y variabilidad
        double baseConfidence = 70.0;

        if (product.getSalesVariability() != null && product.getSalesVariability() < 0.3) {
            baseConfidence += 15;
        }

        return Math.min(95, baseConfidence + ThreadLocalRandom.current().nextDouble(10));
    }

    private String determineUrgency(Integer daysUntilStockout, Integer leadTimeDays) {
        if (daysUntilStockout == null) return "LOW";

        int leadTime = leadTimeDays != null ? leadTimeDays : 7;

        if (daysUntilStockout <= leadTime) {
            return "IMMEDIATE";
        } else if (daysUntilStockout <= leadTime * 2) {
            return "HIGH";
        } else if (daysUntilStockout <= leadTime * 4) {
            return "MEDIUM";
        } else {
            return "LOW";
        }
    }
}