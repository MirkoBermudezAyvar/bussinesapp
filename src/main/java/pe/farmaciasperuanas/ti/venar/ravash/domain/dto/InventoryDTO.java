package pe.farmaciasperuanas.ti.venar.ravash.domain.dto;

import lombok.Data;
import lombok.Builder;
import java.time.LocalDateTime;

@Data
@Builder
public class InventoryDTO {

    private Long productId;
    private String productName;
    private String sku;
    private Long warehouseId;
    private String warehouseName;

    private Integer currentStock;
    private Integer availableStock;
    private Integer reservedStock;

    private LocalDateTime expirationDate;
    private Integer daysUntilExpiration;

    private Double stockTurnover;
    private String healthStatus;
    private Double stockValue; // current stock * cost price
}