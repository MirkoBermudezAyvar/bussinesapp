package pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository;


import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.Inventory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface InventoryRepository extends ReactiveCrudRepository<Inventory, Long> {

    Flux<Inventory> findByProductId(Long productId);

    Mono<Inventory> findByProductIdAndWarehouseId(Long productId, Long warehouseId);

    Flux<Inventory> findByWarehouseId(Long warehouseId);

    @Query("SELECT * FROM inventory WHERE current_stock <= reserved_stock + 5")
    Flux<Inventory> findCriticalStock();

    @Query("SELECT SUM(current_stock * " +
            "(SELECT cost_price FROM products WHERE id = inventory.product_id)) " +
            "as total_value FROM inventory")
    Mono<Double> calculateTotalInventoryValue();
}
