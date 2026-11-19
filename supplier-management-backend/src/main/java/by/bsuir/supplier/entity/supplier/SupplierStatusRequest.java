package by.bsuir.supplier.entity.supplier;

import by.bsuir.supplier.entity.auth.*;
import by.bsuir.supplier.entity.enums.*;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/**
 * Persisted aggregate/data record for supplier_status_requests. No business decisions belong here.
 */
@Entity
@Table(name = "supplier_status_requests")
public class SupplierStatusRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "from_status", nullable = false, length = 40)
    private SupplierStatus fromStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "to_status", nullable = false, length = 40)
    private SupplierStatus toStatus;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "requested_by_id", nullable = false)
    private User requestedBy;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private DecisionState state;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "decided_by_id", nullable = true)
    private User decidedBy;

    @Column(name = "decided_at", nullable = true)
    private Instant decidedAt;

    @Column(name = "decision_comment", nullable = true, columnDefinition = "text")
    private String decisionComment;

    public SupplierStatusRequest() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier value) {
        this.supplier = value;
    }

    public SupplierStatus getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(SupplierStatus value) {
        this.fromStatus = value;
    }

    public SupplierStatus getToStatus() {
        return toStatus;
    }

    public void setToStatus(SupplierStatus value) {
        this.toStatus = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        this.reason = value;
    }

    public User getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(User value) {
        this.requestedBy = value;
    }

    public Instant getRequestedAt() {
        return requestedAt;
    }

    public void setRequestedAt(Instant value) {
        this.requestedAt = value;
    }

    public DecisionState getState() {
        return state;
    }

    public void setState(DecisionState value) {
        this.state = value;
    }

    public User getDecidedBy() {
        return decidedBy;
    }

    public void setDecidedBy(User value) {
        this.decidedBy = value;
    }

    public Instant getDecidedAt() {
        return decidedAt;
    }

    public void setDecidedAt(Instant value) {
        this.decidedAt = value;
    }

    public String getDecisionComment() {
        return decisionComment;
    }

    public void setDecisionComment(String value) {
        this.decisionComment = value;
    }
}
