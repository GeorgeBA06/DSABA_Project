package by.bsuir.supplier.entity.contract;

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

/** Persisted aggregate/data record for contract_versions. No business decisions belong here. */
@Entity
@Table(name = "contract_versions")
public class ContractVersion {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "contract_id", nullable = false)
    private Contract contract;

    @Column(name = "version_no", nullable = false)
    private Integer versionNo;

    @Column(name = "number", nullable = false, length = 100)
    private String number;

    @Column(name = "signed_on", nullable = true)
    private LocalDate signedOn;

    @Column(name = "valid_from", nullable = false)
    private LocalDate validFrom;

    @Column(name = "valid_to", nullable = false)
    private LocalDate validTo;

    @Column(name = "terms", nullable = false, columnDefinition = "text")
    private String terms;

    @Column(name = "change_reason", nullable = true, columnDefinition = "text")
    private String changeReason;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false, length = 40)
    private VersionState state;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "created_by_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "decided_by_id", nullable = true)
    private User decidedBy;

    @Column(name = "decided_at", nullable = true)
    private Instant decidedAt;

    @Column(name = "decision_comment", nullable = true, columnDefinition = "text")
    private String decisionComment;

    public ContractVersion() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public Contract getContract() { return contract; }
    public void setContract(Contract value) { this.contract = value; }

    public Integer getVersionNo() { return versionNo; }
    public void setVersionNo(Integer value) { this.versionNo = value; }

    public String getNumber() { return number; }
    public void setNumber(String value) { this.number = value; }

    public LocalDate getSignedOn() { return signedOn; }
    public void setSignedOn(LocalDate value) { this.signedOn = value; }

    public LocalDate getValidFrom() { return validFrom; }
    public void setValidFrom(LocalDate value) { this.validFrom = value; }

    public LocalDate getValidTo() { return validTo; }
    public void setValidTo(LocalDate value) { this.validTo = value; }

    public String getTerms() { return terms; }
    public void setTerms(String value) { this.terms = value; }

    public String getChangeReason() { return changeReason; }
    public void setChangeReason(String value) { this.changeReason = value; }

    public VersionState getState() { return state; }
    public void setState(VersionState value) { this.state = value; }

    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { this.createdBy = value; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant value) { this.createdAt = value; }

    public User getDecidedBy() { return decidedBy; }
    public void setDecidedBy(User value) { this.decidedBy = value; }

    public Instant getDecidedAt() { return decidedAt; }
    public void setDecidedAt(Instant value) { this.decidedAt = value; }

    public String getDecisionComment() { return decisionComment; }
    public void setDecisionComment(String value) { this.decisionComment = value; }

    @PrePersist
    private void prePersist() {
        if (createdAt == null) createdAt = Instant.now();
    }

}
