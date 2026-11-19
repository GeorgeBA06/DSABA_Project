package by.bsuir.supplier.entity.claim;

import by.bsuir.supplier.entity.auth.*;
import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.entity.fulfillment.*;
import by.bsuir.supplier.entity.order.*;
import by.bsuir.supplier.entity.supplier.*;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Persisted aggregate/data record for claims. No business decisions belong here. */
@Entity
@Table(name = "claims")
public class Claim {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "number", nullable = false, length = 80)
    private String number;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_line_id", nullable = false)
    private OrderLine orderLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "delivery_line_id", nullable = true)
    private DeliveryLine deliveryLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", nullable = false, length = 40)
    private ViolationType violationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private ClaimState state;

    @Column(name = "description", nullable = false, columnDefinition = "text")
    private String description;

    @Column(name = "requirements", nullable = false, columnDefinition = "text")
    private String requirements;

    @Column(name = "response_due_on", nullable = true)
    private LocalDate responseDueOn;

    @Column(name = "sent_at", nullable = true)
    private Instant sentAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "send_channel", nullable = true, length = 40)
    private InteractionChannel sendChannel;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = true, length = 40)
    private ClaimResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "remedy_type", nullable = true, length = 40)
    private RemedyType remedyType;

    @Column(name = "resolution_description", nullable = true, columnDefinition = "text")
    private String resolutionDescription;

    @Column(name = "resolved_at", nullable = true)
    private Instant resolvedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "approved_by_id", nullable = true)
    private User approvedBy;

    @Version
    @Column(name = "lock_version", nullable = false)
    private Long lockVersion;

    public Claim() {}

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

    public String getNumber() {
        return number;
    }

    public void setNumber(String value) {
        this.number = value;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public void setOrderLine(OrderLine value) {
        this.orderLine = value;
    }

    public DeliveryLine getDeliveryLine() {
        return deliveryLine;
    }

    public void setDeliveryLine(DeliveryLine value) {
        this.deliveryLine = value;
    }

    public ViolationType getViolationType() {
        return violationType;
    }

    public void setViolationType(ViolationType value) {
        this.violationType = value;
    }

    public ClaimState getState() {
        return state;
    }

    public void setState(ClaimState value) {
        this.state = value;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String value) {
        this.description = value;
    }

    public String getRequirements() {
        return requirements;
    }

    public void setRequirements(String value) {
        this.requirements = value;
    }

    public LocalDate getResponseDueOn() {
        return responseDueOn;
    }

    public void setResponseDueOn(LocalDate value) {
        this.responseDueOn = value;
    }

    public Instant getSentAt() {
        return sentAt;
    }

    public void setSentAt(Instant value) {
        this.sentAt = value;
    }

    public InteractionChannel getSendChannel() {
        return sendChannel;
    }

    public void setSendChannel(InteractionChannel value) {
        this.sendChannel = value;
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

    public ClaimResult getResult() {
        return result;
    }

    public void setResult(ClaimResult value) {
        this.result = value;
    }

    public RemedyType getRemedyType() {
        return remedyType;
    }

    public void setRemedyType(RemedyType value) {
        this.remedyType = value;
    }

    public String getResolutionDescription() {
        return resolutionDescription;
    }

    public void setResolutionDescription(String value) {
        this.resolutionDescription = value;
    }

    public Instant getResolvedAt() {
        return resolvedAt;
    }

    public void setResolvedAt(Instant value) {
        this.resolvedAt = value;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User value) {
        this.approvedBy = value;
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
    }
}
