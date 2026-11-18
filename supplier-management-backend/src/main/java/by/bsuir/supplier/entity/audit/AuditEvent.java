package by.bsuir.supplier.entity.audit;

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

/** Persisted aggregate/data record for audit_events. No business decisions belong here. */
@Entity
@Table(name = "audit_events")
public class AuditEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "actor_id", nullable = true)
    private User actor;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private AuditCategory category;

    @Column(name = "action", nullable = false, length = 120)
    private String action;

    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;

    @Column(name = "entity_id", nullable = false)
    private UUID entityId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "before_data", nullable = true, columnDefinition = "jsonb")
    private Map<String,Object> beforeData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "after_data", nullable = true, columnDefinition = "jsonb")
    private Map<String,Object> afterData;

    @Column(name = "reason", nullable = true, columnDefinition = "text")
    private String reason;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    public AuditEvent() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public User getActor() { return actor; }
    public void setActor(User value) { this.actor = value; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant value) { this.occurredAt = value; }

    public AuditCategory getCategory() { return category; }
    public void setCategory(AuditCategory value) { this.category = value; }

    public String getAction() { return action; }
    public void setAction(String value) { this.action = value; }

    public String getEntityType() { return entityType; }
    public void setEntityType(String value) { this.entityType = value; }

    public UUID getEntityId() { return entityId; }
    public void setEntityId(UUID value) { this.entityId = value; }

    public Map<String,Object> getBeforeData() { return beforeData; }
    public void setBeforeData(Map<String,Object> value) { this.beforeData = value; }

    public Map<String,Object> getAfterData() { return afterData; }
    public void setAfterData(Map<String,Object> value) { this.afterData = value; }

    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }

    public UUID getCorrelationId() { return correlationId; }
    public void setCorrelationId(UUID value) { this.correlationId = value; }

}
