package by.bsuir.supplier.service.order;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.enums.ClosureAction;
import by.bsuir.supplier.entity.enums.ClosureResult;
import by.bsuir.supplier.entity.enums.OrderState;
import by.bsuir.supplier.entity.enums.OrderVersionKind;
import by.bsuir.supplier.entity.enums.ReturnState;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.VersionState;
import by.bsuir.supplier.entity.order.OrderClosure;
import by.bsuir.supplier.entity.order.OrderClosureLine;
import by.bsuir.supplier.entity.order.OrderClosureLineId;
import by.bsuir.supplier.entity.order.OrderLine;
import by.bsuir.supplier.entity.order.OrderVersionLine;
import by.bsuir.supplier.entity.order.PurchaseOrder;
import by.bsuir.supplier.repository.fulfillment.*;
import by.bsuir.supplier.repository.order.*;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;
import by.bsuir.supplier.service.fulfillment.FulfillmentLedger;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
public class OrderClosureService {
    private final ActorAccess access;
    private final PurchaseOrderRepository orders;
    private final OrderLineRepository lines;
    private final OrderVersionRepository versions;
    private final OrderVersionLineRepository segments;
    private final OrderClosureRepository closures;
    private final OrderClosureLineRepository closureLines;
    private final FulfillmentLedger ledger;
    private final AuditTrail audit;
    private final by.bsuir.supplier.repository.fulfillment.SupplierReturnRepository returns;
    private final by.bsuir.supplier.repository.fulfillment.FactCorrectionRepository corrections;

    public OrderClosureService(
            ActorAccess access,
            PurchaseOrderRepository orders,
            OrderLineRepository lines,
            OrderVersionRepository versions,
            OrderVersionLineRepository segments,
            OrderClosureRepository closures,
            OrderClosureLineRepository closureLines,
            FulfillmentLedger ledger,
            AuditTrail audit,
            by.bsuir.supplier.repository.fulfillment.SupplierReturnRepository returns,
            by.bsuir.supplier.repository.fulfillment.FactCorrectionRepository corrections) {
        this.access = access;
        this.orders = orders;
        this.lines = lines;
        this.versions = versions;
        this.segments = segments;
        this.closures = closures;
        this.closureLines = closureLines;
        this.ledger = ledger;
        this.audit = audit;
        this.returns = returns;
        this.corrections = corrections;
    }

    @Transactional
    public OrderClosure close(UUID actorId, UUID orderId, String reason) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_MANAGER);
        PurchaseOrder o = locked(orderId);
        Rules.require(
                o.getState() == OrderState.APPROVED || o.getState() == OrderState.IN_PROGRESS,
                "Закрывается только согласованный заказ");
        ActorAccess.independent(o.getCreatedBy(), actor);
        Rules.require(o.getCurrentApprovedVersion() != null, "Нет согласованной версии");
        Rules.require(
                versions.findByOrder_Id(orderId).stream()
                        .noneMatch(
                                v ->
                                        v.getKind() == OrderVersionKind.CHANGE
                                                && (v.getState() == VersionState.DRAFT
                                                        || v.getState() == VersionState.PENDING)),
                "Есть открытое изменение");
        Rules.require(
                returns.findByOrder_Id(orderId).stream()
                        .noneMatch(r -> r.getState() == ReturnState.PENDING),
                "Есть ожидающие решения возвраты");
        Rules.require(
                corrections.countOpenForOrder(
                                orderId,
                                java.util.EnumSet.of(VersionState.DRAFT, VersionState.PENDING))
                        == 0,
                "Есть ожидающие решения исправления");
        FulfillmentLedger.Snapshot facts = ledger.snapshot(orderId);
        Rules.require(!facts.hasUninspected(), "Есть не прошедший проверку товар");
        Map<UUID, BigDecimal> ordered = new HashMap<>();
        for (OrderVersionLine s :
                segments.findByOrderVersion_Id(o.getCurrentApprovedVersion().getId()))
            ordered.merge(s.getOrderLine().getId(), s.getQuantity(), BigDecimal::add);
        boolean remainder = false;
        for (var entry : ordered.entrySet())
            if (facts.fulfilled(entry.getKey()).compareTo(entry.getValue()) < 0) remainder = true;
        if (remainder) Rules.text(reason, "Причина закрытия с остатком");
        OrderClosure c = new OrderClosure();
        c.setOrder(o);
        c.setAction(ClosureAction.CLOSE);
        c.setResult(remainder ? ClosureResult.CLOSED_WITH_REMAINDER : ClosureResult.FULFILLED);
        c.setReason(reason);
        c.setDecidedBy(actor);
        c.setOccurredAt(Instant.now());
        c = closures.saveAndFlush(c);
        for (OrderLine line : lines.findByOrder_Id(orderId)) {
            BigDecimal required = ordered.getOrDefault(line.getId(), BigDecimal.ZERO);
            BigDecimal fulfilled = facts.fulfilled(line.getId());
            BigDecimal diff = required.subtract(fulfilled).max(BigDecimal.ZERO);
            OrderClosureLine row =
                    new OrderClosureLine(new OrderClosureLineId(c.getId(), line.getId()));
            row.setClosure(c);
            row.setOrderLine(line);
            row.setOrderedQty(required);
            row.setFulfilledQty(fulfilled);
            row.setUnfulfilledQty(diff);
            closureLines.save(row);
        }
        o.setState(OrderState.COMPLETED);
        audit.record(actor, "ORDER_CLOSED", o, "PurchaseOrder", orderId, reason);
        return c;
    }

    @Transactional
    public OrderClosure reopen(UUID actorId, UUID orderId, String reason) {
        User manager = access.requireRole(actorId, RoleCode.PROCUREMENT_MANAGER);
        PurchaseOrder o = locked(orderId);
        Rules.require(o.getState() == OrderState.COMPLETED, "Заказ не закрыт");
        OrderClosure c = new OrderClosure();
        c.setOrder(o);
        c.setAction(ClosureAction.REOPEN);
        c.setReason(Rules.text(reason, "Причина возобновления"));
        c.setDecidedBy(manager);
        c.setOccurredAt(Instant.now());
        c = closures.saveAndFlush(c);
        o.setState(OrderState.IN_PROGRESS);
        audit.record(manager, "ORDER_REOPENED", o, "PurchaseOrder", orderId, reason);
        return c;
    }

    private PurchaseOrder locked(UUID id) {
        return orders.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("Заказ не найден"));
    }
}
