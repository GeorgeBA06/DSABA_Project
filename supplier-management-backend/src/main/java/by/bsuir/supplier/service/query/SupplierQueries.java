package by.bsuir.supplier.service.query;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.response.SupplierOfferView;
import by.bsuir.supplier.dto.response.SupplierStatusRequestView;
import by.bsuir.supplier.dto.response.SupplierView;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.entity.supplier.SupplierStatusRequest;
import by.bsuir.supplier.mapper.SupplierViewMapper;
import by.bsuir.supplier.repository.supplier.SupplierProductRepository;
import by.bsuir.supplier.repository.supplier.SupplierRepository;
import by.bsuir.supplier.repository.supplier.SupplierStatusRequestRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.BusinessException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SupplierQueries {
    private final ActorAccess access;
    private final SupplierProductRepository offers;
    private final SupplierRepository supplierRepo;
    private final SupplierStatusRequestRepository supplierStatusRequestRepo;

    public SupplierQueries(
            ActorAccess access,
            SupplierProductRepository offers,
            SupplierRepository supplierRepo,
            SupplierStatusRequestRepository supplierStatusRequestRepo) {
        this.access = access;
        this.offers = offers;
        this.supplierRepo = supplierRepo;
        this.supplierStatusRequestRepo = supplierStatusRequestRepo;
    }

    @Transactional(readOnly = true)
    public List<SupplierOfferView> offers(UUID actorId, UUID supplierId) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        supplierRepo
                .findById(supplierId)
                .orElseThrow(() -> new BusinessException("Поставщик не найден"));
        return offers.findBySupplier_Id(supplierId).stream()
                .map(SupplierViewMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierView> search(UUID actorId, String name, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return PageResponse.from(
                supplierRepo
                        .findByNameContainingIgnoreCase(name, pageRequest(page, size))
                        .map(SupplierViewMapper::toView));
    }

    private static Pageable pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new BusinessException("Недопустимые параметры страницы (size: 1–100)");
        return PageRequest.of(page, size, Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public SupplierView getSupplier(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return SupplierViewMapper.toView(
                supplierRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierView> listSupplier(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<Supplier> result = supplierRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(SupplierViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public SupplierStatusRequestView getSupplierStatusRequest(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return SupplierViewMapper.toView(
                supplierStatusRequestRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierStatusRequestView> listSupplierStatusRequest(
            UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<SupplierStatusRequest> result =
                supplierStatusRequestRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(SupplierViewMapper::toView));
    }
}
