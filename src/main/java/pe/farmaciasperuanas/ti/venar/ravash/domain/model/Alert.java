package pe.farmaciasperuanas.ti.venar.ravash.domain.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table("alerts")
public class Alert {

    @Id
    private Long id;

    private Long productId;
    private Long warehouseId;

    private String type; // LOW_STOCK, OVERSTOCK, EXPIRING, ANOMALY
    private String severity; // HIGH, MEDIUM, LOW
    private String message;
    private String recommendation;

    private Boolean resolved;
    private LocalDateTime resolvedAt;
    private String resolvedBy;

    private LocalDateTime createdAt;
    private LocalDateTime scheduledReview;
}