package by.bsuir.supplier.entity.catalog;

import by.bsuir.supplier.entity.enums.*;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Persisted aggregate/data record for products. No business decisions belong here. */
@Entity
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "sku", nullable = false, length = 80)
    private String sku;

    @Column(name = "name", nullable = false, length = 250)
    private String name;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "category_id", nullable = false)
    private ProductCategory category;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "unit_id", nullable = false)
    private Unit unit;

    @Column(name = "active", nullable = false)
    private Boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    public Product() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public String getSku() {
        return sku;
    }

    public void setSku(String value) {
        this.sku = value;
    }

    public String getName() {
        return name;
    }

    public void setName(String value) {
        this.name = value;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory value) {
        this.category = value;
    }

    public Unit getUnit() {
        return unit;
    }

    public void setUnit(Unit value) {
        this.unit = value;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean value) {
        this.active = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant value) {
        this.updatedAt = value;
    }

    public Long getLockVersion() {
        return lockVersion;
    }

    public void setLockVersion(Long value) {
        this.lockVersion = value;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    private void preUpdate() {
        updatedAt = Instant.now();
    }
}
