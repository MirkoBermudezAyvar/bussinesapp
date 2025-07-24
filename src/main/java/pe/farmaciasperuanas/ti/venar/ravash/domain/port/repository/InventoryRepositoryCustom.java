package pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository;

import reactor.core.publisher.Mono;

public interface InventoryRepositoryCustom {
    Mono<Void> updateStock(Long productId, Long warehouseId, Integer quantity);
}