package by.bsuir.supplier.repository.order;

import by.bsuir.supplier.entity.order.OrderClosure;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface OrderClosureRepository extends JpaRepository<OrderClosure, UUID> {}
