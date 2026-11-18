package by.bsuir.supplier.entity.contract;

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
import by.bsuir.supplier.entity.supplier.*;

/** Persisted aggregate/data record for contracts. No business decisions belong here. */
@Entity
@Table(name = "contracts")
public class Contract {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private ContractState state;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "current_registered_version_id", nullable = true)
    private ContractVersion currentRegisteredVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    public Contract() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier value) { this.supplier = value; }

    public ContractState getState() { return state; }
    public void setState(ContractState value) { this.state = value; }

    public ContractVersion getCurrentRegisteredVersion() { return currentRegisteredVersion; }
    public void setCurrentRegisteredVersion(ContractVersion value) { this.currentRegisteredVersion = value; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { this.createdBy = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public Long getLockVersion() { return lockVersion; }
    public void setLockVersion(Long value) { this.lockVersion = value; }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

}
