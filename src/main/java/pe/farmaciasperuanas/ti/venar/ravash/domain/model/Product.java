package pe.farmaciasperuanas.ti.venar.ravash.domain.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;

import jakarta.validation.constraints.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "products",
        indexes = {
                @Index(name = "idx_product_sku", columnList = "sku", unique = true),
                @Index(name = "idx_product_category", columnList = "category"),
                @Index(name = "idx_product_brand", columnList = "brand"),
                @Index(name = "idx_product_active", columnList = "active")
        }
)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(of = "id")
@ToString(exclude = {"createdAt", "updatedAt"})
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @NotBlank(message = "SKU es requerido")
    @Size(max = 50)
    @Column(name = "sku", nullable = false, unique = true, length = 50)
    private String sku;

    @NotBlank(message = "Nombre es requerido")
    @Size(max = 200)
    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Size(max = 1000)
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Size(max = 100)
    @Column(name = "category", length = 100)
    private String category;

    @Size(max = 100)
    @Column(name = "brand", length = 100)
    private String brand;

    @Size(max = 20)
    @Column(name = "unit", length = 20)
    private String unit; // unidad, caja, etc

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "cost_price", precision = 12, scale = 2)
    private BigDecimal costPrice;

    @DecimalMin(value = "0.0", inclusive = false)
    @Digits(integer = 10, fraction = 2)
    @Column(name = "sale_price", precision = 12, scale = 2)
    private BigDecimal salePrice;

    @Min(0)
    @Column(name = "min_stock")
    private Integer minStock;

    @Min(0)
    @Column(name = "max_stock")
    private Integer maxStock;

    @Min(0)
    @Column(name = "reorder_point")
    private Integer reorderPoint;

    @Min(0)
    @Column(name = "reorder_quantity")
    private Integer reorderQuantity;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "perishable", nullable = false)
    @Builder.Default
    private Boolean perishable = false;

    @Min(0)
    @Column(name = "shelf_life_days")
    private Integer shelfLifeDays; // vida útil en días

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    @Temporal(TemporalType.TIMESTAMP)
    private LocalDateTime updatedAt;

    // Campos para análisis predictivo
    @Column(name = "average_daily_sales", precision = 10, scale = 2)
    private Double averageDailySales;

    @Column(name = "sales_variability", precision = 10, scale = 2)
    private Double salesVariability; // desviación estándar

    @Enumerated(EnumType.STRING)
    @Column(name = "seasonality", length = 20)
    private SeasonalityType seasonality;

    @Min(0)
    @Column(name = "lead_time_days")
    private Integer leadTimeDays; // tiempo de entrega del proveedor

    @PrePersist
    protected void onCreate() {
        if (active == null) active = true;
        if (perishable == null) perishable = false;
        if (createdAt == null) createdAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Enum para seasonality
    public enum SeasonalityType {
        ALTA, MEDIA, BAJA
    }
}