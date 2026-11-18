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

/** Persisted aggregate/data record for order_version_lines. No business decisions belong here. */
@Entity
@Table(name = "order_version_lines")
public class OrderVersionLine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_version_id", nullable = false)
    private OrderVersion orderVersion;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_line_id", nullable = false)
    private OrderLine orderLine;

    @Column(name = "segment_key", nullable = false, length = 64)
    private String segmentKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "source_version_line_id", nullable = true)
    private OrderVersionLine sourceVersionLine;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "unit_price", nullable = false, precision = 19, scale = 2)
    private BigDecimal unitPrice;

    @Column(name = "execution_due_on", nullable = false)
    private LocalDate executionDueOn;

    @Column(name = "rating_due_on", nullable = false)
    private LocalDate ratingDueOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "origin_reason", nullable = false, length = 40)
    private OriginReason originReason;

    @Column(name = "product_name_snapshot", nullable = false, length = 250)
    private String productNameSnapshot;

    @Column(name = "category_code_snapshot", nullable = false, length = 50)
    private String categoryCodeSnapshot;

    @Column(name = "category_name_snapshot", nullable = false, length = 150)
    private String categoryNameSnapshot;

    @Column(name = "unit_code_snapshot", nullable = false, length = 20)
    private String unitCodeSnapshot;

    public OrderVersionLine() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public OrderVersion getOrderVersion() { return orderVersion; }
    public void setOrderVersion(OrderVersion value) { this.orderVersion = value; }

    public OrderLine getOrderLine() { return orderLine; }
    public void setOrderLine(OrderLine value) { this.orderLine = value; }

    public String getSegmentKey() { return segmentKey; }
    public void setSegmentKey(String value) { this.segmentKey = value; }

    public OrderVersionLine getSourceVersionLine() { return sourceVersionLine; }
    public void setSourceVersionLine(OrderVersionLine value) { this.sourceVersionLine = value; }

    public BigDecimal getQuantity() { return quantity; }
    public void setQuantity(BigDecimal value) { this.quantity = value; }

    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal value) { this.unitPrice = value; }

    public LocalDate getExecutionDueOn() { return executionDueOn; }
    public void setExecutionDueOn(LocalDate value) { this.executionDueOn = value; }

    public LocalDate getRatingDueOn() { return ratingDueOn; }
    public void setRatingDueOn(LocalDate value) { this.ratingDueOn = value; }

    public OriginReason getOriginReason() { return originReason; }
    public void setOriginReason(OriginReason value) { this.originReason = value; }

    public String getProductNameSnapshot() { return productNameSnapshot; }
    public void setProductNameSnapshot(String value) { this.productNameSnapshot = value; }

    public String getCategoryCodeSnapshot() { return categoryCodeSnapshot; }
    public void setCategoryCodeSnapshot(String value) { this.categoryCodeSnapshot = value; }

    public String getCategoryNameSnapshot() { return categoryNameSnapshot; }
    public void setCategoryNameSnapshot(String value) { this.categoryNameSnapshot = value; }

    public String getUnitCodeSnapshot() { return unitCodeSnapshot; }
    public void setUnitCodeSnapshot(String value) { this.unitCodeSnapshot = value; }

}
