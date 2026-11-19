package by.bsuir.supplier.repository.fulfillment;

import by.bsuir.supplier.entity.fulfillment.Delivery;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface DeliveryRepository extends JpaRepository<Delivery, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Delivery e where e.id = :id")
    Optional<Delivery> findByIdForUpdate(@Param("id") UUID id);

    java.util.List<by.bsuir.supplier.entity.fulfillment.Delivery> findByOrder_Id(
            java.util.UUID orderId);

    java.util.List<by.bsuir.supplier.entity.fulfillment.Delivery> findByCorrection_Id(
            java.util.UUID correctionId);
}
