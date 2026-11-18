package by.bsuir.supplier.repository.supplier;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.supplier.SupplierProduct;
import by.bsuir.supplier.entity.supplier.SupplierProductId;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface SupplierProductRepository extends JpaRepository<SupplierProduct, SupplierProductId> {
    java.util.List<by.bsuir.supplier.entity.supplier.SupplierProduct> findBySupplier_Id(java.util.UUID supplierId);
}
