package by.bsuir.supplier.repository.order;

import by.bsuir.supplier.entity.order.OrderClosureLine;
import by.bsuir.supplier.entity.order.OrderClosureLineId;

import org.springframework.data.jpa.repository.JpaRepository;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface OrderClosureLineRepository
        extends JpaRepository<OrderClosureLine, OrderClosureLineId> {}
