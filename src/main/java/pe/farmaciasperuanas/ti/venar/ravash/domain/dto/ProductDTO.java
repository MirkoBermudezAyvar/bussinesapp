package pe.farmaciasperuanas.ti.venar.ravash.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;
import lombok.Builder;

import java.math.BigDecimal;

@Data
@Builder
public class ProductDTO {

    private Long id;

    @NotBlank(message = "SKU es requerido")
    private String sku;

    @NotBlank(message = "Nombre es requerido")
    private String name;

    private String description;
    private String category;
    private String brand;

    @NotNull(message = "Precio de costo es requerido")
    @Positive
    private BigDecimal costPrice;

    @NotNull(message = "Precio de venta es requerido")
    @Positive
    private BigDecimal salePrice;

    @NotNull
    @Positive
    private Integer minStock;

    private Integer currentStock;
    private Double stockHealth;
    private String stockStatus; // OK, LOW, CRITICAL, OVERSTOCK
}
