package by.bsuir.supplier.entity.claim;

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

/** Persisted aggregate/data record for interactions. No business decisions belong here. */
@Entity
@Table(name = "interactions")
public class Interaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "employee_id", nullable = false)
    private User employee;

    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "channel", nullable = false, length = 40)
    private InteractionChannel channel;

    @Column(name = "subject", nullable = false, length = 250)
    private String subject;

    @Column(name = "content", nullable = false, columnDefinition = "text")
    private String content;

    @Column(name = "outcome", nullable = true, columnDefinition = "text")
    private String outcome;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "claim_id", nullable = true)
    private Claim claim;

    public Interaction() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public Supplier getSupplier() { return supplier; }
    public void setSupplier(Supplier value) { this.supplier = value; }

    public User getEmployee() { return employee; }
    public void setEmployee(User value) { this.employee = value; }

    public Instant getOccurredAt() { return occurredAt; }
    public void setOccurredAt(Instant value) { this.occurredAt = value; }

    public InteractionChannel getChannel() { return channel; }
    public void setChannel(InteractionChannel value) { this.channel = value; }

    public String getSubject() { return subject; }
    public void setSubject(String value) { this.subject = value; }

    public String getContent() { return content; }
    public void setContent(String value) { this.content = value; }

    public String getOutcome() { return outcome; }
    public void setOutcome(String value) { this.outcome = value; }

    public Claim getClaim() { return claim; }
    public void setClaim(Claim value) { this.claim = value; }

}
