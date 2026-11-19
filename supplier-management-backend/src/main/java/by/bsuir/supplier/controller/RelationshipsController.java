package by.bsuir.supplier.controller;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.request.ClaimRequest;
import by.bsuir.supplier.dto.request.InteractionRequest;
import by.bsuir.supplier.dto.request.ResolveClaimRequest;
import by.bsuir.supplier.dto.request.SendClaimRequest;
import by.bsuir.supplier.dto.response.ClaimView;
import by.bsuir.supplier.dto.response.InteractionView;
import by.bsuir.supplier.mapper.ClaimViewMapper;
import by.bsuir.supplier.service.claim.RelationshipService;
import by.bsuir.supplier.service.query.ClaimQueries;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/relationships")
@PreAuthorize("hasAnyRole('PROCUREMENT_SPECIALIST','PROCUREMENT_MANAGER')")
public class RelationshipsController {
    private final RelationshipService service;
    private final ClaimQueries read;

    public RelationshipsController(RelationshipService service, ClaimQueries read) {
        this.service = service;
        this.read = read;
    }

    @GetMapping("/claims")
    public PageResponse<ClaimView> claims(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listClaim(ApiActor.id(a), page, size);
    }

    @GetMapping("/claims/{id}")
    public ClaimView claim(Authentication a, @PathVariable UUID id) {
        return read.getClaim(ApiActor.id(a), id);
    }

    @GetMapping("/interactions")
    public PageResponse<InteractionView> interactions(
            Authentication a,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "25") int size) {
        return read.listInteraction(ApiActor.id(a), page, size);
    }

    @PostMapping("/claims")
    @ResponseStatus(HttpStatus.CREATED)
    public ClaimView createClaim(Authentication a, @Valid @RequestBody ClaimRequest d) {
        return ClaimViewMapper.toView(
                service.createClaim(
                        ApiActor.id(a),
                        d.number(),
                        d.supplierId(),
                        d.orderLineId(),
                        d.deliveryLineId(),
                        d.type(),
                        d.description(),
                        d.requirements(),
                        d.responseDueOn()));
    }

    @PostMapping("/claims/{id}/send")
    public ClaimView send(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody SendClaimRequest d) {
        return ClaimViewMapper.toView(service.sendClaim(ApiActor.id(a), id, d.channel()));
    }

    @PostMapping("/claims/{id}/begin-resolution")
    public ClaimView begin(Authentication a, @PathVariable UUID id) {
        return ClaimViewMapper.toView(service.beginResolution(ApiActor.id(a), id));
    }

    @PostMapping("/claims/{id}/resolve")
    public ClaimView resolve(
            Authentication a, @PathVariable UUID id, @Valid @RequestBody ResolveClaimRequest d) {
        return ClaimViewMapper.toView(
                service.resolve(ApiActor.id(a), id, d.result(), d.remedy(), d.description()));
    }

    @PostMapping("/interactions")
    @ResponseStatus(HttpStatus.CREATED)
    public InteractionView interaction(Authentication a, @Valid @RequestBody InteractionRequest d) {
        return ClaimViewMapper.toView(
                service.logInteraction(
                        ApiActor.id(a),
                        d.supplierId(),
                        d.claimId(),
                        d.channel(),
                        d.occurredAt(),
                        d.subject(),
                        d.content(),
                        d.outcome()));
    }
}
