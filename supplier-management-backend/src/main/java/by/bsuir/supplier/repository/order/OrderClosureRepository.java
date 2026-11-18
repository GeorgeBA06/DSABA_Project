package by.bsuir.supplier.repository.order;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import by.bsuir.supplier.entity.order.OrderClosure;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface OrderClosureRepository extends JpaRepository<OrderClosure, UUID> {
}
