package by.bsuir.supplier.entity.fulfillment;

import by.bsuir.supplier.entity.auth.*;
import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.entity.order.*;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Persisted aggregate/data record for supplier_returns. No business decisions belong here. */
@Entity
@Table(name = "supplier_returns")
public class SupplierReturn {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @Column(name = "returned_on", nullable = false)
    private LocalDate returnedOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private ReturnState state;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "approved_by_id", nullable = true)
    private User approvedBy;

    @Column(name = "approved_at", nullable = true)
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "supersedes_id", nullable = true)
    private SupplierReturn supersedes;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "correction_id", nullable = true)
    private FactCorrection correction;

    public SupplierReturn() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public PurchaseOrder getOrder() {
        return order;
    }

    public void setOrder(PurchaseOrder value) {
        this.order = value;
    }

    public LocalDate getReturnedOn() {
        return returnedOn;
    }

    public void setReturnedOn(LocalDate value) {
        this.returnedOn = value;
    }

    public ReturnState getState() {
        return state;
    }

    public void setState(ReturnState value) {
        this.state = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        this.reason = value;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User value) {
        this.createdBy = value;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User value) {
        this.approvedBy = value;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant value) {
        this.approvedAt = value;
    }

    public SupplierReturn getSupersedes() {
        return supersedes;
    }

    public void setSupersedes(SupplierReturn value) {
        this.supersedes = value;
    }

    public FactCorrection getCorrection() {
        return correction;
    }

    public void setCorrection(FactCorrection value) {
        this.correction = value;
    }
}
