package by.bsuir.supplier.service.query;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.response.OrderClosureView;
import by.bsuir.supplier.dto.response.OrderLineView;
import by.bsuir.supplier.dto.response.OrderVersionLineView;
import by.bsuir.supplier.dto.response.OrderVersionView;
import by.bsuir.supplier.dto.response.PurchaseOrderView;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.order.OrderClosure;
import by.bsuir.supplier.entity.order.OrderLine;
import by.bsuir.supplier.entity.order.OrderVersion;
import by.bsuir.supplier.entity.order.OrderVersionLine;
import by.bsuir.supplier.entity.order.PurchaseOrder;
import by.bsuir.supplier.mapper.OrderViewMapper;
import by.bsuir.supplier.repository.order.OrderClosureRepository;
import by.bsuir.supplier.repository.order.OrderLineRepository;
import by.bsuir.supplier.repository.order.OrderVersionLineRepository;
import by.bsuir.supplier.repository.order.OrderVersionRepository;
import by.bsuir.supplier.repository.order.PurchaseOrderRepository;
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
public class OrderQueries {
    private final ActorAccess access;
    private final PurchaseOrderRepository purchaseOrderRepo;
    private final OrderVersionRepository orderVersionRepo;
    private final OrderVersionLineRepository orderVersionLineRepo;
    private final OrderLineRepository orderLineRepo;
    private final OrderClosureRepository orderClosureRepo;

    public OrderQueries(
            ActorAccess access,
            PurchaseOrderRepository purchaseOrderRepo,
            OrderVersionRepository orderVersionRepo,
            OrderVersionLineRepository orderVersionLineRepo,
            OrderLineRepository orderLineRepo,
            OrderClosureRepository orderClosureRepo) {
        this.access = access;
        this.purchaseOrderRepo = purchaseOrderRepo;
        this.orderVersionRepo = orderVersionRepo;
        this.orderVersionLineRepo = orderVersionLineRepo;
        this.orderLineRepo = orderLineRepo;
        this.orderClosureRepo = orderClosureRepo;
    }

    private static Pageable pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new BusinessException("Недопустимые параметры страницы (size: 1–100)");
        return PageRequest.of(page, size, Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public PurchaseOrderView getPurchaseOrder(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return OrderViewMapper.toView(
                purchaseOrderRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<PurchaseOrderView> listPurchaseOrder(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<PurchaseOrder> result = purchaseOrderRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(OrderViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public OrderVersionView getOrderVersion(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return OrderViewMapper.toView(
                orderVersionRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderVersionView> listOrderVersion(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<OrderVersion> result = orderVersionRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(OrderViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public OrderVersionLineView getOrderVersionLine(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return OrderViewMapper.toView(
                orderVersionLineRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderVersionLineView> listOrderVersionLine(
            UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<OrderVersionLine> result = orderVersionLineRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(OrderViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public OrderLineView getOrderLine(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return OrderViewMapper.toView(
                orderLineRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderLineView> listOrderLine(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<OrderLine> result = orderLineRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(OrderViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public OrderClosureView getOrderClosure(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return OrderViewMapper.toView(
                orderClosureRepo
                        .findById(id)
                        .orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderClosureView> listOrderClosure(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<OrderClosure> result = orderClosureRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(OrderViewMapper::toView));
    }

    @Transactional(readOnly = true)
    public List<OrderVersionView> orderVersions(UUID actorId, UUID orderId) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        purchaseOrderRepo
                .findById(orderId)
                .orElseThrow(() -> new BusinessException("Заказ не найден"));
        return orderVersionRepo.findByOrder_Id(orderId).stream()
                .map(OrderViewMapper::toView)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<OrderVersionLineView> versionLines(UUID actorId, UUID versionId) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        orderVersionRepo
                .findById(versionId)
                .orElseThrow(() -> new BusinessException("Редакция не найдена"));
        return orderVersionLineRepo.findByOrderVersion_Id(versionId).stream()
                .map(OrderViewMapper::toView)
                .toList();
    }
}
