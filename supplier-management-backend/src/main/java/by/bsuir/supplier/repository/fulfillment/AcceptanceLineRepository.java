package by.bsuir.supplier.repository.fulfillment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.fulfillment.AcceptanceLine;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface AcceptanceLineRepository extends JpaRepository<AcceptanceLine, UUID> {
    java.util.List<by.bsuir.supplier.entity.fulfillment.AcceptanceLine> findByAcceptanceDocument_Id(java.util.UUID acceptanceId);
}
