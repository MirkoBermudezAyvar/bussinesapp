package pe.farmaciasperuanas.ti.venar.ravash.domain.port.repository;

import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import pe.farmaciasperuanas.ti.venar.ravash.domain.model.Alert;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Repository
public interface AlertRepository extends ReactiveCrudRepository<Alert, Long> {

    Flux<Alert> findByResolvedFalseOrderByCreatedAtDesc();

    Flux<Alert> findByProductIdAndResolvedFalse(Long productId);

    Flux<Alert> findBySeverityAndResolvedFalse(String severity);

    @Query("SELECT * FROM alerts WHERE created_at >= :since " +
            "ORDER BY severity DESC, created_at DESC")
    Flux<Alert> findRecentAlerts(LocalDateTime since);

    @Query("UPDATE alerts SET resolved = true, resolved_at = CURRENT_TIMESTAMP, " +
            "resolved_by = :resolvedBy WHERE id = :alertId")
    Mono<Void> resolveAlert(Long alertId, String resolvedBy);

    Mono<Long> countByResolvedFalseAndSeverity(String severity);
}