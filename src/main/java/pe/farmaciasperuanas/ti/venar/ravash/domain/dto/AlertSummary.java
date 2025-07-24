package pe.farmaciasperuanas.ti.venar.ravash.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public  class AlertSummary {
    private Long id;
    private String type;
    private String severity;
    private String message;
    private String productName;
    private LocalDateTime createdAt;
}
