package pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository;

import lombok.RequiredArgsConstructor;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Repository;

import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class InventoryRepositoryCustomImpl implements InventoryRepositoryCustom {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Void> updateStock(Long productId, Long warehouseId, Integer quantity) {
        return databaseClient.sql("UPDATE inventory SET current_stock = current_stock + :quantity, " +
                        "last_movement = CURRENT_TIMESTAMP " +
                        "WHERE product_id = :productId AND warehouse_id = :warehouseId")
                .bind("quantity", quantity)
                .bind("productId", productId)
                .bind("warehouseId", warehouseId)
                .fetch()
                .rowsUpdated()
                .then();
    }
}