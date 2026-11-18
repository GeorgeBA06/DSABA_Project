package by.bsuir.supplier.service.fulfillment;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.entity.enums.FactState;
import by.bsuir.supplier.entity.enums.ReturnState;
import by.bsuir.supplier.entity.fulfillment.AcceptanceDocument;
import by.bsuir.supplier.entity.fulfillment.AcceptanceLine;
import by.bsuir.supplier.entity.fulfillment.Delivery;
import by.bsuir.supplier.entity.fulfillment.DeliveryLine;
import by.bsuir.supplier.entity.fulfillment.SupplierReturn;
import by.bsuir.supplier.entity.fulfillment.SupplierReturnLine;
import by.bsuir.supplier.entity.order.OrderVersionLine;
import by.bsuir.supplier.repository.fulfillment.AcceptanceDocumentRepository;
import by.bsuir.supplier.repository.fulfillment.AcceptanceLineRepository;
import by.bsuir.supplier.repository.fulfillment.DeliveryLineRepository;
import by.bsuir.supplier.repository.fulfillment.DeliveryRepository;
import by.bsuir.supplier.repository.fulfillment.SupplierReturnLineRepository;
import by.bsuir.supplier.repository.fulfillment.SupplierReturnRepository;

/**
 * Builds a consistent, detached view of confirmed fulfillment facts for one order.
 *
 * All lazy JPA associations are read while the read-only transaction is active.
 * The returned snapshot contains only scalar values and is therefore safe to use
 * after the persistence context has been closed.
 */
@Service
public class FulfillmentLedger {

    private static final BigDecimal ZERO = BigDecimal.ZERO;

    private final DeliveryRepository deliveries;
    private final DeliveryLineRepository deliveredLines;
    private final AcceptanceDocumentRepository acceptances;
    private final AcceptanceLineRepository acceptedLines;
    private final SupplierReturnRepository returns;
    private final SupplierReturnLineRepository returnedLines;

    public FulfillmentLedger(
        DeliveryRepository deliveries,
        DeliveryLineRepository deliveredLines,
        AcceptanceDocumentRepository acceptances,
        AcceptanceLineRepository acceptedLines,
        SupplierReturnRepository returns,
        SupplierReturnLineRepository returnedLines) {

        this.deliveries = deliveries;
        this.deliveredLines = deliveredLines;
        this.acceptances = acceptances;
        this.acceptedLines = acceptedLines;
        this.returns = returns;
        this.returnedLines = returnedLines;
    }

    public record DeliveryFact(
        UUID id,
        UUID orderVersionLineId,
        UUID orderLineId,
        String segmentKey,
        BigDecimal quantity,
        LocalDate arrivedOn) {
    }

    public record AcceptanceFact(
        UUID id,
        UUID acceptanceDocumentId,
        UUID deliveryLineId,
        UUID orderVersionLineId,
        UUID orderLineId,
        String segmentKey,
        BigDecimal acceptedQty,
        BigDecimal qualityRejectedQty,
        BigDecimal otherRejectedQty,
        LocalDate inspectedOn) {

        public BigDecimal inspectedQuantity() {
            return acceptedQty
                .add(qualityRejectedQty)
                .add(otherRejectedQty);
        }
    }

    public record ReturnFact(
        UUID id,
        UUID acceptanceLineId,
        UUID deliveryLineId,
        UUID orderVersionLineId,
        UUID orderLineId,
        String segmentKey,
        BigDecimal quantity,
        LocalDate returnedOn) {
    }

    public record Snapshot(
        List<DeliveryFact> delivered,
        List<AcceptanceFact> inspected,
        List<ReturnFact> returned) {

        public Snapshot {
            delivered = List.copyOf(delivered);
            inspected = List.copyOf(inspected);
            returned = List.copyOf(returned);
        }

        public BigDecimal delivered(UUID deliveryLineId) {
            return delivered.stream()
                .filter(line -> line.id().equals(deliveryLineId))
                .map(DeliveryFact::quantity)
                .reduce(ZERO, BigDecimal::add);
        }

        public BigDecimal inspected(UUID deliveryLineId) {
            return inspected.stream()
                .filter(line ->
                    line.deliveryLineId().equals(deliveryLineId))
                .map(AcceptanceFact::inspectedQuantity)
                .reduce(ZERO, BigDecimal::add);
        }

        public BigDecimal accepted(UUID acceptanceLineId) {
            return inspected.stream()
                .filter(line -> line.id().equals(acceptanceLineId))
                .map(AcceptanceFact::acceptedQty)
                .reduce(ZERO, BigDecimal::add);
        }

        public BigDecimal returned(UUID acceptanceLineId) {
            return returned.stream()
                .filter(line ->
                    line.acceptanceLineId().equals(acceptanceLineId))
                .map(ReturnFact::quantity)
                .reduce(ZERO, BigDecimal::add);
        }

        public BigDecimal fulfilled(UUID orderLineId) {

            BigDecimal acceptedQuantity = inspected.stream()
                .filter(line ->
                    line.orderLineId().equals(orderLineId))
                .map(AcceptanceFact::acceptedQty)
                .reduce(ZERO, BigDecimal::add);

            BigDecimal returnedQuantity = returned.stream()
                .filter(line ->
                    line.orderLineId().equals(orderLineId))
                .map(ReturnFact::quantity)
                .reduce(ZERO, BigDecimal::add);

            return acceptedQuantity.subtract(returnedQuantity);
        }

        public BigDecimal netBySegment(String segmentKey) {

            BigDecimal acceptedQuantity = inspected.stream()
                .filter(line ->
                    line.segmentKey().equals(segmentKey))
                .map(AcceptanceFact::acceptedQty)
                .reduce(ZERO, BigDecimal::add);

            BigDecimal returnedQuantity = returned.stream()
                .filter(line ->
                    line.segmentKey().equals(segmentKey))
                .map(ReturnFact::quantity)
                .reduce(ZERO, BigDecimal::add);

            return acceptedQuantity.subtract(returnedQuantity);
        }

        public BigDecimal deliveredForSegment(
            String segmentKey,
            LocalDate byDate) {

            return delivered.stream()
                .filter(line ->
                    line.segmentKey().equals(segmentKey))
                .filter(line ->
                    byDate == null || !line.arrivedOn().isAfter(byDate))
                .map(DeliveryFact::quantity)
                .reduce(ZERO, BigDecimal::add);
        }

        public boolean hasUninspected() {
            return delivered.stream()
                .anyMatch(line ->
                    inspected(line.id()).compareTo(line.quantity()) < 0);
        }
    }

    @Transactional(readOnly = true)
    public Snapshot snapshot(UUID orderId) {

        List<Delivery> deliveryDocuments =
            deliveries.findByOrder_Id(orderId);

        Set<UUID> supersededDeliveryIds = deliveryDocuments.stream()
            .filter(delivery ->
                delivery.getState() == FactState.CONFIRMED
                    && delivery.getSupersedes() != null)
            .map(delivery -> delivery.getSupersedes().getId())
            .collect(Collectors.toSet());

        List<DeliveryLine> activeDeliveryLines = new ArrayList<>();
        List<AcceptanceDocument> acceptanceDocuments = new ArrayList<>();

        for (Delivery delivery : deliveryDocuments) {

            if (delivery.getState() != FactState.CONFIRMED
                || supersededDeliveryIds.contains(delivery.getId())) {
                continue;
            }

            activeDeliveryLines.addAll(
                deliveredLines.findByDelivery_Id(delivery.getId())
            );

            acceptanceDocuments.addAll(
                acceptances.findByDelivery_Id(delivery.getId())
            );
        }

        Set<UUID> supersededAcceptanceIds = acceptanceDocuments.stream()
            .filter(document ->
                document.getState() == FactState.CONFIRMED
                    && document.getSupersedes() != null)
            .map(document -> document.getSupersedes().getId())
            .collect(Collectors.toSet());

        List<AcceptanceLine> activeAcceptanceLines = new ArrayList<>();

        for (AcceptanceDocument document : acceptanceDocuments) {

            if (document.getState() == FactState.CONFIRMED
                && !supersededAcceptanceIds.contains(document.getId())) {

                activeAcceptanceLines.addAll(
                    acceptedLines.findByAcceptanceDocument_Id(document.getId())
                );
            }
        }

        List<SupplierReturn> returnDocuments =
            returns.findByOrder_Id(orderId);

        Set<UUID> supersededReturnIds = returnDocuments.stream()
            .filter(document ->
                document.getState() == ReturnState.CONFIRMED
                    && document.getSupersedes() != null)
            .map(document -> document.getSupersedes().getId())
            .collect(Collectors.toSet());

        List<SupplierReturnLine> activeReturnLines = new ArrayList<>();

        for (SupplierReturn document : returnDocuments) {

            if (document.getState() == ReturnState.CONFIRMED
                && !supersededReturnIds.contains(document.getId())) {

                activeReturnLines.addAll(
                    returnedLines.findBySupplierReturn_Id(document.getId())
                );
            }
        }

        // Map every required association to scalar values before
        // the transaction ends.
        List<DeliveryFact> deliveryFacts = activeDeliveryLines.stream()
            .map(FulfillmentLedger::toFact)
            .toList();

        List<AcceptanceFact> acceptanceFacts = activeAcceptanceLines.stream()
            .map(FulfillmentLedger::toFact)
            .toList();

        List<ReturnFact> returnFacts = activeReturnLines.stream()
            .map(FulfillmentLedger::toFact)
            .toList();

        return new Snapshot(
            deliveryFacts,
            acceptanceFacts,
            returnFacts
        );
    }

    private static DeliveryFact toFact(DeliveryLine line) {

        OrderVersionLine versionLine =
            line.getOrderVersionLine();

        return new DeliveryFact(
            line.getId(),
            versionLine.getId(),
            versionLine.getOrderLine().getId(),
            versionLine.getSegmentKey(),
            line.getQuantity(),
            line.getDelivery().getArrivedOn()
        );
    }

    private static AcceptanceFact toFact(AcceptanceLine line) {

        DeliveryLine deliveryLine =
            line.getDeliveryLine();

        OrderVersionLine versionLine =
            deliveryLine.getOrderVersionLine();

        return new AcceptanceFact(
            line.getId(),
            line.getAcceptanceDocument().getId(),
            deliveryLine.getId(),
            versionLine.getId(),
            versionLine.getOrderLine().getId(),
            versionLine.getSegmentKey(),
            line.getAcceptedQty(),
            line.getQualityRejectedQty(),
            line.getOtherRejectedQty(),
            line.getAcceptanceDocument().getInspectedOn()
        );
    }

    private static ReturnFact toFact(SupplierReturnLine line) {

        AcceptanceLine acceptanceLine =
            line.getAcceptanceLine();

        DeliveryLine deliveryLine =
            acceptanceLine.getDeliveryLine();

        OrderVersionLine versionLine =
            deliveryLine.getOrderVersionLine();

        return new ReturnFact(
            line.getId(),
            acceptanceLine.getId(),
            deliveryLine.getId(),
            versionLine.getId(),
            versionLine.getOrderLine().getId(),
            versionLine.getSegmentKey(),
            line.getQuantity(),
            line.getSupplierReturn().getReturnedOn()
        );
    }
}
