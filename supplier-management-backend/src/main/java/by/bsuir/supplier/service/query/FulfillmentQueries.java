package by.bsuir.supplier.service.query;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.response.AcceptanceDocumentView;
import by.bsuir.supplier.dto.response.AcceptanceLineView;
import by.bsuir.supplier.dto.response.DeliveryLineView;
import by.bsuir.supplier.dto.response.DeliveryView;
import by.bsuir.supplier.dto.response.FactCorrectionView;
import by.bsuir.supplier.dto.response.SupplierReturnLineView;
import by.bsuir.supplier.dto.response.SupplierReturnView;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.fulfillment.AcceptanceDocument;
import by.bsuir.supplier.entity.fulfillment.AcceptanceLine;
import by.bsuir.supplier.entity.fulfillment.Delivery;
import by.bsuir.supplier.entity.fulfillment.DeliveryLine;
import by.bsuir.supplier.entity.fulfillment.FactCorrection;
import by.bsuir.supplier.entity.fulfillment.SupplierReturn;
import by.bsuir.supplier.entity.fulfillment.SupplierReturnLine;
import by.bsuir.supplier.mapper.FulfillmentViewMapper;
import by.bsuir.supplier.repository.fulfillment.AcceptanceDocumentRepository;
import by.bsuir.supplier.repository.fulfillment.AcceptanceLineRepository;
import by.bsuir.supplier.repository.fulfillment.DeliveryLineRepository;
import by.bsuir.supplier.repository.fulfillment.DeliveryRepository;
import by.bsuir.supplier.repository.fulfillment.FactCorrectionRepository;
import by.bsuir.supplier.repository.fulfillment.SupplierReturnLineRepository;
import by.bsuir.supplier.repository.fulfillment.SupplierReturnRepository;
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
public class FulfillmentQueries {
    private final ActorAccess access;
    private final DeliveryRepository deliveryRepo;
    private final DeliveryLineRepository deliveryLineRepo;
    private final AcceptanceDocumentRepository acceptanceDocumentRepo;
    private final AcceptanceLineRepository acceptanceLineRepo;
    private final SupplierReturnRepository supplierReturnRepo;
    private final SupplierReturnLineRepository supplierReturnLineRepo;
    private final FactCorrectionRepository factCorrectionRepo;

    public FulfillmentQueries(
            ActorAccess access,
            DeliveryRepository deliveryRepo,
            DeliveryLineRepository deliveryLineRepo,
            AcceptanceDocumentRepository acceptanceDocumentRepo,
            AcceptanceLineRepository acceptanceLineRepo,
            SupplierReturnRepository supplierReturnRepo,
            SupplierReturnLineRepository supplierReturnLineRepo,
            FactCorrectionRepository factCorrectionRepo) {
        this.access = access;
        this.deliveryRepo = deliveryRepo;
        this.deliveryLineRepo = deliveryLineRepo;
        this.acceptanceDocumentRepo = acceptanceDocumentRepo;
        this.acceptanceLineRepo = acceptanceLineRepo;
        this.supplierReturnRepo = supplierReturnRepo;
        this.supplierReturnLineRepo = supplierReturnLineRepo;
        this.factCorrectionRepo = factCorrectionRepo;
    }

    private static Pageable pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new BusinessException("Недопустимые параметры страницы (size: 1–100)");
        return PageRequest.of(page, size, Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public DeliveryView getDelivery(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return FulfillmentViewMapper.toView(
                deliveryRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<DeliveryView> listDelivery(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<Delivery> result = deliveryRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(FulfillmentViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public DeliveryLineView getDeliveryLine(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return FulfillmentViewMapper.toView(
                deliveryLineRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<DeliveryLineView> listDeliveryLine(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<DeliveryLine> result = deliveryLineRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(FulfillmentViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public AcceptanceDocumentView getAcceptanceDocument(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return FulfillmentViewMapper.toView(
                acceptanceDocumentRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<AcceptanceDocumentView> listAcceptanceDocument(
            UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<AcceptanceDocument> result = acceptanceDocumentRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(FulfillmentViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public AcceptanceLineView getAcceptanceLine(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return FulfillmentViewMapper.toView(
                acceptanceLineRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<AcceptanceLineView> listAcceptanceLine(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<AcceptanceLine> result = acceptanceLineRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(FulfillmentViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public SupplierReturnView getSupplierReturn(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return FulfillmentViewMapper.toView(
                supplierReturnRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierReturnView> listSupplierReturn(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<SupplierReturn> result = supplierReturnRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(FulfillmentViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public SupplierReturnLineView getSupplierReturnLine(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return FulfillmentViewMapper.toView(
                supplierReturnLineRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<SupplierReturnLineView> listSupplierReturnLine(
            UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<SupplierReturnLine> result = supplierReturnLineRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(FulfillmentViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public FactCorrectionView getFactCorrection(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return FulfillmentViewMapper.toView(
                factCorrectionRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<FactCorrectionView> listFactCorrection(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<FactCorrection> result = factCorrectionRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(FulfillmentViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public List<DeliveryLineView> deliveryLines(UUID actorId, UUID deliveryId) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        deliveryRepo
                .findById(deliveryId)
                .orElseThrow(() -> new BusinessException("Поступление не найдено"));
        return deliveryLineRepo.findByDelivery_Id(deliveryId).stream()
                .map(FulfillmentViewMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<AcceptanceLineView> acceptanceLines(UUID actorId, UUID acceptanceId) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        acceptanceDocumentRepo
                .findById(acceptanceId)
                .orElseThrow(() -> new BusinessException("Приёмка не найдена"));
        return acceptanceLineRepo.findByAcceptanceDocument_Id(acceptanceId).stream()
                .map(FulfillmentViewMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<SupplierReturnLineView> returnLines(UUID actorId, UUID returnId) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        supplierReturnRepo
                .findById(returnId)
                .orElseThrow(() -> new BusinessException("Возврат не найден"));
        return supplierReturnLineRepo.findBySupplierReturn_Id(returnId).stream()
                .map(FulfillmentViewMapper::toView)
                .toList();
    }
}
