package by.bsuir.supplier.entity.fulfillment;

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

/** Persisted aggregate/data record for fact_corrections. No business decisions belong here. */
@Entity
@Table(name = "fact_corrections")
public class FactCorrection {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "reason", nullable = false, columnDefinition = "text")
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private VersionState state;

    @Column(name = "effective_on", nullable = false)
    private LocalDate effectiveOn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "decided_by_id", nullable = true)
    private User decidedBy;

    @Column(name = "decided_at", nullable = true)
    private Instant decidedAt;

    @Column(name = "decision_comment", nullable = true, columnDefinition = "text")
    private String decisionComment;

    public FactCorrection() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }

    public VersionState getState() { return state; }
    public void setState(VersionState value) { this.state = value; }

    public LocalDate getEffectiveOn() { return effectiveOn; }
    public void setEffectiveOn(LocalDate value) { this.effectiveOn = value; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { this.createdBy = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public User getDecidedBy() { return decidedBy; }
    public void setDecidedBy(User value) { this.decidedBy = value; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant value) { this.decidedAt = value; }

    public String getDecisionComment() { return decisionComment; }
    public void setDecisionComment(String value) { this.decisionComment = value; }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

}
