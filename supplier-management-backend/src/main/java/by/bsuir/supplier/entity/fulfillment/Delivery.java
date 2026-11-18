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
import by.bsuir.supplier.entity.order.*;

/** Persisted aggregate/data record for deliveries. No business decisions belong here. */
@Entity
@Table(name = "deliveries")
public class Delivery {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @Column(name = "arrived_on", nullable = false)
    private LocalDate arrivedOn;

    @Column(name = "registered_at", nullable = false)
    private Instant registeredAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "registered_by_id", nullable = false)
    private User registeredBy;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private FactState state;

    @Column(name = "external_number", nullable = true, length = 100)
    private String externalNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "supersedes_id", nullable = true)
    private Delivery supersedes;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "correction_id", nullable = true)
    private FactCorrection correction;

    public Delivery() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public PurchaseOrder getOrder() { return order; }
    public void setOrder(PurchaseOrder value) { this.order = value; }

    public LocalDate getArrivedOn() { return arrivedOn; }
    public void setArrivedOn(LocalDate value) { this.arrivedOn = value; }

    public Instant getRegisteredAt() { return registeredAt; }
    public void setRegisteredAt(Instant value) { this.registeredAt = value; }

    public User getRegisteredBy() { return registeredBy; }
    public void setRegisteredBy(User value) { this.registeredBy = value; }

    public FactState getState() { return state; }
    public void setState(FactState value) { this.state = value; }

    public String getExternalNumber() { return externalNumber; }
    public void setExternalNumber(String value) { this.externalNumber = value; }

    public Delivery getSupersedes() { return supersedes; }
    public void setSupersedes(Delivery value) { this.supersedes = value; }

    public FactCorrection getCorrection() { return correction; }
    public void setCorrection(FactCorrection value) { this.correction = value; }

}
