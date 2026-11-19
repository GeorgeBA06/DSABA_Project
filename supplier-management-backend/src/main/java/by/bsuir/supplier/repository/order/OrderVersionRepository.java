package by.bsuir.supplier.repository.order;

import by.bsuir.supplier.entity.order.OrderVersion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface OrderVersionRepository extends JpaRepository<OrderVersion, UUID> {
    java.util.List<by.bsuir.supplier.entity.order.OrderVersion> findByOrder_Id(
            java.util.UUID orderId);
}
