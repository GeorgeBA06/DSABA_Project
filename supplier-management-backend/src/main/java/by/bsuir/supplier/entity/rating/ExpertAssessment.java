package by.bsuir.supplier.entity.rating;

import by.bsuir.supplier.entity.auth.*;
import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.entity.supplier.*;

import jakarta.persistence.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/** Persisted aggregate/data record for expert_assessments. No business decisions belong here. */
@Entity
@Table(name = "expert_assessments")
public class ExpertAssessment {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @Column(name = "period_start", nullable = false)
    private LocalDate periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDate periodEnd;

    @Column(name = "applicable_from", nullable = false)
    private LocalDate applicableFrom;

    @Column(name = "applicable_to", nullable = false)
    private LocalDate applicableTo;

    @Column(name = "score", nullable = false)
    private Integer score;

    @Column(name = "justification", nullable = false, columnDefinition = "text")
    private String justification;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private VersionState state;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "approved_by_id", nullable = true)
    private User approvedBy;

    @Column(name = "approved_at", nullable = true)
    private Instant approvedAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "supersedes_id", nullable = true)
    private ExpertAssessment supersedes;

    public ExpertAssessment() {}

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

    public LocalDate getApplicableFrom() {
        return applicableFrom;
    }

    public void setApplicableFrom(LocalDate value) {
        this.applicableFrom = value;
    }

    public LocalDate getApplicableTo() {
        return applicableTo;
    }

    public void setApplicableTo(LocalDate value) {
        this.applicableTo = value;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer value) {
        this.score = value;
    }

    public String getJustification() {
        return justification;
    }

    public void setJustification(String value) {
        this.justification = value;
    }

    public VersionState getState() {
        return state;
    }

    public void setState(VersionState value) {
        this.state = value;
    }

    public User getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(User value) {
        this.createdBy = value;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant value) {
        this.createdAt = value;
    }

    public User getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(User value) {
        this.approvedBy = value;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant value) {
        this.approvedAt = value;
    }

    public ExpertAssessment getSupersedes() {
        return supersedes;
    }

    public void setSupersedes(ExpertAssessment value) {
        this.supersedes = value;
    }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }
}
