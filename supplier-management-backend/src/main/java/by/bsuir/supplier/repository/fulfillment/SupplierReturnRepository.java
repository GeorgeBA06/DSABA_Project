package by.bsuir.supplier.repository.fulfillment;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import by.bsuir.supplier.entity.fulfillment.SupplierReturn;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface SupplierReturnRepository extends JpaRepository<SupplierReturn, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from SupplierReturn e where e.id = :id")
    Optional<SupplierReturn> findByIdForUpdate(@Param("id") UUID id);
    java.util.List<by.bsuir.supplier.entity.fulfillment.SupplierReturn> findByOrder_Id(java.util.UUID orderId);
}
