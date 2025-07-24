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
@Table("inventory")
public class Inventory {

    @Id
    private Long id;

    private Long productId;
    private Long warehouseId;
    private String warehouseName;

    private Integer currentStock;
    private Integer reservedStock; // stock comprometido
    private Integer availableStock; // stock disponible (current - reserved)

    private String batchNumber;
    private LocalDateTime expirationDate;

    private LocalDateTime lastStockCheck;
    private LocalDateTime lastMovement;

    // Métricas calculadas
    private Double stockTurnover; // rotación de inventario
    private Integer stockoutDays; // días sin stock en el último mes
    private Double stockHealth; // salud del inventario (0-100)

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}