package by.bsuir.supplier.repository.fulfillment;

import by.bsuir.supplier.entity.fulfillment.SupplierReturnLine;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface SupplierReturnLineRepository extends JpaRepository<SupplierReturnLine, UUID> {
    java.util.List<by.bsuir.supplier.entity.fulfillment.SupplierReturnLine> findBySupplierReturn_Id(
            java.util.UUID returnId);
}
