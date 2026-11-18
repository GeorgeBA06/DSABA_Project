package by.bsuir.supplier.repository.fulfillment;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.fulfillment.DeliveryLine;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface DeliveryLineRepository extends JpaRepository<DeliveryLine, UUID> {
    java.util.List<by.bsuir.supplier.entity.fulfillment.DeliveryLine> findByDelivery_Id(java.util.UUID deliveryId);
}
