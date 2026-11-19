package by.bsuir.supplier.controller;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.request.DecisionRequest;
import by.bsuir.supplier.dto.request.IncreaseRequest;
import by.bsuir.supplier.dto.request.OrderCreateRequest;
import by.bsuir.supplier.dto.request.OrderItemRequest;
import by.bsuir.supplier.dto.request.OrderItemsRequest;
import by.bsuir.supplier.dto.request.OrderRevisionRequest;
import by.bsuir.supplier.dto.request.ReasonRequest;
import by.bsuir.supplier.dto.request.RescheduleRequest;
import by.bsuir.supplier.dto.response.OrderClosureView;
import by.bsuir.supplier.dto.response.OrderVersionLineView;
import by.bsuir.supplier.dto.response.OrderVersionView;
import by.bsuir.supplier.dto.response.PurchaseOrderView;
import by.bsuir.supplier.mapper.OrderViewMapper;
import by.bsuir.supplier.service.order.*;
import by.bsuir.supplier.service.query.OrderQueries;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/orders")
@PreAuthorize("hasAnyRole('PROCUREMENT_SPECIALIST','PROCUREMENT_MANAGER')")
public class OrdersController {
    private final OrderService service;
    private final OrderClosureService closing;
    private final OrderQueries read;

    public OrdersController(OrderService service, OrderClosureService closing, OrderQueries read) {
        this.service = service;
        this.closing = closing;
        this.read = read;
    }

    @GetMapping
    public PageResponse<PurchaseOrderView> list(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listPurchaseOrder(ApiActor.id(a), page, size);
    }

    @GetMapping("/{id}")
    public PurchaseOrderView detail(Authentication a, @PathVariable UUID id) {
        return read.getPurchaseOrder(ApiActor.id(a), id);
    }

    @GetMapping("/{id}/versions")
    public List<OrderVersionView> versions(Authentication a, @PathVariable UUID id) {
        return read.orderVersions(ApiActor.id(a), id);
    }

    @GetMapping("/versions/{id}/lines")
    public List<OrderVersionLineView> lines(Authentication a, @PathVariable UUID id) {
        return read.versionLines(ApiActor.id(a), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PurchaseOrderView create(Authentication a, @Valid @RequestBody OrderCreateRequest d) {
        return OrderViewMapper.toView(
                service.create(
                        ApiActor.id(a),
                        d.number(),
                        d.supplierId(),
                        d.contractId(),
                        items(d.items())));
    }

    @PutMapping("/{id}/draft")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void replace(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody OrderItemsRequest d) {
        service.replaceInitialDraft(ApiActor.id(a), id, items(d.items()));
    }

    @PostMapping("/versions/{id}/submit")
    public OrderVersionView submit(Authentication a, @PathVariable UUID id) {
        return OrderViewMapper.toView(service.submit(ApiActor.id(a), id));
    }

    @PostMapping("/versions/{id}/decision")
    public OrderVersionView decide(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody DecisionRequest d) {
        return OrderViewMapper.toView(service.decide(ApiActor.id(a), id, d.approve(), d.comment()));
    }

    @PostMapping("/{id}/revise-rejected")
    public OrderVersionView rejected(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody OrderRevisionRequest d) {
        return OrderViewMapper.toView(
                service.reviseRejected(ApiActor.id(a), id, items(d.items()), d.reason()));
    }

    @PostMapping("/{id}/increase")
    public OrderVersionView increase(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody IncreaseRequest d) {
        return OrderViewMapper.toView(
                service.proposeIncrease(
                        ApiActor.id(a),
                        id,
                        d.orderLineId(),
                        d.additional(),
                        d.unitPrice(),
                        d.dueOn(),
                        d.reason()));
    }

    @PostMapping("/{id}/reschedule")
    public OrderVersionView reschedule(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody RescheduleRequest d) {
        return OrderViewMapper.toView(
                service.proposeSupplierReschedule(
                        ApiActor.id(a), id, d.segmentKey(), d.newDueOn(), d.reason()));
    }

    @PostMapping("/{id}/close")
    public OrderClosureView close(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody ReasonRequest d) {
        return OrderViewMapper.toView(closing.close(ApiActor.id(a), id, d.reason()));
    }

    @PostMapping("/{id}/reopen")
    public OrderClosureView reopen(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody ReasonRequest d) {
        return OrderViewMapper.toView(closing.reopen(ApiActor.id(a), id, d.reason()));
    }

    private static List<OrderService.Item> items(List<OrderItemRequest> xs) {
        return xs.stream()
                .map(
                        x ->
                                new OrderService.Item(
                                        x.productId(), x.quantity(), x.unitPrice(), x.dueOn()))
                .toList();
    }
}
