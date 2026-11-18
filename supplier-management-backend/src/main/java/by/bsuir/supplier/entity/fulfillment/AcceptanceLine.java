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

/** Persisted aggregate/data record for acceptance_lines. No business decisions belong here. */
@Entity
@Table(name = "acceptance_lines")
public class AcceptanceLine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acceptance_document_id", nullable = false)
    private AcceptanceDocument acceptanceDocument;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "delivery_line_id", nullable = false)
    private DeliveryLine deliveryLine;

    @Column(name = "accepted_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal acceptedQty;

    @Column(name = "quality_rejected_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal qualityRejectedQty;

    @Column(name = "other_rejected_qty", nullable = false, precision = 18, scale = 3)
    private BigDecimal otherRejectedQty;

    @Column(name = "other_reason", nullable = true, columnDefinition = "text")
    private String otherReason;

    public AcceptanceLine() {}

    public UUID getId() { return id; }
    public void setId(UUID value) { this.id = value; }

    public AcceptanceDocument getAcceptanceDocument() { return acceptanceDocument; }
    public void setAcceptanceDocument(AcceptanceDocument value) { this.acceptanceDocument = value; }

    public DeliveryLine getDeliveryLine() { return deliveryLine; }
    public void setDeliveryLine(DeliveryLine value) { this.deliveryLine = value; }

    public BigDecimal getAcceptedQty() { return acceptedQty; }
    public void setAcceptedQty(BigDecimal value) { this.acceptedQty = value; }

    public BigDecimal getQualityRejectedQty() { return qualityRejectedQty; }
    public void setQualityRejectedQty(BigDecimal value) { this.qualityRejectedQty = value; }

    public BigDecimal getOtherRejectedQty() { return otherRejectedQty; }
    public void setOtherRejectedQty(BigDecimal value) { this.otherRejectedQty = value; }

    public String getOtherReason() { return otherReason; }
    public void setOtherReason(String value) { this.otherReason = value; }

}
