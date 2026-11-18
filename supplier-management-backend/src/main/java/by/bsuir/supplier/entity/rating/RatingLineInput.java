package by.bsuir.supplier.entity.rating;

import jakarta.persistence.*;
import java.util.UUID;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.entity.order.*;

/** Persisted aggregate/data record for rating_line_inputs. No business decisions belong here. */
@Entity
@Table(name = "rating_line_inputs")
public class RatingLineInput {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "rating_id", nullable = false)
    private SupplierRating rating;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "order_version_line_id", nullable = false)
    private OrderVersionLine orderVersionLine;

    @Enumerated(EnumType.STRING)
    @Column(name = "selection_reason", nullable = false, length = 40)
    private SelectionReason selectionReason;

    @Column(name = "ordered_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal orderedQty;

    @Column(name = "delivered_on_time_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal deliveredOnTimeQty;

    @Column(name = "fulfilled_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal fulfilledQty;

    @Column(name = "quality_checked_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal qualityCheckedQty;

    @Column(name = "quality_good_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal qualityGoodQty;

    @Column(name = "timeliness_score", nullable = false, precision = 5, scale = 1)
    private BigDecimal timelinessScore;

    @Column(name = "completeness_score", nullable = false, precision = 5, scale = 1)
    private BigDecimal completenessScore;

    @Column(name = "quality_score", nullable = true, precision = 5, scale = 1)
    private BigDecimal qualityScore;

    @Column(name = "source_cutoff_at", nullable = false)
    private Instant sourceCutoffAt;

    @Column(name = "source_hash", nullable = true, length = 128)
    private String sourceHash;

    public RatingLineInput() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public SupplierRating getRating() { return rating; }
    public void setRating(SupplierRating value) { this.rating = value; }

    public OrderVersionLine getOrderVersionLine() { return orderVersionLine; }
    public void setOrderVersionLine(OrderVersionLine value) { this.orderVersionLine = value; }

    public SelectionReason getSelectionReason() { return selectionReason; }
    public void setSelectionReason(SelectionReason value) { this.selectionReason = value; }

    public BigDecimal getOrderedQty() { return orderedQty; }
    public void setOrderedQty(BigDecimal value) { this.orderedQty = value; }

    public BigDecimal getDeliveredOnTimeQty() { return deliveredOnTimeQty; }
    public void setDeliveredOnTimeQty(BigDecimal value) { this.deliveredOnTimeQty = value; }

    public BigDecimal getFulfilledQty() { return fulfilledQty; }
    public void setFulfilledQty(BigDecimal value) { this.fulfilledQty = value; }

    public BigDecimal getQualityCheckedQty() { return qualityCheckedQty; }
    public void setQualityCheckedQty(BigDecimal value) { this.qualityCheckedQty = value; }

    public BigDecimal getQualityGoodQty() { return qualityGoodQty; }
    public void setQualityGoodQty(BigDecimal value) { this.qualityGoodQty = value; }

    public BigDecimal getTimelinessScore() { return timelinessScore; }
    public void setTimelinessScore(BigDecimal value) { this.timelinessScore = value; }

    public BigDecimal getCompletenessScore() { return completenessScore; }
    public void setCompletenessScore(BigDecimal value) { this.completenessScore = value; }

    public BigDecimal getQualityScore() { return qualityScore; }
    public void setQualityScore(BigDecimal value) { this.qualityScore = value; }

    public Instant getSourceCutoffAt() { return sourceCutoffAt; }
    public void setSourceCutoffAt(Instant value) { this.sourceCutoffAt = value; }

    public String getSourceHash() { return sourceHash; }
    public void setSourceHash(String value) { this.sourceHash = value; }

}
