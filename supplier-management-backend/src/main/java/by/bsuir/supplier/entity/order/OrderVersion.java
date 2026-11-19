package by.bsuir.supplier.entity.order;

import by.bsuir.supplier.entity.auth.*;
import by.bsuir.supplier.entity.contract.*;
import by.bsuir.supplier.entity.enums.*;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

/** Persisted aggregate/data record for order_versions. No business decisions belong here. */
@Entity
@Table(name = "order_versions")
public class OrderVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "base_version_id", nullable = true)
    private OrderVersion baseVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "contract_version_id", nullable = true)
    private ContractVersion contractVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 40)
    private OrderVersionKind kind;

    @Enumerated(EnumType.STRING)
    @Column(name = "initiator", nullable = true, length = 40)
    private Initiator initiator;

    @Column(name = "reason", nullable = true, columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private VersionState state;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "submitted_at", nullable = true)
    private Instant submittedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "decided_by_id", nullable = true)
    private User decidedBy;

    @Column(name = "decided_at", nullable = true)
    private Instant decidedAt;

    @Column(name = "decision_comment", nullable = true, columnDefinition = "text")
    private String decisionComment;

    public OrderVersion() {}

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

    public Integer getVersionNo() {
        return versionNo;
    }

    public void setVersionNo(Integer value) {
        this.versionNo = value;
    }

    public OrderVersion getBaseVersion() {
        return baseVersion;
    }

    public void setBaseVersion(OrderVersion value) {
        this.baseVersion = value;
    }

    public ContractVersion getContractVersion() {
        return contractVersion;
    }

    public void setContractVersion(ContractVersion value) {
        this.contractVersion = value;
    }

    public OrderVersionKind getKind() {
        return kind;
    }

    public void setKind(OrderVersionKind value) {
        this.kind = value;
    }

    public Initiator getInitiator() {
        return initiator;
    }

    public void setInitiator(Initiator value) {
        this.initiator = value;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String value) {
        this.reason = value;
    }

    public VersionState getState() {
        return state;
    }

    public void setState(VersionState value) {
        this.state = value;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User value) {
        this.createdBy = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }

    public Instant getSubmittedAt() {
        return submittedAt;
    }

    public void setSubmittedAt(Instant value) {
        this.submittedAt = value;
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

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
