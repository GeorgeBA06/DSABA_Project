package by.bsuir.supplier.entity.order;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite identity; both columns are real foreign keys. */
@Embeddable
public class OrderClosureLineId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "closure_id", nullable = false)
    private UUID closureId;

    @Column(name = "order_line_id", nullable = false)
    private UUID orderLineId;

    public OrderClosureLineId() {}

    public OrderClosureLineId(UUID closureId, UUID orderLineId) {
        this.closureId = closureId;
        this.orderLineId = orderLineId;
    }

    public UUID getClosureId() {
        return closureId;
    }

    public void setClosureId(UUID value) {
        this.closureId = value;
    }

    public UUID getOrderLineId() {
        return orderLineId;
    }

    public void setOrderLineId(UUID value) {
        this.orderLineId = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OrderClosureLineId that)) return false;
        return Objects.equals(closureId, that.closureId)
                && Objects.equals(orderLineId, that.orderLineId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(closureId, orderLineId);
    }
}
