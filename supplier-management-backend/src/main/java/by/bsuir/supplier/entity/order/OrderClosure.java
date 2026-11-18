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

/** Persisted aggregate/data record for order_closures. No business decisions belong here. */
@Entity
@Table(name = "order_closures")
public class OrderClosure {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private ClosureAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = true, length = 40)
    private ClosureResult result;

    @Column(name = "reason", nullable = true, columnDefinition = "text")
    private String reason;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "requested_by_id", nullable = true)
    private User requestedBy;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "decided_by_id", nullable = true)
    private User decidedBy;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    public OrderClosure() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public PurchaseOrder getOrder() { return order; }
    public void setOrder(PurchaseOrder value) { this.order = value; }

    public ClosureAction getAction() { return action; }
    public void setAction(ClosureAction value) { this.action = value; }

    public ClosureResult getResult() { return result; }
    public void setResult(ClosureResult value) { this.result = value; }

    public String getReason() { return reason; }
    public void setReason(String value) { this.reason = value; }

    public User getRequestedBy() { return requestedBy; }
    public void setRequestedBy(User value) { this.requestedBy = value; }

    public User getDecidedBy() { return decidedBy; }
    public void setDecidedBy(User value) { this.decidedBy = value; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant value) { this.occurredAt = value; }

}
