package by.bsuir.supplier.service.supplier;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.catalog.Product;
import by.bsuir.supplier.entity.enums.DecisionState;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.SupplierStatus;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.entity.supplier.SupplierProduct;
import by.bsuir.supplier.entity.supplier.SupplierProductId;
import by.bsuir.supplier.entity.supplier.SupplierStatusRequest;
import by.bsuir.supplier.repository.catalog.ProductRepository;
import by.bsuir.supplier.repository.supplier.*;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
public class SupplierService {
    private final ActorAccess access;
    private final SupplierRepository suppliers;
    private final SupplierProductRepository offers;
    private final SupplierStatusRequestRepository requests;
    private final ProductRepository products;
    private final AuditTrail audit;

    public SupplierService(
            ActorAccess access,
            SupplierRepository suppliers,
            SupplierProductRepository offers,
            SupplierStatusRequestRepository requests,
            ProductRepository products,
            AuditTrail audit) {
        this.access = access;
        this.suppliers = suppliers;
        this.offers = offers;
        this.requests = requests;
        this.products = products;
        this.audit = audit;
    }

    public record Details(
            String name,
            String contactName,
            String email,
            String phone,
            String address,
            String registrationNumber) {}

    @Transactional
    public Supplier create(UUID actorId, Details details) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Supplier s = new Supplier();
        fill(s, details);
        s.setStatus(SupplierStatus.CANDIDATE);
        s.setCreatedBy(actor);
        s = suppliers.saveAndFlush(s);
        audit.record(actor, "SUPPLIER_CREATED", s, "Supplier", s.getId(), null);
        return s;
    }

    @Transactional
    public Supplier update(UUID actorId, UUID supplierId, Details details) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Supplier s =
                suppliers
                        .findByIdForUpdate(supplierId)
                        .orElseThrow(() -> new BusinessException("Поставщик не найден"));
        Rules.require(s.getStatus() != SupplierStatus.TERMINATED, "Сотрудничество прекращено");
        fill(s, details);
        audit.record(actor, "SUPPLIER_UPDATED", s, "Supplier", s.getId(), null);
        return s;
    }

    private void fill(Supplier s, Details d) {
        Rules.present(d, "Данные поставщика");
        s.setName(Rules.text(d.name(), "Наименование"));
        s.setContactName(d.contactName());
        s.setEmail(d.email());
        s.setPhone(d.phone());
        s.setAddress(d.address());
        s.setRegistrationNumber(d.registrationNumber());
    }

    @Transactional
    public SupplierProduct offerProduct(
            UUID actorId, UUID supplierId, UUID productId, String supplierCode) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Supplier s =
                suppliers
                        .findByIdForUpdate(supplierId)
                        .orElseThrow(() -> new BusinessException("Поставщик не найден"));
        Rules.require(s.getStatus() != SupplierStatus.TERMINATED, "Сотрудничество прекращено");
        Product product =
                products.findById(productId)
                        .orElseThrow(() -> new BusinessException("Товар не найден"));
        Rules.require(Boolean.TRUE.equals(product.getActive()), "Товар неактивен");
        SupplierProductId id = new SupplierProductId(s.getId(), product.getId());
        SupplierProduct sp = offers.findById(id).orElseGet(() -> new SupplierProduct(id));
        sp.setSupplier(s);
        sp.setProduct(product);
        sp.setSupplierItemCode(supplierCode);
        sp.setActive(true);
        sp = offers.save(sp);
        audit.record(actor, "SUPPLIER_PRODUCT_LINKED", sp, "Supplier", s.getId(), null);
        return sp;
    }

    @Transactional
    public SupplierStatusRequest requestStatus(
            UUID actorId, UUID supplierId, SupplierStatus target, String reason) {
        User actor =
                access.requireRole(
                        actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Supplier s =
                suppliers
                        .findByIdForUpdate(supplierId)
                        .orElseThrow(() -> new BusinessException("Поставщик не найден"));
        validateTransition(s.getStatus(), Rules.present(target, "Новый статус"));
        SupplierStatusRequest r = new SupplierStatusRequest();
        r.setSupplier(s);
        r.setFromStatus(s.getStatus());
        r.setToStatus(target);
        r.setRequestedBy(actor);
        r.setRequestedAt(Instant.now());
        r.setReason(Rules.text(reason, "Основание"));
        r.setState(DecisionState.PENDING);
        r = requests.saveAndFlush(r);
        audit.record(
                actor, "SUPPLIER_STATUS_REQUESTED", r, "SupplierStatusRequest", r.getId(), reason);
        return r;
    }

    @Transactional
    public SupplierStatusRequest decideStatus(
            UUID managerId, UUID requestId, boolean approve, String comment) {
        User manager = access.requireRole(managerId, RoleCode.PROCUREMENT_MANAGER);
        SupplierStatusRequest r =
                requests.findById(requestId)
                        .orElseThrow(() -> new BusinessException("Заявка не найдена"));
        Supplier s =
                suppliers
                        .findByIdForUpdate(r.getSupplier().getId())
                        .orElseThrow(() -> new BusinessException("Поставщик не найден"));
        Rules.require(r.getState() == DecisionState.PENDING, "Заявка уже рассмотрена");
        ActorAccess.independent(r.getRequestedBy(), manager);
        Rules.require(s.getStatus() == r.getFromStatus(), "Статус изменился после подачи заявки");
        if (approve) {
            validateTransition(s.getStatus(), r.getToStatus());
            s.setStatus(r.getToStatus());
        } else Rules.text(comment, "Причина отклонения");
        r.setState(approve ? DecisionState.APPROVED : DecisionState.REJECTED);
        r.setDecidedBy(manager);
        r.setDecidedAt(Instant.now());
        r.setDecisionComment(comment);
        audit.record(
                manager,
                approve ? "SUPPLIER_STATUS_APPROVED" : "SUPPLIER_STATUS_REJECTED",
                r,
                "SupplierStatusRequest",
                r.getId(),
                comment);
        return r;
    }

    private static void validateTransition(SupplierStatus from, SupplierStatus to) {
        boolean ok =
                switch (from) {
                    case CANDIDATE ->
                            to == SupplierStatus.ACTIVE || to == SupplierStatus.TERMINATED;
                    case ACTIVE ->
                            to == SupplierStatus.SUSPENDED || to == SupplierStatus.TERMINATED;
                    case SUSPENDED ->
                            to == SupplierStatus.ACTIVE || to == SupplierStatus.TERMINATED;
                    case TERMINATED -> false;
                };
        Rules.require(ok, "Недопустимый переход состояния поставщика");
    }
}
