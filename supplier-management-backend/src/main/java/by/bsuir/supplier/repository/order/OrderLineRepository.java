package by.bsuir.supplier.repository.order;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.order.OrderLine;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface OrderLineRepository extends JpaRepository<OrderLine, UUID> {
    java.util.List<by.bsuir.supplier.entity.order.OrderLine> findByOrder_Id(java.util.UUID orderId);
}
