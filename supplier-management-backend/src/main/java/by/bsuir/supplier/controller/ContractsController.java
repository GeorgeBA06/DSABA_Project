package by.bsuir.supplier.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.request.ContractCreateRequest;
import by.bsuir.supplier.dto.request.ContractRevisionRequest;
import by.bsuir.supplier.dto.request.ContractTermsRequest;
import by.bsuir.supplier.dto.request.DecisionRequest;
import by.bsuir.supplier.dto.request.ReasonRequest;
import by.bsuir.supplier.dto.response.ContractVersionView;
import by.bsuir.supplier.dto.response.ContractView;
import by.bsuir.supplier.mapper.ContractViewMapper;
import by.bsuir.supplier.service.contract.ContractService;
import by.bsuir.supplier.service.query.ContractQueries;

@RestController
@RequestMapping("/api/v1/contracts")
@PreAuthorize("hasAnyRole('PROCUREMENT_SPECIALIST','PROCUREMENT_MANAGER')")
public class ContractsController {
    private final ContractService service;
    private final ContractQueries read;
    public ContractsController(ContractService service, ContractQueries read) {
        this.service = service;
        this.read = read;
    }
    @GetMapping
    public PageResponse<ContractView> list(
        Authentication a,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return read.listContract(ApiActor.id(a), page, size);
    }
    @GetMapping("/{id}")
    public ContractView detail(Authentication a, @PathVariable UUID id) {
        return read.getContract(ApiActor.id(a), id);
    }
    @GetMapping("/{id}/versions")
    public List<ContractVersionView> versions(Authentication a, @PathVariable UUID id) {
        return read.versions(ApiActor.id(a), id);
    }
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ContractView create(Authentication a, @Valid @RequestBody ContractCreateRequest d) {
        return ContractViewMapper.toView(service.create(ApiActor.id(a), d.supplierId(), terms(d.terms())));
    }
    @PostMapping("/{id}/versions")
    @ResponseStatus(HttpStatus.CREATED)
    public ContractVersionView revise(Authentication a, @PathVariable UUID id, @Valid @RequestBody ContractRevisionRequest d) {
        return ContractViewMapper.toView(service.createRevision(ApiActor.id(a), id, terms(d.terms()), d.reason()));
    }
    @PostMapping("/versions/{id}/submit")
    public ContractVersionView submit(Authentication a, @PathVariable UUID id) {
        return ContractViewMapper.toView(service.submit(ApiActor.id(a), id));
    }
    @PostMapping("/versions/{id}/decision")
    public ContractVersionView decision(Authentication a, @PathVariable UUID id, @Valid @RequestBody DecisionRequest d) {
        return ContractViewMapper.toView(service.decide(ApiActor.id(a), id, d.approve(), d.comment()));
    }
    @PostMapping("/{id}/terminate")
    public ContractView terminate(Authentication a, @PathVariable UUID id, @Valid @RequestBody ReasonRequest d) {
        return ContractViewMapper.toView(service.terminate(ApiActor.id(a), id, d.reason()));
    }
    private static ContractService.Terms terms(ContractTermsRequest t) {
        return new ContractService.Terms(t.number(), t.signedOn(), t.validFrom(), t.validTo(), t.terms());
    }
}
