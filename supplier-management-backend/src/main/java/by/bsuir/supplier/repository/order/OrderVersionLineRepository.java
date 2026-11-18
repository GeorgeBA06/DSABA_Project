package by.bsuir.supplier.repository.order;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.order.OrderVersionLine;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface OrderVersionLineRepository extends JpaRepository<OrderVersionLine, UUID> {
    java.util.List<by.bsuir.supplier.entity.order.OrderVersionLine> findByOrderVersion_Id(java.util.UUID versionId);
}
