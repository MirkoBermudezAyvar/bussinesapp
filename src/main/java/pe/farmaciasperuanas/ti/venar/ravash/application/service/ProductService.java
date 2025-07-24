package pe.farmaciasperuanas.ti.venar.ravash.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import pe.farmaciasperuanas.ti.venar.ravash.domain.dto.ProductDTO;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.Product;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.InventoryRepository;
import pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository.ProductRepository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProductService {

    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;

    public Mono<ProductDTO> createProduct(ProductDTO dto) {
        log.info("Creating product with SKU: {}", dto.getSku());

        return productRepository.findBySku(dto.getSku())
                .flatMap(existing -> Mono.<Product>error(new RuntimeException("SKU ya existe: " + dto.getSku())))
                .switchIfEmpty(Mono.defer(() -> {
                    Product product = Product.builder()
                            .sku(dto.getSku())
                            .name(dto.getName())
                            .description(dto.getDescription())
                            .category(dto.getCategory())
                            .brand(dto.getBrand())
                            .costPrice(dto.getCostPrice())
                            .salePrice(dto.getSalePrice())
                            .minStock(dto.getMinStock())
                            .active(true)
                            .build();

                    return productRepository.save(product);
                }))
                .map(this::toDTO)
                .doOnSuccess(saved -> log.info("Product created: {}", saved.getId()));
    }

    public Mono<ProductDTO> getProduct(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Producto no encontrado: " + id)))
                .flatMap(this::enrichWithInventoryData);
    }

    public Flux<ProductDTO> getAllProducts() {
        return productRepository.findByActiveTrue()
                .flatMap(this::enrichWithInventoryData);
    }

    public Flux<ProductDTO> getLowStockProducts() {
        return productRepository.findLowStockProducts()
                .flatMap(this::enrichWithInventoryData)
                .doOnNext(p -> p.setStockStatus("LOW"));
    }

    public Flux<ProductDTO> getOverstockProducts() {
        return productRepository.findOverstockProducts()
                .flatMap(this::enrichWithInventoryData)
                .doOnNext(p -> p.setStockStatus("OVERSTOCK"));
    }

    public Mono<ProductDTO> updateProduct(Long id, ProductDTO dto) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Producto no encontrado")))
                .flatMap(product -> {
                    product.setName(dto.getName());
                    product.setDescription(dto.getDescription());
                    product.setCategory(dto.getCategory());
                    product.setBrand(dto.getBrand());
                    product.setCostPrice(dto.getCostPrice());
                    product.setSalePrice(dto.getSalePrice());
                    product.setMinStock(dto.getMinStock());
                    return productRepository.save(product);
                })
                .map(this::toDTO);
    }

    public Mono<Void> deleteProduct(Long id) {
        return productRepository.findById(id)
                .switchIfEmpty(Mono.error(new RuntimeException("Producto no encontrado")))
                .flatMap(product -> {
                    product.setActive(false);
                    return productRepository.save(product);
                })
                .then();
    }

    private Mono<ProductDTO> enrichWithInventoryData(Product product) {
        return inventoryRepository.findByProductId(product.getId())
                .collectList()
                .map(inventories -> {
                    int totalStock = inventories.stream()
                            .mapToInt(i -> i.getCurrentStock())
                            .sum();

                    ProductDTO dto = toDTO(product);
                    dto.setCurrentStock(totalStock);
                    dto.setStockHealth(calculateStockHealth(totalStock, product.getMinStock(), product.getMaxStock()));
                    dto.setStockStatus(determineStockStatus(totalStock, product.getMinStock(), product.getMaxStock()));
                    return dto;
                });
    }

    private Double calculateStockHealth(int currentStock, Integer minStock, Integer maxStock) {
        if (minStock == null) minStock = 0;
        if (maxStock == null) maxStock = Integer.MAX_VALUE;

        if (currentStock < minStock) {
            return Math.max(0, (double) currentStock / minStock * 50);
        } else if (currentStock > maxStock) {
            double excess = currentStock - maxStock;
            return Math.max(50, 100 - (excess / maxStock * 50));
        } else {
            double range = maxStock - minStock;
            double position = currentStock - minStock;
            return 50 + (position / range * 50);
        }
    }

    private String determineStockStatus(int currentStock, Integer minStock, Integer maxStock) {
        if (minStock == null) minStock = 0;
        if (maxStock == null) maxStock = Integer.MAX_VALUE;

        if (currentStock <= minStock * 0.5) {
            return "CRITICAL";
        } else if (currentStock < minStock) {
            return "LOW";
        } else if (currentStock > maxStock) {
            return "OVERSTOCK";
        } else {
            return "OK";
        }
    }

    private ProductDTO toDTO(Product product) {
        return ProductDTO.builder()
                .id(product.getId())
                .sku(product.getSku())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .costPrice(product.getCostPrice())
                .salePrice(product.getSalePrice())
                .minStock(product.getMinStock())
                .build();
    }
}