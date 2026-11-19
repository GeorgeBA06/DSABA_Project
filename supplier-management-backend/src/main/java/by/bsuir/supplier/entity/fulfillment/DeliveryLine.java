package by.bsuir.supplier.entity.fulfillment;

import by.bsuir.supplier.entity.claim.*;
import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.entity.order.*;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Persisted aggregate/data record for delivery_lines. No business decisions belong here. */
@Entity
@Table(name = "delivery_lines")
public class DeliveryLine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_id", nullable = false)
    private Delivery delivery;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_version_line_id", nullable = false)
    private OrderVersionLine orderVersionLine;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "replacement_for_line_id", nullable = true)
    private DeliveryLine replacementForLine;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "claim_id", nullable = true)
    private Claim claim;

    public DeliveryLine() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public Delivery getDelivery() {
        return delivery;
    }

    public void setDelivery(Delivery value) {
        this.delivery = value;
    }

    public OrderVersionLine getOrderVersionLine() {
        return orderVersionLine;
    }

    public void setOrderVersionLine(OrderVersionLine value) {
        this.orderVersionLine = value;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal value) {
        this.quantity = value;
    }

    public DeliveryLine getReplacementForLine() {
        return replacementForLine;
    }

    public void setReplacementForLine(DeliveryLine value) {
        this.replacementForLine = value;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim value) {
        this.claim = value;
    }
}
