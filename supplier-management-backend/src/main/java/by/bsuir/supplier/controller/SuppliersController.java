package by.bsuir.supplier.controller;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.request.DecisionRequest;
import by.bsuir.supplier.dto.request.OfferRequest;
import by.bsuir.supplier.dto.request.SupplierRequest;
import by.bsuir.supplier.dto.request.SupplierStatusChangeRequest;
import by.bsuir.supplier.dto.response.OfferView;
import by.bsuir.supplier.dto.response.SupplierOfferView;
import by.bsuir.supplier.dto.response.SupplierStatusRequestView;
import by.bsuir.supplier.dto.response.SupplierView;
import by.bsuir.supplier.entity.supplier.SupplierProduct;
import by.bsuir.supplier.mapper.SupplierViewMapper;
import by.bsuir.supplier.service.query.SupplierQueries;
import by.bsuir.supplier.service.supplier.SupplierService;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/suppliers")
@PreAuthorize("hasAnyRole('PROCUREMENT_SPECIALIST','PROCUREMENT_MANAGER')")
public class SuppliersController {
    private final SupplierService service;
    private final SupplierQueries read;

    public SuppliersController(SupplierService service, SupplierQueries read) {
        this.service = service;
        this.read = read;
    }

    @GetMapping
    public PageResponse<SupplierView> list(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size,
            @RequestParam(defaultValue = "") String search) {
        return search.isBlank()
                ? read.listSupplier(ApiActor.id(a), page, size)
                : read.search(ApiActor.id(a), search.strip(), page, size);
    }

    @GetMapping("/{id}")
    public SupplierView detail(Authentication a, @PathVariable UUID id) {
        return read.getSupplier(ApiActor.id(a), id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierView create(Authentication a, @Valid @RequestBody SupplierRequest d) {
        return SupplierViewMapper.toView(service.create(ApiActor.id(a), details(d)));
    }

    @PutMapping("/{id}")
    public SupplierView update(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody SupplierRequest d) {
        return SupplierViewMapper.toView(service.update(ApiActor.id(a), id, details(d)));
    }

    @GetMapping("/{id}/products")
    public List<SupplierOfferView> offers(Authentication a, @PathVariable UUID id) {
        return read.offers(ApiActor.id(a), id);
    }

    @PostMapping("/{id}/products")
    public OfferView offer(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody OfferRequest d) {
        SupplierProduct p =
                service.offerProduct(ApiActor.id(a), id, d.productId(), d.supplierItemCode());
        return new OfferView(
                p.getSupplier().getId(),
                p.getProduct().getId(),
                p.getSupplierItemCode(),
                p.getActive());
    }

    @PostMapping("/{id}/status-requests")
    @ResponseStatus(HttpStatus.CREATED)
    public SupplierStatusRequestView request(
            Authentication a,
            @PathVariable UUID id,
            @Valid @RequestBody SupplierStatusChangeRequest d) {
        return SupplierViewMapper.toView(
                service.requestStatus(ApiActor.id(a), id, d.status(), d.reason()));
    }

    @GetMapping("/status-requests")
    public PageResponse<SupplierStatusRequestView> statusRequests(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listSupplierStatusRequest(ApiActor.id(a), page, size);
    }

    @PostMapping("/status-requests/{id}/decision")
    public SupplierStatusRequestView decide(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody DecisionRequest d) {
        return SupplierViewMapper.toView(
                service.decideStatus(ApiActor.id(a), id, d.approve(), d.comment()));
    }

    private static SupplierService.Details details(SupplierRequest d) {
        return new SupplierService.Details(
                d.name(),
                d.contactName(),
                d.email(),
                d.phone(),
                d.address(),
                d.registrationNumber());
    }
}
