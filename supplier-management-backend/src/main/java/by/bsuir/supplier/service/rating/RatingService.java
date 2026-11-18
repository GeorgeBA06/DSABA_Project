package by.bsuir.supplier.service.rating;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.catalog.ProductCategory;
import by.bsuir.supplier.entity.enums.RatingScope;
import by.bsuir.supplier.entity.enums.RatingState;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.SelectionReason;
import by.bsuir.supplier.entity.enums.VersionState;
import by.bsuir.supplier.entity.order.OrderVersionLine;
import by.bsuir.supplier.entity.order.PurchaseOrder;
import by.bsuir.supplier.entity.rating.ExpertAssessment;
import by.bsuir.supplier.entity.rating.RatingLineInput;
import by.bsuir.supplier.entity.rating.SupplierRating;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.repository.catalog.ProductCategoryRepository;
import by.bsuir.supplier.repository.order.*;
import by.bsuir.supplier.repository.rating.*;
import by.bsuir.supplier.repository.supplier.SupplierRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;
import by.bsuir.supplier.service.fulfillment.FulfillmentLedger;

/**
 * Daily 90-day snapshot.
 * Old published ratings are preserved; recalculations append a new row.
 *
 * Only current-day as-of supported: the reduced model does not reconstruct
 * arbitrary past source states.
 */
@Service
public class RatingService {

    private static final ZoneId ZONE =
        ZoneId.of("Europe/Minsk");

    private final ActorAccess access;
    private final SupplierRepository suppliers;
    private final ProductCategoryRepository categories;
    private final PurchaseOrderRepository orders;
    private final OrderVersionLineRepository segments;
    private final FulfillmentLedger ledger;
    private final ExpertAssessmentRepository experts;
    private final SupplierRatingRepository ratings;
    private final RatingLineInputRepository inputs;
    private final AuditTrail audit;

    public RatingService(
        ActorAccess access,
        SupplierRepository suppliers,
        ProductCategoryRepository categories,
        PurchaseOrderRepository orders,
        OrderVersionLineRepository segments,
        FulfillmentLedger ledger,
        ExpertAssessmentRepository experts,
        SupplierRatingRepository ratings,
        RatingLineInputRepository inputs,
        AuditTrail audit) {

        this.access = access;
        this.suppliers = suppliers;
        this.categories = categories;
        this.orders = orders;
        this.segments = segments;
        this.ledger = ledger;
        this.experts = experts;
        this.ratings = ratings;
        this.inputs = inputs;
        this.audit = audit;
    }

    private record Input(
        OrderVersionLine segment,
        SelectionReason reason,
        BigDecimal onTime,
        BigDecimal fulfilled,
        BigDecimal inspected,
        BigDecimal good,
        BigDecimal timeScore,
        BigDecimal completeScore,
        BigDecimal qualityScore,
        boolean pendingInspection) {
    }

    @Transactional(isolation = Isolation.REPEATABLE_READ)
    public SupplierRating calculate(
        UUID actorId,
        UUID supplierId,
        RatingScope scope,
        UUID categoryId) {

        User actor = access.requireRole(
            actorId,
            RoleCode.PROCUREMENT_MANAGER
        );

        Supplier supplier = suppliers.findByIdForUpdate(supplierId)
            .orElseThrow(() ->
                new BusinessException("Поставщик не найден"));

        Rules.present(scope, "Охват рейтинга");

        ProductCategory category = null;

        if (scope == RatingScope.CATEGORY) {

            category = categories.findById(
                Rules.present(categoryId, "Категория рейтинга")
            ).orElseThrow(() ->
                new BusinessException("Категория не найдена"));

        } else {

            Rules.require(
                categoryId == null,
                "Общий рейтинг не имеет категории"
            );
        }

        Instant now = Instant.now();

        LocalDate end = LocalDate.now(ZONE);
        LocalDate start = end.minusDays(89);

        List<Input> selected = new ArrayList<>();
        Set<UUID> includedOrders = new HashSet<>();

        for (PurchaseOrder order : orders.findBySupplier_Id(supplierId)) {

            if (order.getCurrentApprovedVersion() == null) {
                continue;
            }

            List<OrderVersionLine> active =
                segments.findByOrderVersion_Id(
                    order.getCurrentApprovedVersion().getId()
                );

            FulfillmentLedger.Snapshot snapshot =
                ledger.snapshot(order.getId());

            int before = selected.size();

            for (OrderVersionLine line : active) {

                if (scope == RatingScope.CATEGORY
                    && !category.getCode().equals(
                    line.getCategoryCodeSnapshot())) {
                    continue;
                }

                BigDecimal atStart = fulfilledAtStart(
                    snapshot,
                    line.getSegmentKey(),
                    start
                );

                boolean dueInPeriod =
                    !line.getRatingDueOn().isBefore(start)
                        && !line.getRatingDueOn().isAfter(end);

                boolean carried =
                    !line.getRatingDueOn().isAfter(start.minusDays(1))
                        && atStart.compareTo(line.getQuantity()) < 0;

                if (!dueInPeriod && !carried) {
                    continue;
                }

                BigDecimal ordered = line.getQuantity();
                String key = line.getSegmentKey();

                BigDecimal timely = snapshot
                    .deliveredForSegment(key, line.getRatingDueOn())
                    .min(ordered);

                BigDecimal net = snapshot
                    .netBySegment(key)
                    .max(BigDecimal.ZERO)
                    .min(ordered);

                BigDecimal inspected = BigDecimal.ZERO;
                BigDecimal good = BigDecimal.ZERO;
                BigDecimal otherRejected = BigDecimal.ZERO;

                for (FulfillmentLedger.AcceptanceFact accepted :
                    snapshot.inspected()) {

                    if (!key.equals(accepted.segmentKey())) {
                        continue;
                    }

                    inspected = inspected
                        .add(accepted.acceptedQty())
                        .add(accepted.qualityRejectedQty());

                    good = good.add(accepted.acceptedQty());

                    otherRejected = otherRejected
                        .add(accepted.otherRejectedQty());
                }

                for (FulfillmentLedger.ReturnFact returned :
                    snapshot.returned()) {

                    if (key.equals(returned.segmentKey())) {
                        good = good.subtract(returned.quantity());
                    }
                }

                good = good
                    .max(BigDecimal.ZERO)
                    .min(inspected);

                boolean pending =
                    snapshot.deliveredForSegment(key, null)
                        .compareTo(inspected.add(otherRejected)) > 0;

                CriterionStrategies.Facts facts =
                    new CriterionStrategies.Facts(
                        ordered,
                        timely,
                        net,
                        inspected,
                        good,
                        null
                    );

                selected.add(new Input(
                    line,
                    dueInPeriod
                        ? SelectionReason.DUE_IN_PERIOD
                        : SelectionReason.OPEN_AT_PERIOD_START,
                    timely,
                    net,
                    inspected,
                    good,
                    CriterionStrategies.evaluate(
                        RatingPolicy.Criterion.TIMELINESS,
                        facts
                    ),
                    CriterionStrategies.evaluate(
                        RatingPolicy.Criterion.COMPLETENESS,
                        facts
                    ),
                    CriterionStrategies.evaluate(
                        RatingPolicy.Criterion.QUALITY,
                        facts
                    ),
                    pending
                ));
            }

            if (selected.size() > before) {
                includedOrders.add(order.getId());
            }
        }

        ExpertAssessment expert = experts.findBySupplier_Id(supplierId)
            .stream()
            .filter(e ->
                e.getState() == VersionState.APPROVED
                    && !e.getApplicableFrom().isAfter(end)
                    && !e.getApplicableTo().isBefore(end))
            .max(Comparator.comparing(
                ExpertAssessment::getApprovedAt
            ))
            .orElse(null);

        BigDecimal timeliness = selected.isEmpty()
            ? null
            : RatingPolicy.mean(
            selected.stream()
                .map(Input::timeScore)
                .toList()
        );

        BigDecimal completeness = selected.isEmpty()
            ? null
            : RatingPolicy.mean(
            selected.stream()
                .map(Input::completeScore)
                .toList()
        );

        List<BigDecimal> availableQuality = selected.stream()
            .map(Input::qualityScore)
            .filter(Objects::nonNull)
            .toList();

        BigDecimal quality = RatingPolicy.mean(availableQuality);

        BigDecimal interaction = CriterionStrategies.evaluate(
            RatingPolicy.Criterion.INTERACTION,
            new CriterionStrategies.Facts(
                null,
                null,
                null,
                null,
                null,
                expert == null ? null : expert.getScore()
            )
        );

        boolean missingQuality = selected.stream()
            .anyMatch(i -> i.qualityScore() == null);

        boolean unfinished = selected.stream()
            .anyMatch(Input::pendingInspection);

        Map<String, Object> reasons = new LinkedHashMap<>();

        if (includedOrders.size() < 3) {
            reasons.put(
                "minimumOrders",
                "Необходимо минимум 3 согласованных заказа в периоде"
            );
        }

        if (missingQuality) {
            reasons.put(
                "qualityMissing",
                "Не все строки имеют проверенное количество для качества"
            );
        }

        if (unfinished) {
            reasons.put(
                "pendingInspection",
                "Есть непроверенные поступившие товары"
            );
        }

        if (expert == null) {
            reasons.put(
                "expertMissing",
                "Нет действующей утверждённой экспертной оценки"
            );
        }

        RatingState state = selected.isEmpty()
            ? RatingState.INSUFFICIENT_DATA
            : reasons.isEmpty()
            ? RatingState.PUBLISHED
            : RatingState.PRELIMINARY;

        BigDecimal total = state == RatingState.PUBLISHED
            ? RatingPolicy.weighted(Map.of(
            RatingPolicy.Criterion.TIMELINESS, timeliness,
            RatingPolicy.Criterion.COMPLETENESS, completeness,
            RatingPolicy.Criterion.QUALITY, quality,
            RatingPolicy.Criterion.INTERACTION, interaction
        ))
            : null;

        // The method deliberately publishes only when all four criteria are known.
        final UUID categoryKey =
            category == null ? null : category.getId();

        SupplierRating previous = ratings.findBySupplier_Id(supplierId)
            .stream()
            .filter(r ->
                r.getScope() == scope
                    && Objects.equals(
                    r.getCategory() == null
                        ? null
                        : r.getCategory().getId(),
                    categoryKey
                ))
            .max(Comparator.comparing(
                SupplierRating::getAsOfAt
            ))
            .orElse(null);

        SupplierRating result = new SupplierRating();

        result.setSupplier(supplier);
        result.setScope(scope);
        result.setCategory(category);
        result.setPeriodStart(start);
        result.setPeriodEnd(end);
        result.setAsOfAt(now);
        result.setMethodVersion("WEIGHTED_V1");
        result.setRulesSnapshot(RatingPolicy.rules());
        result.setExpertAssessment(expert);
        result.setSupersedes(previous);
        result.setTimelinessScore(timeliness);
        result.setCompletenessScore(completeness);
        result.setQualityScore(quality);
        result.setInteractionScore(interaction);
        result.setTotalScore(total);
        result.setBand(RatingPolicy.band(total));
        result.setState(state);
        result.setMissingReasons(
            reasons.isEmpty() ? null : reasons
        );
        result.setOrderCount(includedOrders.size());
        result.setLineCount(selected.size());
        result.setCreatedAt(now);

        result = ratings.saveAndFlush(result);

        // Immutable after insert: do not update this row.
        for (Input item : selected) {

            RatingLineInput detail = new RatingLineInput();

            detail.setRating(result);
            detail.setOrderVersionLine(item.segment());
            detail.setSelectionReason(item.reason());
            detail.setOrderedQty(item.segment().getQuantity());
            detail.setDeliveredOnTimeQty(item.onTime());
            detail.setFulfilledQty(item.fulfilled());
            detail.setQualityCheckedQty(item.inspected());
            detail.setQualityGoodQty(item.good());
            detail.setTimelinessScore(item.timeScore());
            detail.setCompletenessScore(item.completeScore());
            detail.setQualityScore(item.qualityScore());
            detail.setSourceCutoffAt(now);

            inputs.save(detail);
        }

        audit.record(
            actor,
            "RATING_CALCULATED",
            result,
            "SupplierRating",
            result.getId(),
            state.name()
        );

        return result;
    }

    private static BigDecimal fulfilledAtStart(
        FulfillmentLedger.Snapshot snapshot,
        String segmentKey,
        LocalDate start) {

        BigDecimal accepted = BigDecimal.ZERO;
        BigDecimal returned = BigDecimal.ZERO;

        for (FulfillmentLedger.AcceptanceFact line :
            snapshot.inspected()) {

            if (line.inspectedOn().isBefore(start)
                && segmentKey.equals(line.segmentKey())) {

                accepted = accepted.add(line.acceptedQty());
            }
        }

        for (FulfillmentLedger.ReturnFact line :
            snapshot.returned()) {

            if (line.returnedOn().isBefore(start)
                && segmentKey.equals(line.segmentKey())) {

                returned = returned.add(line.quantity());
            }
        }

        return accepted.subtract(returned).max(BigDecimal.ZERO);
    }
}
