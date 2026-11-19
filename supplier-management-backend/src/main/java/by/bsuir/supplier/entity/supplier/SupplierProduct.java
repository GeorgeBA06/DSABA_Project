package by.bsuir.supplier.entity.supplier;

import by.bsuir.supplier.entity.catalog.*;
import by.bsuir.supplier.entity.enums.*;

import jakarta.persistence.*;

/** Persisted aggregate/data record for supplier_products. No business decisions belong here. */
@Entity
@Table(name = "supplier_products")
public class SupplierProduct {
    @EmbeddedId private SupplierProductId id;

    @MapsId("supplierId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id", nullable = false)
    private Supplier supplier;

    @MapsId("productId")
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "product_id", nullable = false)
    private Product product;

    @Column(name = "supplier_item_code", nullable = true, length = 80)
    private String supplierItemCode;

    @Column(name = "active", nullable = false)
    private Boolean active;

    public SupplierProduct() {}

    public SupplierProduct(SupplierProductId id) {
        this.id = id;
    }

    public SupplierProductId getId() {
        return id;
    }

    public void setId(SupplierProductId value) {
        this.id = value;
    }

    public Supplier getSupplier() {
        return supplier;
    }

    public void setSupplier(Supplier value) {
        this.supplier = value;
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product value) {
        this.product = value;
    }

    public String getSupplierItemCode() {
        return supplierItemCode;
    }

    public void setSupplierItemCode(String value) {
        this.supplierItemCode = value;
    }

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean value) {
        this.active = value;
    }
}
