package by.bsuir.supplier.entity.order;

import jakarta.persistence.*;
import java.util.UUID;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.entity.auth.*;
import by.bsuir.supplier.entity.contract.*;
import by.bsuir.supplier.entity.supplier.*;

/** Persisted aggregate/data record for purchase_orders. No business decisions belong here. */
@Entity
@Table(name = "purchase_orders")
public class PurchaseOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "number", nullable = false, length = 80)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "contract_id", nullable = true)
    private Contract contract;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private OrderState state;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "current_approved_version_id", nullable = true)
    private OrderVersion currentApprovedVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    public PurchaseOrder() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public String getNumber() { return number; }
    public void setNumber(String value) { this.number = value; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier value) { this.supplier = value; }

    public Contract getContract() { return contract; }
    public void setContract(Contract value) { this.contract = value; }

    public OrderState getState() { return state; }
    public void setState(OrderState value) { this.state = value; }

    public OrderVersion getCurrentApprovedVersion() { return currentApprovedVersion; }
    public void setCurrentApprovedVersion(OrderVersion value) { this.currentApprovedVersion = value; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { this.createdBy = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant value) { this.updatedAt = value; }

    public Long getLockVersion() { return lockVersion; }
    public void setLockVersion(Long value) { this.lockVersion = value; }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
        if (updatedAt == null) updatedAt = Instant.now();
    }

    @PreUpdate
    private void preUpdate() { updatedAt = Instant.now(); }

}
