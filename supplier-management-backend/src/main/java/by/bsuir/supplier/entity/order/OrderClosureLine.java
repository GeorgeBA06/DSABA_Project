package by.bsuir.supplier.entity.order;

import by.bsuir.supplier.entity.enums.*;

import jakarta.persistence.*;

import java.math.BigDecimal;

/** Persisted aggregate/data record for order_closure_lines. No business decisions belong here. */
@Entity
@Table(name = "order_closure_lines")
public class OrderClosureLine {
    @EmbeddedId private OrderClosureLineId id;

    @MapsId("closureId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "closure_id", nullable = false)
    private OrderClosure closure;

    @MapsId("orderLineId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_line_id", nullable = false)
    private OrderLine orderLine;

    @Column(name = "ordered_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal orderedQty;

    @Column(name = "fulfilled_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal fulfilledQty;

    @Column(name = "unfulfilled_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal unfulfilledQty;

    public OrderClosureLine() {}

    public OrderClosureLine(OrderClosureLineId id) {
        this.id = id;
    }

    public OrderClosureLineId getId() {
        return id;
    }

    public void setId(OrderClosureLineId value) {
        this.id = value;
    }

    public OrderClosure getClosure() {
        return closure;
    }

    public void setClosure(OrderClosure value) {
        this.closure = value;
    }

    public OrderLine getOrderLine() {
        return orderLine;
    }

    public void setOrderLine(OrderLine value) {
        this.orderLine = value;
    }

    public BigDecimal getOrderedQty() {
        return orderedQty;
    }

    public void setOrderedQty(BigDecimal value) {
        this.orderedQty = value;
    }

    public BigDecimal getFulfilledQty() {
        return fulfilledQty;
    }

    public void setFulfilledQty(BigDecimal value) {
        this.fulfilledQty = value;
    }

    public BigDecimal getUnfulfilledQty() {
        return unfulfilledQty;
    }

    public void setUnfulfilledQty(BigDecimal value) {
        this.unfulfilledQty = value;
    }
}
