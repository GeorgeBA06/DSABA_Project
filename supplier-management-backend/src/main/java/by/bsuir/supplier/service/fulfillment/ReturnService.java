package by.bsuir.supplier.service.fulfillment;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.enums.FactState;
import by.bsuir.supplier.entity.enums.OrderState;
import by.bsuir.supplier.entity.enums.ReturnState;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.fulfillment.AcceptanceLine;
import by.bsuir.supplier.entity.fulfillment.SupplierReturn;
import by.bsuir.supplier.entity.fulfillment.SupplierReturnLine;
import by.bsuir.supplier.entity.order.PurchaseOrder;
import by.bsuir.supplier.repository.fulfillment.*;
import by.bsuir.supplier.repository.order.PurchaseOrderRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class ReturnService {

    private final ActorAccess access;
    private final PurchaseOrderRepository orders;
    private final SupplierReturnRepository returns;
    private final SupplierReturnLineRepository lines;
    private final AcceptanceLineRepository acceptances;
    private final FulfillmentLedger ledger;
    private final AuditTrail audit;

    public ReturnService(
            ActorAccess access,
            PurchaseOrderRepository orders,
            SupplierReturnRepository returns,
            SupplierReturnLineRepository lines,
            AcceptanceLineRepository acceptances,
            FulfillmentLedger ledger,
            AuditTrail audit) {

        this.access = access;
        this.orders = orders;
        this.returns = returns;
        this.lines = lines;
        this.acceptances = acceptances;
        this.ledger = ledger;
        this.audit = audit;
    }

    public record ReturnItem(
            UUID acceptanceLineId, BigDecimal quantity, String defectDescription) {}

    @Transactional
    public SupplierReturn create(
            UUID actorId,
            UUID orderId,
            LocalDate returnedOn,
            String reason,
            List<ReturnItem> items) {

        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);

        PurchaseOrder order = locked(orderId);

        Rules.require(order.getCurrentApprovedVersion() != null, "Заказ не согласован");

        Rules.present(returnedOn, "Дата возврата");

        Rules.require(
                !returnedOn.isAfter(LocalDate.now(ZoneId.of("Europe/Minsk"))),
                "Дата возврата в будущем");

        Rules.notEmpty(items, "Позиции возврата");

        SupplierReturn r = new SupplierReturn();
        r.setOrder(order);
        r.setReturnedOn(returnedOn);
        r.setReason(Rules.text(reason, "Основание возврата"));
        r.setCreatedBy(actor);
        r.setState(ReturnState.DRAFT);

        r = returns.saveAndFlush(r);

        for (ReturnItem item : items) {

            Rules.present(item, "Позиция возврата");
            Rules.positive(item.quantity(), "Количество возврата");

            AcceptanceLine acceptance =
                    acceptances
                            .findById(item.acceptanceLineId())
                            .orElseThrow(() -> new BusinessException("Приёмка не найдена"));

            Rules.require(
                    acceptance.getAcceptanceDocument().getState() == FactState.CONFIRMED,
                    "Нет подтверждённой приёмки");

            Rules.require(
                    !returnedOn.isBefore(acceptance.getAcceptanceDocument().getInspectedOn()),
                    "Возврат не может быть раньше приёмки");

            Rules.require(
                    acceptance.getDeliveryLine().getDelivery().getOrder().getId().equals(orderId),
                    "Строка другого заказа");

            SupplierReturnLine l = new SupplierReturnLine();
            l.setSupplierReturn(r);
            l.setAcceptanceLine(acceptance);
            l.setQuantity(item.quantity());
            l.setDefectDescription(Rules.text(item.defectDescription(), "Описание дефекта"));

            lines.save(l);
        }

        audit.record(actor, "RETURN_CREATED", r, "SupplierReturn", r.getId(), reason);

        return r;
    }

    @Transactional
    public SupplierReturn submit(UUID actorId, UUID returnId) {

        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);

        SupplierReturn r =
                returns.findById(returnId)
                        .orElseThrow(() -> new BusinessException("Возврат не найден"));

        locked(r.getOrder().getId());

        Rules.require(r.getState() == ReturnState.DRAFT, "Возврат уже отправлен");

        Rules.notEmpty(lines.findBySupplierReturn_Id(returnId), "Строки возврата");

        r.setState(ReturnState.PENDING);

        audit.record(actor, "RETURN_SUBMITTED", r, "SupplierReturn", r.getId(), null);

        return r;
    }

    @Transactional
    public SupplierReturn decide(UUID managerId, UUID returnId, boolean approve) {

        User manager = access.requireRole(managerId, RoleCode.PROCUREMENT_MANAGER);

        SupplierReturn r =
                returns.findById(returnId)
                        .orElseThrow(() -> new BusinessException("Возврат не найден"));

        PurchaseOrder order = locked(r.getOrder().getId());

        Rules.require(r.getState() == ReturnState.PENDING, "Возврат не ожидает решения");

        ActorAccess.independent(r.getCreatedBy(), manager);

        if (approve) {

            Rules.require(
                    order.getState() != OrderState.COMPLETED,
                    "Закрытый заказ нужно сначала возобновить");

            FulfillmentLedger.Snapshot facts = ledger.snapshot(order.getId());

            Map<UUID, BigDecimal> requested = new HashMap<>();

            for (SupplierReturnLine l : lines.findBySupplierReturn_Id(returnId)) {

                requested.merge(l.getAcceptanceLine().getId(), l.getQuantity(), BigDecimal::add);
            }

            for (var entry : requested.entrySet()) {

                Rules.require(
                        facts.inspected().stream().anyMatch(x -> x.id().equals(entry.getKey())),
                        "Приёмка неактуальна или исправлена");

                Rules.require(
                        facts.returned(entry.getKey())
                                        .add(entry.getValue())
                                        .compareTo(facts.accepted(entry.getKey()))
                                <= 0,
                        "Возврат превышает доступное ранее принятое количество");
            }
        }

        r.setState(approve ? ReturnState.CONFIRMED : ReturnState.REJECTED);

        if (approve) {

            r.setApprovedBy(manager);
            r.setApprovedAt(Instant.now());

        } else {

            // The schema reserves approved_* for confirmed returns.
            // Rejection actor and timestamp are preserved by the audit event below.
            r.setApprovedBy(null);
            r.setApprovedAt(null);
        }

        audit.record(
                manager,
                approve ? "RETURN_APPROVED" : "RETURN_REJECTED",
                r,
                "SupplierReturn",
                r.getId(),
                null);

        return r;
    }

    private PurchaseOrder locked(UUID id) {
        return orders.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("Заказ не найден"));
    }
}
