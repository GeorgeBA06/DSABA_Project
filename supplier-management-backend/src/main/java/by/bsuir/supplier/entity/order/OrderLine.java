package by.bsuir.supplier.entity.order;

import by.bsuir.supplier.entity.catalog.*;
import by.bsuir.supplier.entity.enums.*;

import jakarta.persistence.*;

import java.util.UUID;

/** Persisted aggregate/data record for order_lines. No business decisions belong here. */
@Entity
@Table(name = "order_lines")
public class OrderLine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_id", nullable = false)
    private PurchaseOrder order;

    @Column(name = "line_no", nullable = false)
    private Integer lineNo;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    public OrderLine() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public PurchaseOrder getOrder() {
        return order;
    }

    public void setOrder(PurchaseOrder value) {
        this.order = value;
    }

    public Integer getLineNo() {
        return lineNo;
    }

    public void setLineNo(Integer value) {
        this.lineNo = value;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product value) {
        this.product = value;
    }
}
