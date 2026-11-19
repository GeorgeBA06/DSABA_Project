package by.bsuir.supplier.controller;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.request.CorrectInspectionRequest;
import by.bsuir.supplier.dto.request.CorrectReceiptRequest;
import by.bsuir.supplier.dto.request.DecisionRequest;
import by.bsuir.supplier.dto.request.DeliveryRequest;
import by.bsuir.supplier.dto.request.InspectionRequest;
import by.bsuir.supplier.dto.request.ReturnRequest;
import by.bsuir.supplier.dto.response.AcceptanceDocumentView;
import by.bsuir.supplier.dto.response.AcceptanceLineView;
import by.bsuir.supplier.dto.response.DeliveryLineView;
import by.bsuir.supplier.dto.response.DeliveryView;
import by.bsuir.supplier.dto.response.FactCorrectionView;
import by.bsuir.supplier.dto.response.SupplierReturnLineView;
import by.bsuir.supplier.dto.response.SupplierReturnView;
import by.bsuir.supplier.mapper.FulfillmentViewMapper;
import by.bsuir.supplier.service.fulfillment.*;
import by.bsuir.supplier.service.query.FulfillmentQueries;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/fulfillment")
@PreAuthorize("hasAnyRole('PROCUREMENT_SPECIALIST','PROCUREMENT_MANAGER')")
public class FulfillmentController {
    private final ReceivingService receiving;
    private final ReturnService returns;
    private final CorrectionService corrections;
    private final FulfillmentQueries read;

    public FulfillmentController(
            ReceivingService receiving,
            ReturnService returns,
            CorrectionService corrections,
            FulfillmentQueries read) {
        this.receiving = receiving;
        this.returns = returns;
        this.corrections = corrections;
        this.read = read;
    }

    @GetMapping("/deliveries")
    public PageResponse<DeliveryView> deliveries(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listDelivery(ApiActor.id(a), page, size);
    }

    @GetMapping("/deliveries/{id}")
    public DeliveryView delivery(Authentication a, @PathVariable UUID id) {
        return read.getDelivery(ApiActor.id(a), id);
    }

    @GetMapping("/deliveries/{id}/lines")
    public List<DeliveryLineView> deliveryLines(Authentication a, @PathVariable UUID id) {
        return read.deliveryLines(ApiActor.id(a), id);
    }

    @GetMapping("/acceptances")
    public PageResponse<AcceptanceDocumentView> acceptances(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listAcceptanceDocument(ApiActor.id(a), page, size);
    }

    @GetMapping("/acceptances/{id}/lines")
    public List<AcceptanceLineView> acceptanceLines(Authentication a, @PathVariable UUID id) {
        return read.acceptanceLines(ApiActor.id(a), id);
    }

    @GetMapping("/acceptances/{id}")
    public AcceptanceDocumentView acceptance(Authentication a, @PathVariable UUID id) {
        return read.getAcceptanceDocument(ApiActor.id(a), id);
    }

    @GetMapping("/returns/{id}")
    public SupplierReturnView returnedDetail(Authentication a, @PathVariable UUID id) {
        return read.getSupplierReturn(ApiActor.id(a), id);
    }

    @GetMapping("/corrections/{id}")
    public FactCorrectionView correctionDetail(Authentication a, @PathVariable UUID id) {
        return read.getFactCorrection(ApiActor.id(a), id);
    }

    @GetMapping("/returns")
    public PageResponse<SupplierReturnView> returns(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listSupplierReturn(ApiActor.id(a), page, size);
    }

    @GetMapping("/returns/{id}/lines")
    public List<SupplierReturnLineView> returnLines(Authentication a, @PathVariable UUID id) {
        return read.returnLines(ApiActor.id(a), id);
    }

    @GetMapping("/corrections")
    public PageResponse<FactCorrectionView> corrections(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listFactCorrection(ApiActor.id(a), page, size);
    }

    @PostMapping("/orders/{orderId}/deliveries")
    @ResponseStatus(HttpStatus.CREATED)
    public DeliveryView received(
            Authentication a, @PathVariable UUID orderId, @Valid @RequestBody DeliveryRequest d) {
        return FulfillmentViewMapper.toView(
                receiving.registerDelivery(
                        ApiActor.id(a),
                        orderId,
                        d.arrivedOn(),
                        d.externalNumber(),
                        d.items().stream()
                                .map(
                                        x ->
                                                new ReceivingService.ReceivedItem(
                                                        x.segmentId(), x.quantity()))
                                .toList()));
    }

    @PostMapping("/deliveries/{id}/confirm")
    public DeliveryView confirmDelivery(Authentication a, @PathVariable UUID id) {
        return FulfillmentViewMapper.toView(receiving.confirmDelivery(ApiActor.id(a), id));
    }

    @PostMapping("/deliveries/{id}/acceptances")
    @ResponseStatus(HttpStatus.CREATED)
    public AcceptanceDocumentView inspection(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody InspectionRequest d) {
        return FulfillmentViewMapper.toView(
                receiving.registerInspection(
                        ApiActor.id(a),
                        id,
                        d.inspectedOn(),
                        d.items().stream()
                                .map(
                                        x ->
                                                new ReceivingService.InspectionItem(
                                                        x.deliveryLineId(),
                                                        x.accepted(),
                                                        x.qualityRejected(),
                                                        x.otherRejected(),
                                                        x.reason()))
                                .toList()));
    }

    @PostMapping("/acceptances/{id}/confirm")
    public AcceptanceDocumentView confirmInspection(Authentication a, @PathVariable UUID id) {
        return FulfillmentViewMapper.toView(receiving.confirmInspection(ApiActor.id(a), id));
    }

    @PostMapping("/orders/{id}/returns")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierReturnView returned(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody ReturnRequest d) {
        return FulfillmentViewMapper.toView(
                returns.create(
                        ApiActor.id(a),
                        id,
                        d.returnedOn(),
                        d.reason(),
                        d.items().stream()
                                .map(
                                        x ->
                                                new ReturnService.ReturnItem(
                                                        x.acceptanceLineId(),
                                                        x.quantity(),
                                                        x.defectDescription()))
                                .toList()));
    }

    @PostMapping("/returns/{id}/submit")
    public SupplierReturnView submitReturn(Authentication a, @PathVariable UUID id) {
        return FulfillmentViewMapper.toView(returns.submit(ApiActor.id(a), id));
    }

    @PostMapping("/returns/{id}/decision")
    public SupplierReturnView decideReturn(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody DecisionRequest d) {
        return FulfillmentViewMapper.toView(returns.decide(ApiActor.id(a), id, d.approve()));
    }

    @PostMapping("/deliveries/{id}/correction")
    @ResponseStatus(HttpStatus.CREATED)
    public FactCorrectionView receiptCorrection(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody CorrectReceiptRequest d) {
        return FulfillmentViewMapper.toView(
                corrections.proposeReceipt(
                        ApiActor.id(a),
                        id,
                        d.arrivedOn(),
                        d.reason(),
                        d.items().stream()
                                .map(
                                        x ->
                                                new CorrectionService.ReceiptFix(
                                                        x.approvedSegmentId(), x.quantity()))
                                .toList()));
    }

    @PostMapping("/acceptances/{id}/correction")
    @ResponseStatus(HttpStatus.CREATED)
    public FactCorrectionView inspectionCorrection(
            Authentication a,
            @PathVariable UUID id,
            @Valid @RequestBody CorrectInspectionRequest d) {
        return FulfillmentViewMapper.toView(
                corrections.proposeInspection(
                        ApiActor.id(a),
                        id,
                        d.inspectedOn(),
                        d.reason(),
                        d.items().stream()
                                .map(
                                        x ->
                                                new CorrectionService.InspectionFix(
                                                        x.deliveryLineId(),
                                                        x.accepted(),
                                                        x.qualityRejected(),
                                                        x.otherRejected(),
                                                        x.reason()))
                                .toList()));
    }

    @PostMapping("/corrections/{id}/submit")
    public FactCorrectionView submitCorrection(Authentication a, @PathVariable UUID id) {
        return FulfillmentViewMapper.toView(corrections.submit(ApiActor.id(a), id));
    }

    @PostMapping("/corrections/{id}/decision")
    public FactCorrectionView decideCorrection(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody DecisionRequest d) {
        return FulfillmentViewMapper.toView(
                corrections.decide(ApiActor.id(a), id, d.approve(), d.comment()));
    }
}
