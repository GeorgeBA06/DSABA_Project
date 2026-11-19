package by.bsuir.supplier.service.fulfillment;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.enums.FactState;
import by.bsuir.supplier.entity.enums.OrderState;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.fulfillment.AcceptanceDocument;
import by.bsuir.supplier.entity.fulfillment.AcceptanceLine;
import by.bsuir.supplier.entity.fulfillment.Delivery;
import by.bsuir.supplier.entity.fulfillment.DeliveryLine;
import by.bsuir.supplier.entity.order.OrderVersionLine;
import by.bsuir.supplier.entity.order.PurchaseOrder;
import by.bsuir.supplier.repository.fulfillment.*;
import by.bsuir.supplier.repository.order.*;
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
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** Register physical receipts first; inspected quantities only become fulfilled on confirmation. */
@Service
public class ReceivingService {
    private final ActorAccess access;
    private final PurchaseOrderRepository orders;
    private final OrderVersionLineRepository segments;
    private final DeliveryRepository deliveries;
    private final DeliveryLineRepository deliveryLines;
    private final AcceptanceDocumentRepository acceptances;
    private final AcceptanceLineRepository acceptanceLines;
    private final FulfillmentLedger ledger;
    private final AuditTrail audit;

    public ReceivingService(
            ActorAccess access,
            PurchaseOrderRepository orders,
            OrderVersionLineRepository segments,
            DeliveryRepository deliveries,
            DeliveryLineRepository deliveryLines,
            AcceptanceDocumentRepository acceptances,
            AcceptanceLineRepository acceptanceLines,
            FulfillmentLedger ledger,
            AuditTrail audit) {
        this.access = access;
        this.orders = orders;
        this.segments = segments;
        this.deliveries = deliveries;
        this.deliveryLines = deliveryLines;
        this.acceptances = acceptances;
        this.acceptanceLines = acceptanceLines;
        this.ledger = ledger;
        this.audit = audit;
    }

    public record ReceivedItem(UUID segmentId, BigDecimal quantity) {}

    public record InspectionItem(
            UUID deliveryLineId,
            BigDecimal accepted,
            BigDecimal rejectedQuality,
            BigDecimal rejectedOther,
            String reason) {}

    @Transactional
    public Delivery registerDelivery(
            UUID actorId,
            UUID orderId,
            LocalDate arrivedOn,
            String externalNumber,
            List<ReceivedItem> items) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        PurchaseOrder o = locked(orderId);
        ensureExecuting(o);
        Rules.present(arrivedOn, "Дата поставки");
        Rules.require(
                !arrivedOn.isAfter(LocalDate.now(ZoneId.of("Europe/Minsk"))),
                "Дата поступления в будущем");
        Rules.notEmpty(items, "Строки поставки");
        Set<UUID> allowed =
                segments.findByOrderVersion_Id(o.getCurrentApprovedVersion().getId()).stream()
                        .map(OrderVersionLine::getId)
                        .collect(Collectors.toSet());
        Delivery d = new Delivery();
        d.setOrder(o);
        d.setArrivedOn(arrivedOn);
        d.setExternalNumber(externalNumber);
        d.setRegisteredBy(actor);
        d.setRegisteredAt(Instant.now());
        d.setState(FactState.DRAFT);
        d = deliveries.saveAndFlush(d);
        for (ReceivedItem item : items) {
            Rules.present(item, "Строка поставки");
            Rules.positive(item.quantity(), "Полученное количество");
            Rules.require(
                    allowed.contains(item.segmentId()),
                    "Сегмент не относится к текущему согласованному заказу");
            OrderVersionLine s =
                    segments.findById(item.segmentId())
                            .orElseThrow(() -> new BusinessException("Сегмент не найден"));
            DeliveryLine line = new DeliveryLine();
            line.setDelivery(d);
            line.setOrderVersionLine(s);
            line.setQuantity(item.quantity());
            deliveryLines.save(line);
        }
        audit.record(actor, "DELIVERY_REGISTERED", d, "Delivery", d.getId(), null);
        return d;
    }

    @Transactional
    public Delivery confirmDelivery(UUID actorId, UUID deliveryId) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Delivery d =
                deliveries
                        .findById(deliveryId)
                        .orElseThrow(() -> new BusinessException("Поставка не найдена"));
        PurchaseOrder o = locked(d.getOrder().getId());
        ensureExecuting(o);
        Rules.require(d.getState() == FactState.DRAFT, "Поставка уже подтверждена");
        Rules.require(
                d.getCorrection() == null,
                "Исправление подтверждается через отдельное согласование");
        Rules.notEmpty(deliveryLines.findByDelivery_Id(deliveryId), "Строки поставки");
        d.setState(FactState.CONFIRMED);
        o.setState(OrderState.IN_PROGRESS);
        audit.record(actor, "DELIVERY_CONFIRMED", d, "Delivery", d.getId(), null);
        return d;
    }

    @Transactional
    public AcceptanceDocument registerInspection(
            UUID actorId, UUID deliveryId, LocalDate inspectedOn, List<InspectionItem> items) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Delivery d =
                deliveries
                        .findById(deliveryId)
                        .orElseThrow(() -> new BusinessException("Поставка не найдена"));
        locked(d.getOrder().getId());
        Rules.require(d.getState() == FactState.CONFIRMED, "Поставка не подтверждена");
        Rules.present(inspectedOn, "Дата проверки");
        Rules.require(
                !inspectedOn.isBefore(d.getArrivedOn())
                        && !inspectedOn.isAfter(LocalDate.now(ZoneId.of("Europe/Minsk"))),
                "Дата проверки вне допустимого диапазона");
        Rules.require(
                deliveries.findByOrder_Id(d.getOrder().getId()).stream()
                        .noneMatch(
                                x ->
                                        x.getSupersedes() != null
                                                && x.getSupersedes().getId().equals(deliveryId)
                                                && x.getState() != FactState.CANCELLED),
                "Поступление находится на исправлении");
        Rules.notEmpty(items, "Строки приёмки");
        Set<UUID> allowed =
                deliveryLines.findByDelivery_Id(deliveryId).stream()
                        .map(DeliveryLine::getId)
                        .collect(Collectors.toSet());
        AcceptanceDocument a = new AcceptanceDocument();
        a.setDelivery(d);
        a.setInspectedOn(inspectedOn);
        a.setRegisteredBy(actor);
        a.setRegisteredAt(Instant.now());
        a.setState(FactState.DRAFT);
        a = acceptances.saveAndFlush(a);
        for (InspectionItem i : items) {
            Rules.present(i, "Строка проверки");
            Rules.require(allowed.contains(i.deliveryLineId()), "Строка другой поставки");
            Rules.nonnegative(i.accepted(), "Принято");
            Rules.nonnegative(i.rejectedQuality(), "Брак");
            Rules.nonnegative(i.rejectedOther(), "Прочие отклонения");
            BigDecimal sum = i.accepted().add(i.rejectedQuality()).add(i.rejectedOther());
            Rules.positive(sum, "Количество для проверки");
            if (i.rejectedOther().signum() > 0)
                Rules.text(i.reason(), "Причина прочего отклонения");
            AcceptanceLine l = new AcceptanceLine();
            l.setAcceptanceDocument(a);
            l.setDeliveryLine(deliveryLines.findById(i.deliveryLineId()).orElseThrow());
            l.setAcceptedQty(i.accepted());
            l.setQualityRejectedQty(i.rejectedQuality());
            l.setOtherRejectedQty(i.rejectedOther());
            l.setOtherReason(i.reason());
            acceptanceLines.save(l);
        }
        audit.record(actor, "ACCEPTANCE_REGISTERED", a, "AcceptanceDocument", a.getId(), null);
        return a;
    }

    @Transactional
    public AcceptanceDocument confirmInspection(UUID actorId, UUID acceptanceId) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        AcceptanceDocument a =
                acceptances
                        .findById(acceptanceId)
                        .orElseThrow(() -> new BusinessException("Приёмка не найдена"));
        PurchaseOrder o = locked(a.getDelivery().getOrder().getId());
        ensureExecuting(o);
        Rules.require(a.getState() == FactState.DRAFT, "Приёмка уже обработана");
        Rules.require(
                a.getCorrection() == null,
                "Исправление подтверждается через отдельное согласование");
        Rules.require(
                a.getDelivery().getState() == FactState.CONFIRMED, "Поставка не подтверждена");
        List<AcceptanceLine> proposed = acceptanceLines.findByAcceptanceDocument_Id(acceptanceId);
        Rules.notEmpty(proposed, "Строки приёмки");
        FulfillmentLedger.Snapshot facts = ledger.snapshot(o.getId());
        Map<UUID, BigDecimal> additions = new HashMap<>();
        Map<UUID, BigDecimal> acceptedByLine = new HashMap<>();
        Map<String, BigDecimal> acceptedBySegment = new HashMap<>();
        for (AcceptanceLine l : proposed) {
            UUID dlId = l.getDeliveryLine().getId();
            BigDecimal total =
                    l.getAcceptedQty().add(l.getQualityRejectedQty()).add(l.getOtherRejectedQty());
            additions.merge(dlId, total, BigDecimal::add);
            acceptedByLine.merge(
                    l.getDeliveryLine().getOrderVersionLine().getOrderLine().getId(),
                    l.getAcceptedQty(),
                    BigDecimal::add);
            acceptedBySegment.merge(
                    l.getDeliveryLine().getOrderVersionLine().getSegmentKey(),
                    l.getAcceptedQty(),
                    BigDecimal::add);
        }
        for (var e : additions.entrySet()) {
            BigDecimal delivered = facts.delivered(e.getKey());
            Rules.require(
                    delivered.signum() > 0
                            && facts.inspected(e.getKey()).add(e.getValue()).compareTo(delivered)
                                    <= 0,
                    "Приёмка превышает количество поступившего товара");
        }
        List<OrderVersionLine> active =
                segments.findByOrderVersion_Id(o.getCurrentApprovedVersion().getId());
        for (var entry : acceptedBySegment.entrySet()) {
            OrderVersionLine segment =
                    active.stream()
                            .filter(x -> x.getSegmentKey().equals(entry.getKey()))
                            .findFirst()
                            .orElseThrow(
                                    () ->
                                            new BusinessException(
                                                    "Сегмент отсутствует в текущей версии"));
            Rules.require(
                    facts.netBySegment(entry.getKey())
                                    .add(entry.getValue())
                                    .compareTo(segment.getQuantity())
                            <= 0,
                    "Принятое количество превышает обязательство по сегменту");
        }
        for (var e : acceptedByLine.entrySet()) {
            BigDecimal ordered =
                    segments.findByOrderVersion_Id(o.getCurrentApprovedVersion().getId()).stream()
                            .filter(s -> s.getOrderLine().getId().equals(e.getKey()))
                            .map(OrderVersionLine::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
            Rules.require(
                    facts.fulfilled(e.getKey()).add(e.getValue()).compareTo(ordered) <= 0,
                    "Принятое количество превышает согласованное обязательство");
        }
        a.setState(FactState.CONFIRMED);
        audit.record(actor, "ACCEPTANCE_CONFIRMED", a, "AcceptanceDocument", a.getId(), null);
        return a;
    }

    private PurchaseOrder locked(UUID id) {
        return orders.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("Заказ не найден"));
    }

    private static void ensureExecuting(PurchaseOrder o) {
        Rules.require(
                o.getState() == OrderState.APPROVED || o.getState() == OrderState.IN_PROGRESS,
                "Заказ не находится в исполнении");
    }
}
