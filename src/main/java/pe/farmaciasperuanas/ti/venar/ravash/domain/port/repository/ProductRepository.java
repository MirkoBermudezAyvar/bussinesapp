package pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository;


import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.Product;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public interface ProductRepository extends ReactiveCrudRepository<Product, Long> {

    Mono<Product> findBySku(String sku);

    Flux<Product> findByCategory(String category);

    Flux<Product> findByActiveTrue();

    @Query("SELECT p.* FROM products p " +
            "JOIN inventory i ON p.id = i.product_id " +
            "WHERE i.current_stock < p.min_stock")
    Flux<Product> findLowStockProducts();

    @Query("SELECT p.* FROM products p " +
            "JOIN inventory i ON p.id = i.product_id " +
            "WHERE i.current_stock > p.max_stock")
    Flux<Product> findOverstockProducts();

    @Query("SELECT p.* FROM products p " +
            "WHERE p.perishable = true " +
            "AND EXISTS (SELECT 1 FROM inventory i " +
            "WHERE i.product_id = p.id " +
            "AND i.expiration_date <= DATE_ADD(CURRENT_DATE, INTERVAL 7 DAY))")
    Flux<Product> findExpiringProducts();
}