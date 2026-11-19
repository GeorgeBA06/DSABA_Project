package by.bsuir.supplier.mapper;

import by.bsuir.supplier.dto.response.OrderClosureView;
import by.bsuir.supplier.dto.response.OrderLineView;
import by.bsuir.supplier.dto.response.OrderVersionLineView;
import by.bsuir.supplier.dto.response.OrderVersionView;
import by.bsuir.supplier.dto.response.PurchaseOrderView;
import by.bsuir.supplier.entity.order.OrderClosure;
import by.bsuir.supplier.entity.order.OrderLine;
import by.bsuir.supplier.entity.order.OrderVersion;
import by.bsuir.supplier.entity.order.OrderVersionLine;
import by.bsuir.supplier.entity.order.PurchaseOrder;

/** Maps persistence entities to immutable public response DTOs. */
public final class OrderViewMapper {
    private OrderViewMapper() {}

    public static OrderVersionView toView(OrderVersion e) {
        return new OrderVersionView(
                e.getId(),
                e.getOrder() == null ? null : e.getOrder().getId(),
                e.getVersionNo(),
                e.getBaseVersion() == null ? null : e.getBaseVersion().getId(),
                e.getContractVersion() == null ? null : e.getContractVersion().getId(),
                e.getKind(),
                e.getInitiator(),
                e.getReason(),
                e.getState(),
                e.getCreatedBy() == null ? null : e.getCreatedBy().getId(),
                e.getCreatedAt(),
                e.getSubmittedAt(),
                e.getDecidedBy() == null ? null : e.getDecidedBy().getId(),
                e.getDecidedAt(),
                e.getDecisionComment());
    }

    public static PurchaseOrderView toView(PurchaseOrder e) {
        return new PurchaseOrderView(
                e.getId(),
                e.getNumber(),
                e.getSupplier() == null ? null : e.getSupplier().getId(),
                e.getContract() == null ? null : e.getContract().getId(),
                e.getState(),
                e.getCurrentApprovedVersion() == null
                        ? null
                        : e.getCurrentApprovedVersion().getId(),
                e.getCreatedBy() == null ? null : e.getCreatedBy().getId(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }

    public static OrderVersionLineView toView(OrderVersionLine e) {
        return new OrderVersionLineView(
                e.getId(),
                e.getOrderVersion() == null ? null : e.getOrderVersion().getId(),
                e.getOrderLine() == null ? null : e.getOrderLine().getId(),
                e.getOrderLine() == null ? null : e.getOrderLine().getProduct().getId(),
                e.getOrderLine() == null ? null : e.getOrderLine().getLineNo(),
                e.getSegmentKey(),
                e.getSourceVersionLine() == null ? null : e.getSourceVersionLine().getId(),
                e.getQuantity(),
                e.getUnitPrice(),
                e.getExecutionDueOn(),
                e.getRatingDueOn(),
                e.getOriginReason(),
                e.getProductNameSnapshot(),
                e.getCategoryCodeSnapshot(),
                e.getCategoryNameSnapshot(),
                e.getUnitCodeSnapshot());
    }

    public static OrderLineView toView(OrderLine e) {
        return new OrderLineView(
                e.getId(),
                e.getOrder() == null ? null : e.getOrder().getId(),
                e.getLineNo(),
                e.getProduct() == null ? null : e.getProduct().getId());
    }

    public static OrderClosureView toView(OrderClosure e) {
        return new OrderClosureView(
                e.getId(),
                e.getOrder() == null ? null : e.getOrder().getId(),
                e.getAction(),
                e.getResult(),
                e.getReason(),
                e.getRequestedBy() == null ? null : e.getRequestedBy().getId(),
                e.getDecidedBy() == null ? null : e.getDecidedBy().getId(),
                e.getOccurredAt());
    }
}
