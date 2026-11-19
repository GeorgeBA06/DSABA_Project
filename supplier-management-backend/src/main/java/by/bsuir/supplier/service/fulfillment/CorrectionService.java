package by.bsuir.supplier.service.fulfillment;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.enums.FactState;
import by.bsuir.supplier.entity.enums.ReturnState;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.VersionState;
import by.bsuir.supplier.entity.fulfillment.AcceptanceDocument;
import by.bsuir.supplier.entity.fulfillment.AcceptanceLine;
import by.bsuir.supplier.entity.fulfillment.Delivery;
import by.bsuir.supplier.entity.fulfillment.DeliveryLine;
import by.bsuir.supplier.entity.fulfillment.FactCorrection;
import by.bsuir.supplier.entity.fulfillment.SupplierReturn;
import by.bsuir.supplier.entity.fulfillment.SupplierReturnLine;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/** One confirmed receipt or inspection per correction application. No historical overwrite. */
@Service
public class CorrectionService {

    private final ActorAccess access;
    private final PurchaseOrderRepository orders;
    private final OrderVersionLineRepository segments;
    private final DeliveryRepository deliveries;
    private final DeliveryLineRepository deliveryLines;
    private final AcceptanceDocumentRepository acceptances;
    private final AcceptanceLineRepository acceptanceLines;
    private final SupplierReturnRepository returns;
    private final SupplierReturnLineRepository returnLines;
    private final FactCorrectionRepository corrections;
    private final FulfillmentLedger ledger;
    private final AuditTrail audit;

    public CorrectionService(
            ActorAccess access,
            PurchaseOrderRepository orders,
            OrderVersionLineRepository segments,
            DeliveryRepository deliveries,
            DeliveryLineRepository deliveryLines,
            AcceptanceDocumentRepository acceptances,
            AcceptanceLineRepository acceptanceLines,
            SupplierReturnRepository returns,
            SupplierReturnLineRepository returnLines,
            FactCorrectionRepository corrections,
            FulfillmentLedger ledger,
            AuditTrail audit) {

        this.access = access;
        this.orders = orders;
        this.segments = segments;
        this.deliveries = deliveries;
        this.deliveryLines = deliveryLines;
        this.acceptances = acceptances;
        this.acceptanceLines = acceptanceLines;
        this.returns = returns;
        this.returnLines = returnLines;
        this.corrections = corrections;
        this.ledger = ledger;
        this.audit = audit;
    }

    public record ReceiptFix(UUID approvedSegmentId, BigDecimal quantity) {}

    public record InspectionFix(
            UUID deliveryLineId,
            BigDecimal accepted,
            BigDecimal qualityRejected,
            BigDecimal otherRejected,
            String reason) {}

    @Transactional
    public FactCorrection proposeReceipt(
            UUID actorId,
            UUID originalId,
            LocalDate arrivedOn,
            String reason,
            List<ReceiptFix> corrected) {

        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);

        Delivery old =
                deliveries
                        .findById(originalId)
                        .orElseThrow(() -> new BusinessException("Поставка не найдена"));

        PurchaseOrder o = locked(old.getOrder().getId());

        Rules.require(old.getState() == FactState.CONFIRMED, "Исходная поставка не подтверждена");

        Rules.require(
                acceptances.findByDelivery_Id(originalId).stream()
                        .noneMatch(x -> x.getState() != FactState.CANCELLED),
                "Для корректировки поступления сначала необходимо разрешить связанные приёмки");

        Rules.require(
                deliveries.findByOrder_Id(o.getId()).stream()
                        .noneMatch(
                                x ->
                                        x.getSupersedes() != null
                                                && x.getSupersedes().getId().equals(originalId)
                                                && x.getState() != FactState.CANCELLED),
                "Для поступления уже существует исправление");

        Rules.notEmpty(corrected, "Строки исправленной поставки");
        Rules.present(arrivedOn, "Исправленная дата");

        FactCorrection correction = newCorrection(actor, reason, arrivedOn);

        Set<UUID> allowed =
                segments.findByOrderVersion_Id(o.getCurrentApprovedVersion().getId()).stream()
                        .map(OrderVersionLine::getId)
                        .collect(Collectors.toSet());

        Delivery replacement = new Delivery();
        replacement.setOrder(o);
        replacement.setSupersedes(old);
        replacement.setCorrection(correction);
        replacement.setArrivedOn(arrivedOn);
        replacement.setRegisteredAt(Instant.now());
        replacement.setRegisteredBy(actor);
        replacement.setState(FactState.DRAFT);
        replacement.setExternalNumber(old.getExternalNumber());

        replacement = deliveries.saveAndFlush(replacement);

        for (ReceiptFix f : corrected) {

            Rules.positive(f.quantity(), "Количество");

            Rules.require(allowed.contains(f.approvedSegmentId()), "Сегмент неактуален");

            DeliveryLine line = new DeliveryLine();
            line.setDelivery(replacement);
            line.setOrderVersionLine(segments.findById(f.approvedSegmentId()).orElseThrow());
            line.setQuantity(f.quantity());

            deliveryLines.save(line);
        }

        audit.record(
                actor,
                "DELIVERY_CORRECTION_REQUESTED",
                correction,
                "FactCorrection",
                correction.getId(),
                reason);

        return correction;
    }

    @Transactional
    public FactCorrection proposeInspection(
            UUID actorId,
            UUID originalId,
            LocalDate inspectedOn,
            String reason,
            List<InspectionFix> corrected) {

        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);

        AcceptanceDocument old =
                acceptances
                        .findById(originalId)
                        .orElseThrow(() -> new BusinessException("Приёмка не найдена"));

        PurchaseOrder o = locked(old.getDelivery().getOrder().getId());

        Rules.require(old.getState() == FactState.CONFIRMED, "Исходная приёмка не подтверждена");

        Rules.require(
                acceptances.findByDelivery_Id(old.getDelivery().getId()).stream()
                        .noneMatch(
                                x ->
                                        x.getSupersedes() != null
                                                && x.getSupersedes().getId().equals(originalId)
                                                && x.getState() != FactState.CANCELLED),
                "Для приёмки уже существует исправление");

        for (SupplierReturn r : returns.findByOrder_Id(o.getId())) {
            if (r.getState() == ReturnState.PENDING || r.getState() == ReturnState.CONFIRMED) {

                for (SupplierReturnLine l : returnLines.findBySupplierReturn_Id(r.getId())) {

                    Rules.require(
                            !l.getAcceptanceLine()
                                    .getAcceptanceDocument()
                                    .getId()
                                    .equals(originalId),
                            "Сначала разберите связанные возвраты");
                }
            }
        }

        Rules.notEmpty(corrected, "Строки исправленной приёмки");
        Rules.present(inspectedOn, "Исправленная дата");

        Rules.require(
                !inspectedOn.isBefore(old.getDelivery().getArrivedOn()),
                "Проверка раньше поступления");

        FactCorrection correction = newCorrection(actor, reason, inspectedOn);

        Set<UUID> allowed =
                deliveryLines.findByDelivery_Id(old.getDelivery().getId()).stream()
                        .map(DeliveryLine::getId)
                        .collect(Collectors.toSet());

        AcceptanceDocument replacement = new AcceptanceDocument();
        replacement.setDelivery(old.getDelivery());
        replacement.setSupersedes(old);
        replacement.setCorrection(correction);
        replacement.setInspectedOn(inspectedOn);
        replacement.setRegisteredBy(actor);
        replacement.setRegisteredAt(Instant.now());
        replacement.setState(FactState.DRAFT);

        replacement = acceptances.saveAndFlush(replacement);

        for (InspectionFix f : corrected) {

            Rules.require(allowed.contains(f.deliveryLineId()), "Позиция другого поступления");

            Rules.nonnegative(f.accepted(), "Принято");
            Rules.nonnegative(f.qualityRejected(), "Брак");
            Rules.nonnegative(f.otherRejected(), "Прочие отклонения");

            Rules.positive(
                    f.accepted().add(f.qualityRejected()).add(f.otherRejected()), "Количество");

            if (f.otherRejected().signum() > 0) {
                Rules.text(f.reason(), "Причина отклонения");
            }

            AcceptanceLine l = new AcceptanceLine();
            l.setAcceptanceDocument(replacement);
            l.setDeliveryLine(deliveryLines.findById(f.deliveryLineId()).orElseThrow());
            l.setAcceptedQty(f.accepted());
            l.setQualityRejectedQty(f.qualityRejected());
            l.setOtherRejectedQty(f.otherRejected());
            l.setOtherReason(f.reason());

            acceptanceLines.save(l);
        }

        audit.record(
                actor,
                "ACCEPTANCE_CORRECTION_REQUESTED",
                correction,
                "FactCorrection",
                correction.getId(),
                reason);

        return correction;
    }

    @Transactional
    public FactCorrection submit(UUID actorId, UUID correctionId) {

        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);

        FactCorrection c =
                corrections
                        .findById(correctionId)
                        .orElseThrow(() -> new BusinessException("Исправление не найдено"));

        Rules.require(c.getState() == VersionState.DRAFT, "Исправление уже отправлено");

        c.setState(VersionState.PENDING);

        audit.record(actor, "FACT_CORRECTION_SUBMITTED", c, "FactCorrection", c.getId(), null);

        return c;
    }

    @Transactional
    public FactCorrection decide(
            UUID managerId, UUID correctionId, boolean approve, String comment) {

        User manager = access.requireRole(managerId, RoleCode.PROCUREMENT_MANAGER);

        FactCorrection c =
                corrections
                        .findById(correctionId)
                        .orElseThrow(() -> new BusinessException("Исправление не найдено"));

        Rules.require(c.getState() == VersionState.PENDING, "Исправление не ожидает решения");

        ActorAccess.independent(c.getCreatedBy(), manager);

        List<Delivery> deliveryFixes = deliveries.findByCorrection_Id(correctionId);

        List<AcceptanceDocument> acceptanceFixes = acceptances.findByCorrection_Id(correctionId);

        Rules.require(
                deliveryFixes.size() + acceptanceFixes.size() == 1,
                "Исправление должно содержать один документ");

        UUID orderId =
                !deliveryFixes.isEmpty()
                        ? deliveryFixes.get(0).getOrder().getId()
                        : acceptanceFixes.get(0).getDelivery().getOrder().getId();

        PurchaseOrder o = locked(orderId);

        if (approve) {
            if (!deliveryFixes.isEmpty()) {
                validateDeliveryFix(deliveryFixes.get(0));
            } else {
                validateInspectionFix(o, acceptanceFixes.get(0));
            }
        } else {
            Rules.text(comment, "Причина отклонения");
        }

        c.setState(approve ? VersionState.APPROVED : VersionState.REJECTED);

        c.setDecidedBy(manager);
        c.setDecidedAt(Instant.now());
        c.setDecisionComment(comment);

        if (!deliveryFixes.isEmpty()) {
            deliveryFixes.get(0).setState(approve ? FactState.CONFIRMED : FactState.CANCELLED);
        } else {
            acceptanceFixes.get(0).setState(approve ? FactState.CONFIRMED : FactState.CANCELLED);
        }

        audit.record(
                manager,
                approve ? "FACT_CORRECTION_APPROVED" : "FACT_CORRECTION_REJECTED",
                c,
                "FactCorrection",
                c.getId(),
                comment);

        return c;
    }

    private void validateDeliveryFix(Delivery replacement) {

        Delivery old = Rules.present(replacement.getSupersedes(), "Исходный документ");

        Rules.require(
                old.getState() == FactState.CONFIRMED && replacement.getState() == FactState.DRAFT,
                "Недопустимое состояние исправления");

        Rules.require(
                acceptances.findByDelivery_Id(old.getId()).stream()
                        .noneMatch(x -> x.getState() != FactState.CANCELLED),
                "Для поступления существуют приёмки");

        Rules.notEmpty(deliveryLines.findByDelivery_Id(replacement.getId()), "Строки исправления");
    }

    private void validateInspectionFix(PurchaseOrder o, AcceptanceDocument replacement) {

        AcceptanceDocument old = Rules.present(replacement.getSupersedes(), "Исходный документ");

        Rules.require(
                old.getState() == FactState.CONFIRMED && replacement.getState() == FactState.DRAFT,
                "Недопустимое состояние исправления");

        FulfillmentLedger.Snapshot snap = ledger.snapshot(o.getId());

        Rules.require(
                snap.inspected().stream()
                        .anyMatch(x -> x.acceptanceDocumentId().equals(old.getId())),
                "Исходная приёмка уже неактуальна");

        List<AcceptanceLine> past = acceptanceLines.findByAcceptanceDocument_Id(old.getId());

        List<AcceptanceLine> revised =
                acceptanceLines.findByAcceptanceDocument_Id(replacement.getId());

        Rules.notEmpty(revised, "Строки исправления");

        Map<UUID, BigDecimal> changesByDelivery = new HashMap<>();
        Map<UUID, BigDecimal> changesByLine = new HashMap<>();

        for (AcceptanceLine x : past) {

            changesByDelivery.merge(
                    x.getDeliveryLine().getId(),
                    x.getAcceptedQty()
                            .add(x.getQualityRejectedQty())
                            .add(x.getOtherRejectedQty())
                            .negate(),
                    BigDecimal::add);

            changesByLine.merge(
                    x.getDeliveryLine().getOrderVersionLine().getOrderLine().getId(),
                    x.getAcceptedQty().negate(),
                    BigDecimal::add);
        }

        for (AcceptanceLine x : revised) {

            changesByDelivery.merge(
                    x.getDeliveryLine().getId(),
                    x.getAcceptedQty().add(x.getQualityRejectedQty()).add(x.getOtherRejectedQty()),
                    BigDecimal::add);

            changesByLine.merge(
                    x.getDeliveryLine().getOrderVersionLine().getOrderLine().getId(),
                    x.getAcceptedQty(),
                    BigDecimal::add);
        }

        for (var change : changesByDelivery.entrySet()) {

            Rules.require(
                    snap.inspected(change.getKey())
                                    .add(change.getValue())
                                    .compareTo(snap.delivered(change.getKey()))
                            <= 0,
                    "Исправленная приёмка превышает физическое поступление");
        }

        Map<String, BigDecimal> segmentChanges = new HashMap<>();

        for (AcceptanceLine row : past) {
            segmentChanges.merge(
                    row.getDeliveryLine().getOrderVersionLine().getSegmentKey(),
                    row.getAcceptedQty().negate(),
                    BigDecimal::add);
        }

        for (AcceptanceLine row : revised) {
            segmentChanges.merge(
                    row.getDeliveryLine().getOrderVersionLine().getSegmentKey(),
                    row.getAcceptedQty(),
                    BigDecimal::add);
        }

        List<OrderVersionLine> activeSegments =
                segments.findByOrderVersion_Id(o.getCurrentApprovedVersion().getId());

        for (var e : segmentChanges.entrySet()) {

            OrderVersionLine segment =
                    activeSegments.stream()
                            .filter(x -> x.getSegmentKey().equals(e.getKey()))
                            .findFirst()
                            .orElseThrow(() -> new BusinessException("Сегмент отсутствует"));

            Rules.require(
                    snap.netBySegment(e.getKey()).add(e.getValue()).compareTo(segment.getQuantity())
                            <= 0,
                    "Исправленная приёмка превышает количество сегмента");
        }

        for (var change : changesByLine.entrySet()) {

            BigDecimal ordered =
                    segments.findByOrderVersion_Id(o.getCurrentApprovedVersion().getId()).stream()
                            .filter(x -> x.getOrderLine().getId().equals(change.getKey()))
                            .map(OrderVersionLine::getQuantity)
                            .reduce(BigDecimal.ZERO, BigDecimal::add);

            Rules.require(
                    snap.fulfilled(change.getKey()).add(change.getValue()).compareTo(ordered) <= 0,
                    "Исправленная приёмка превышает обязательство");
        }
    }

    private FactCorrection newCorrection(User actor, String reason, LocalDate date) {

        FactCorrection c = new FactCorrection();
        c.setReason(Rules.text(reason, "Причина исправления"));
        c.setState(VersionState.DRAFT);
        c.setEffectiveOn(date);
        c.setCreatedBy(actor);
        c.setCreatedAt(Instant.now());

        return corrections.saveAndFlush(c);
    }

    private PurchaseOrder locked(UUID id) {
        return orders.findByIdForUpdate(id)
                .orElseThrow(() -> new BusinessException("Заказ не найден"));
    }
}
