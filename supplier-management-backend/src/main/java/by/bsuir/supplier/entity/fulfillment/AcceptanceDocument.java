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

/** Persisted aggregate/data record for acceptance_documents. No business decisions belong here. */
@Entity
@Table(name = "acceptance_documents")
public class AcceptanceDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @Column(name = "inspected_on", nullable = false)
    private LocalDate inspectedOn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by_id", nullable = false)
    private User registeredBy;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private FactState state;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "supersedes_id", nullable = true)
    private AcceptanceDocument supersedes;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "correction_id", nullable = true)
    private FactCorrection correction;

    public AcceptanceDocument() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public Delivery getDelivery() { return delivery; }
    public void setDelivery(Delivery value) { this.delivery = value; }

    public LocalDate getInspectedOn() { return inspectedOn; }
    public void setInspectedOn(LocalDate value) { this.inspectedOn = value; }

    public User getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(User value) { this.registeredBy = value; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant value) { this.registeredAt = value; }

    public FactState getState() { return state; }
    public void setState(FactState value) { this.state = value; }

    public AcceptanceDocument getSupersedes() { return supersedes; }
    public void setSupersedes(AcceptanceDocument value) { this.supersedes = value; }

    public FactCorrection getCorrection() { return correction; }
    public void setCorrection(FactCorrection value) { this.correction = value; }

}
