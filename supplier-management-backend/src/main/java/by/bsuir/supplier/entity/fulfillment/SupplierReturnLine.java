package by.bsuir.supplier.entity.fulfillment;

import by.bsuir.supplier.entity.claim.*;
import by.bsuir.supplier.entity.enums.*;

import jakarta.persistence.*;

import java.math.BigDecimal;
import java.util.UUID;

/** Persisted aggregate/data record for supplier_return_lines. No business decisions belong here. */
@Entity
@Table(name = "supplier_return_lines")
public class SupplierReturnLine {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "return_id", nullable = false)
    private SupplierReturn supplierReturn;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "acceptance_line_id", nullable = false)
    private AcceptanceLine acceptanceLine;

    @Column(name = "quantity", nullable = false, precision = 18, scale = 3)
    private BigDecimal quantity;

    @Column(name = "defect_description", nullable = false, columnDefinition = "text")
    private String defectDescription;

    @ManyToOne(fetch = FetchType.LAZY, optional = true)
    @JoinColumn(name = "claim_id", nullable = true)
    private Claim claim;

    public SupplierReturnLine() {}

    public UUID getId() {
        return id;
    }

    public void setId(UUID value) {
        this.id = value;
    }

    public SupplierReturn getSupplierReturn() {
        return supplierReturn;
    }

    public void setSupplierReturn(SupplierReturn value) {
        this.supplierReturn = value;
    }

    public AcceptanceLine getAcceptanceLine() {
        return acceptanceLine;
    }

    public void setAcceptanceLine(AcceptanceLine value) {
        this.acceptanceLine = value;
    }

    public BigDecimal getQuantity() {
        return quantity;
    }

    public void setQuantity(BigDecimal value) {
        this.quantity = value;
    }

    public String getDefectDescription() {
        return defectDescription;
    }

    public void setDefectDescription(String value) {
        this.defectDescription = value;
    }

    public Claim getClaim() {
        return claim;
    }

    public void setClaim(Claim value) {
        this.claim = value;
    }
}
