package by.bsuir.supplier.repository.fulfillment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.fulfillment.FactCorrection;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface FactCorrectionRepository extends JpaRepository<FactCorrection, UUID> {
    @org.springframework.data.jpa.repository.Query("""
        select count(f) from FactCorrection f
        where f.state in :states
        and (exists (select d.id from Delivery d where d.correction = f and d.order.id = :orderId)
          or exists (select a.id from AcceptanceDocument a where a.correction = f and a.delivery.order.id = :orderId))
        """)
    long countOpenForOrder(@org.springframework.data.repository.query.Param("orderId") java.util.UUID orderId,
        @org.springframework.data.repository.query.Param("states") java.util.Collection<by.bsuir.supplier.entity.enums.VersionState> states);
}
