package by.bsuir.supplier.entity.rating;

import by.bsuir.supplier.entity.catalog.*;
import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.entity.supplier.*;

import jakarta.persistence.*;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

/** Persisted aggregate/data record for supplier_ratings. No business decisions belong here. */
@Entity
@Table(name = "supplier_ratings")
public class SupplierRating {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false, length = 40)
    private RatingScope scope;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "category_id", nullable = true)
    private ProductCategory category;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "as_of_at", nullable = false)
    private Instant asOfAt;

    @Column(name = "method_version", nullable = false, length = 40)
    private String methodVersion;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "rules_snapshot", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> rulesSnapshot;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "expert_assessment_id", nullable = true)
    private ExpertAssessment expertAssessment;

    @Column(name = "timeliness_score", nullable = true, precision = 5, scale = 1)
    private BigDecimal timelinessScore;

    @Column(name = "completeness_score", nullable = true, precision = 5, scale = 1)
    private BigDecimal completenessScore;

    @Column(name = "quality_score", nullable = true, precision = 5, scale = 1)
    private BigDecimal qualityScore;

    @Column(name = "interaction_score", nullable = true, precision = 5, scale = 1)
    private BigDecimal interactionScore;

    @Column(name = "total_score", nullable = true, precision = 5, scale = 1)
    private BigDecimal totalScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "band", nullable = true, length = 40)
    private RatingBand band;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private RatingState state;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "missing_reasons", nullable = true, columnDefinition = "jsonb")
    private Map<String, Object> missingReasons;

    @Column(name = "order_count", nullable = false)
    private Integer orderCount;

    @Column(name = "line_count", nullable = false)
    private Integer lineCount;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "supersedes_id", nullable = true)
    private SupplierRating supersedes;

    public SupplierRating() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier value) {
        this.supplier = value;
    }

    public RatingScope getScope() {
        return scope;
    }

    public void setScope(RatingScope value) {
        this.scope = value;
    }

    public ProductCategory getCategory() {
        return category;
    }

    public void setCategory(ProductCategory value) {
        this.category = value;
    }

    public LocalDate getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDate value) {
        this.periodStart = value;
    }

    public LocalDate getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDate value) {
        this.periodEnd = value;
    }

    public Instant getAsOfAt() {
        return asOfAt;
    }

    public void setAsOfAt(Instant value) {
        this.asOfAt = value;
    }

    public String getMethodVersion() {
        return methodVersion;
    }

    public void setMethodVersion(String value) {
        this.methodVersion = value;
    }

    public Map<String, Object> getRulesSnapshot() {
        return rulesSnapshot;
    }

    public void setRulesSnapshot(Map<String, Object> value) {
        this.rulesSnapshot = value;
    }

    public ExpertAssessment getExpertAssessment() {
        return expertAssessment;
    }

    public void setExpertAssessment(ExpertAssessment value) {
        this.expertAssessment = value;
    }

    public BigDecimal getTimelinessScore() {
        return timelinessScore;
    }

    public void setTimelinessScore(BigDecimal value) {
        this.timelinessScore = value;
    }

    public BigDecimal getCompletenessScore() {
        return completenessScore;
    }

    public void setCompletenessScore(BigDecimal value) {
        this.completenessScore = value;
    }

    public BigDecimal getQualityScore() {
        return qualityScore;
    }

    public void setQualityScore(BigDecimal value) {
        this.qualityScore = value;
    }

    public BigDecimal getInteractionScore() {
        return interactionScore;
    }

    public void setInteractionScore(BigDecimal value) {
        this.interactionScore = value;
    }

    public BigDecimal getTotalScore() {
        return totalScore;
    }

    public void setTotalScore(BigDecimal value) {
        this.totalScore = value;
    }

    public RatingBand getBand() {
        return band;
    }

    public void setBand(RatingBand value) {
        this.band = value;
    }

    public RatingState getState() {
        return state;
    }

    public void setState(RatingState value) {
        this.state = value;
    }

    public Map<String, Object> getMissingReasons() {
        return missingReasons;
    }

    public void setMissingReasons(Map<String, Object> value) {
        this.missingReasons = value;
    }

    public Integer getOrderCount() {
        return orderCount;
    }

    public void setOrderCount(Integer value) {
        this.orderCount = value;
    }

    public Integer getLineCount() {
        return lineCount;
    }

    public void setLineCount(Integer value) {
        this.lineCount = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }

    public SupplierRating getSupersedes() {
        return supersedes;
    }

    public void setSupersedes(SupplierRating value) {
        this.supersedes = value;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
