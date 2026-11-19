package by.bsuir.supplier.repository.order;

import by.bsuir.supplier.entity.order.PurchaseOrder;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from PurchaseOrder e where e.id = :id")
    Optional<PurchaseOrder> findByIdForUpdate(@Param("id") UUID id);

    java.util.List<by.bsuir.supplier.entity.order.PurchaseOrder> findBySupplier_Id(
            java.util.UUID supplierId);
}
