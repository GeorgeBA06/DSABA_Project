package by.bsuir.supplier.entity.supplier;

import jakarta.persistence.*;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite identity; both columns are real foreign keys. */
@Embeddable
public class SupplierProductId implements Serializable {
    private static final long serialVersionUID = 1L;

    @Column(name = "supplier_id", nullable = false)
    private UUID supplierId;

    @Column(name = "product_id", nullable = false)
    private UUID productId;

    public SupplierProductId() {}

    public SupplierProductId(UUID supplierId, UUID productId) {
        this.supplierId = supplierId;
        this.productId = productId;
    }

    public UUID getSupplierId() {
        return supplierId;
    }

    public void setSupplierId(UUID value) {
        this.supplierId = value;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID value) {
        this.productId = value;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplierProductId that)) return false;
        return Objects.equals(supplierId, that.supplierId)
                && Objects.equals(productId, that.productId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(supplierId, productId);
    }
}
