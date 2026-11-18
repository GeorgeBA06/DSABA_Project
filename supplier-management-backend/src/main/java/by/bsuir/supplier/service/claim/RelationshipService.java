package by.bsuir.supplier.service.claim;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.claim.Claim;
import by.bsuir.supplier.entity.claim.Interaction;
import by.bsuir.supplier.entity.enums.ClaimResult;
import by.bsuir.supplier.entity.enums.ClaimState;
import by.bsuir.supplier.entity.enums.InteractionChannel;
import by.bsuir.supplier.entity.enums.RemedyType;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.ViolationType;
import by.bsuir.supplier.entity.fulfillment.DeliveryLine;
import by.bsuir.supplier.entity.order.OrderLine;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.repository.claim.*;
import by.bsuir.supplier.repository.fulfillment.DeliveryLineRepository;
import by.bsuir.supplier.repository.order.OrderLineRepository;
import by.bsuir.supplier.repository.supplier.SupplierRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;

@Service
public class RelationshipService {
    private final ActorAccess access;
    private final SupplierRepository suppliers;
    private final OrderLineRepository orderLines;
    private final DeliveryLineRepository deliveryLines;
    private final ClaimRepository claims;
    private final InteractionRepository interactions;
    private final AuditTrail audit;
    public RelationshipService(ActorAccess access, SupplierRepository suppliers, OrderLineRepository orderLines,
        DeliveryLineRepository deliveryLines, ClaimRepository claims, InteractionRepository interactions, AuditTrail audit) {
        this.access = access;
        this.suppliers = suppliers;
        this.orderLines = orderLines;
        this.deliveryLines = deliveryLines;
        this.claims = claims;
        this.interactions = interactions;
        this.audit = audit;
    }
    @Transactional
    public Claim createClaim(UUID actorId, String number, UUID supplierId, UUID orderLineId, UUID deliveryLineId,
        ViolationType type, String description, String requirements, LocalDate deadline) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Supplier supplier = suppliers.findById(supplierId).orElseThrow(()->new BusinessException("Поставщик не найден"));
        OrderLine line = orderLines.findById(orderLineId).orElseThrow(()->new BusinessException("Строка заказа не найдена"));
        Rules.require(line.getOrder().getSupplier().getId().equals(supplierId),"Неверный поставщик претензии");
        DeliveryLine delivery = null;
        if (deliveryLineId != null) {
            delivery = deliveryLines.findById(deliveryLineId).orElseThrow(()->new BusinessException("Строка поставки не найдена"));
            Rules.require(delivery.getOrderVersionLine().getOrderLine().getId().equals(orderLineId),"Поставка другого заказа");
        }
        Claim claim = new Claim();
        claim.setNumber(Rules.text(number,"Номер претензии"));
        claim.setSupplier(supplier);
        claim.setOrderLine(line);
        claim.setDeliveryLine(delivery);
        claim.setViolationType(Rules.present(type,"Тип нарушения"));
        claim.setDescription(Rules.text(description,"Описание"));
        claim.setRequirements(Rules.text(requirements,"Требования"));
        claim.setResponseDueOn(deadline);
        claim.setCreatedBy(actor);
        claim.setState(ClaimState.DRAFT);
        claim = claims.saveAndFlush(claim);
        audit.record(actor,"CLAIM_CREATED", claim,"Claim", claim.getId(), null);
        return claim;
    }
    @Transactional
    public Claim sendClaim(UUID actorId, UUID claimId, InteractionChannel channel) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Claim claim = claim(claimId);
        Rules.require(claim.getState() == ClaimState.DRAFT,"Претензия уже отправлена");
        claim.setSendChannel(Rules.present(channel,"Канал отправки"));
        claim.setSentAt(Instant.now());
        claim.setState(ClaimState.SENT);
        audit.record(actor,"CLAIM_SENT", claim,"Claim", claimId, null);
        return claim;
    }
    @Transactional
    public Claim beginResolution(UUID actorId, UUID claimId) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Claim claim = claim(claimId);
        Rules.require(claim.getState() == ClaimState.SENT,"Претензия должна быть отправлена");
        claim.setState(ClaimState.IN_RESOLUTION);
        audit.record(actor,"CLAIM_RESOLUTION_STARTED", claim,"Claim", claimId, null);
        return claim;
    }
    @Transactional
    public Claim resolve(UUID actorId, UUID claimId, ClaimResult result, RemedyType remedy, String description) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Claim claim = claim(claimId);
        Rules.require(claim.getState() == ClaimState.SENT || claim.getState() == ClaimState.IN_RESOLUTION,"Претензия не находится в работе");
        Rules.present(result,"Результат урегулирования");
        Rules.present(remedy,"Способ урегулирования");
        if (result != ClaimResult.FULL) {
            User manager = access.requireRole(actorId, RoleCode.PROCUREMENT_MANAGER);
            ActorAccess.independent(claim.getCreatedBy(), manager);
            claim.setApprovedBy(manager);
        }
        claim.setResult(result);
        claim.setRemedyType(remedy);
        claim.setResolutionDescription(Rules.text(description,"Описание решения"));
        claim.setResolvedAt(Instant.now());
        claim.setState(ClaimState.CLOSED);
        audit.record(actor,"CLAIM_CLOSED", claim,"Claim", claimId, description);
        return claim;
    }
    @Transactional
    public Interaction logInteraction(UUID actorId, UUID supplierId, UUID claimId, InteractionChannel channel,
        Instant occurredAt, String subject, String content, String outcome) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Supplier supplier = suppliers.findById(supplierId).orElseThrow(()->new BusinessException("Поставщик не найден"));
        Claim claim = claimId == null?null:claim(claimId);
        if (claim != null)Rules.require(claim.getSupplier().getId().equals(supplierId),"Претензия другого поставщика");
        Interaction interaction = new Interaction();
        interaction.setSupplier(supplier);
        interaction.setClaim(claim);
        interaction.setEmployee(actor);
        interaction.setChannel(Rules.present(channel,"Канал"));
        interaction.setOccurredAt(Rules.present(occurredAt,"Дата взаимодействия"));
        Rules.require(!occurredAt.isAfter(Instant.now()),"Событие не может быть в будущем");
        interaction.setSubject(Rules.text(subject,"Тема"));
        interaction.setContent(Rules.text(content,"Содержание"));
        interaction.setOutcome(outcome);
        interaction = interactions.saveAndFlush(interaction);
        audit.record(actor,"INTERACTION_RECORDED", interaction,"Interaction", interaction.getId(), null);
        return interaction;
    }
    private Claim claim(UUID id) {
        return claims.findById(id).orElseThrow(()->new BusinessException("Претензия не найдена"));
    }
}
