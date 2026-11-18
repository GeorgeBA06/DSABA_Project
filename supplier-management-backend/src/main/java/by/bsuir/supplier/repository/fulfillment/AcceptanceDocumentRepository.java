package by.bsuir.supplier.repository.fulfillment;

import java.util.Optional;
import java.util.UUID;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import by.bsuir.supplier.entity.fulfillment.AcceptanceDocument;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface AcceptanceDocumentRepository extends JpaRepository<AcceptanceDocument, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from AcceptanceDocument e where e.id = :id")
    Optional<AcceptanceDocument> findByIdForUpdate(@Param("id") UUID id);
    java.util.List<by.bsuir.supplier.entity.fulfillment.AcceptanceDocument> findByDelivery_Id(java.util.UUID deliveryId);
    java.util.List<by.bsuir.supplier.entity.fulfillment.AcceptanceDocument> findByCorrection_Id(java.util.UUID correctionId);
}
