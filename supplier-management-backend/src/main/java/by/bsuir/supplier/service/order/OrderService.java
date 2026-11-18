package by.bsuir.supplier.service.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.catalog.Product;
import by.bsuir.supplier.entity.contract.Contract;
import by.bsuir.supplier.entity.enums.Initiator;
import by.bsuir.supplier.entity.enums.OrderState;
import by.bsuir.supplier.entity.enums.OrderVersionKind;
import by.bsuir.supplier.entity.enums.OriginReason;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.SupplierStatus;
import by.bsuir.supplier.entity.enums.VersionState;
import by.bsuir.supplier.entity.order.OrderLine;
import by.bsuir.supplier.entity.order.OrderVersion;
import by.bsuir.supplier.entity.order.OrderVersionLine;
import by.bsuir.supplier.entity.order.PurchaseOrder;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.entity.supplier.SupplierProductId;
import by.bsuir.supplier.repository.catalog.ProductRepository;
import by.bsuir.supplier.repository.contract.ContractRepository;
import by.bsuir.supplier.repository.order.*;
import by.bsuir.supplier.repository.supplier.*;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;
import by.bsuir.supplier.service.contract.ContractService;

/** Versioned purchase orders. All changes create new immutable-on-approval versions. */
@Service
public class OrderService {
    private final ActorAccess access;
    private final PurchaseOrderRepository orders;
    private final OrderLineRepository lines;
    private final OrderVersionRepository versions;
    private final OrderVersionLineRepository segments;
    private final SupplierRepository suppliers;
    private final SupplierProductRepository offers;
    private final ProductRepository products;
    private final ContractRepository contracts;
    private final AuditTrail audit;
    public OrderService(ActorAccess access, PurchaseOrderRepository orders, OrderLineRepository lines,
        OrderVersionRepository versions, OrderVersionLineRepository segments, SupplierRepository suppliers,
        SupplierProductRepository offers, ProductRepository products, ContractRepository contracts, AuditTrail audit) {
        this.access = access;
        this.orders = orders;
        this.lines = lines;
        this.versions = versions;
        this.segments = segments;
        this.suppliers = suppliers;
        this.offers = offers;
        this.products = products;
        this.contracts = contracts;
        this.audit = audit;
    }
    public record Item(UUID productId, BigDecimal quantity, BigDecimal unitPrice, LocalDate dueOn) {
    }
    private static final ZoneId ZONE = ZoneId.of("Europe/Minsk");
    @Transactional
    public PurchaseOrder create(UUID actorId, String number, UUID supplierId, UUID contractId, List<Item> items) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Supplier supplier = suppliers.findById(supplierId).orElseThrow(()->new BusinessException("Поставщик не найден"));
        Rules.require(supplier.getStatus() != SupplierStatus.TERMINATED,"Сотрудничество прекращено");
        Contract contract = contractId == null?null:contracts.findById(contractId).orElseThrow(()->new BusinessException("Договор не найден"));
        if (contract != null)Rules.require(contract.getSupplier().getId().equals(supplierId),"Договор другого поставщика");
        Rules.notEmpty(items,"Позиции заказа");
        PurchaseOrder order = new PurchaseOrder();
        order.setNumber(Rules.text(number,"Номер заказа"));
        order.setSupplier(supplier);
        order.setContract(contract);
        order.setCreatedBy(actor);
        order.setState(OrderState.DRAFT);
        order = orders.saveAndFlush(order);
        OrderVersion v = new OrderVersion();
        v.setOrder(order);
        v.setVersionNo(1);
        v.setKind(OrderVersionKind.INITIAL);
        v.setState(VersionState.DRAFT);
        v.setCreatedBy(actor);
        v = versions.saveAndFlush(v);
        createLines(supplier, order, v, items);
        audit.record(actor,"ORDER_CREATED", order,"PurchaseOrder", order.getId(), null);
        return order;
    }
    /** Edit only the initial unsent draft. Historical versions are never rewritten. */
    @Transactional
    public void replaceInitialDraft(UUID actorId, UUID orderId, List<Item> items) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        PurchaseOrder order = locked(orderId);
        Rules.require(order.getState() == OrderState.DRAFT,"Только черновик");
        OrderVersion v = initial(orderId);
        Rules.require(v.getState() == VersionState.DRAFT,"Версия уже отправлена");
        Rules.require(order.getCreatedBy().getId().equals(actorId),"Черновик редактирует автор");
        Rules.notEmpty(items,"Позиции заказа");
        segments.deleteAll(segments.findByOrderVersion_Id(v.getId()));
        segments.flush();
        lines.deleteAll(lines.findByOrder_Id(orderId));
        lines.flush();
        createLines(order.getSupplier(), order, v, items);
        audit.record(actor,"ORDER_DRAFT_REPLACED", order,"PurchaseOrder", orderId, null);
    }
    @Transactional
    public OrderVersion submit(UUID actorId, UUID versionId) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        OrderVersion v = version(versionId);
        PurchaseOrder order = locked(v.getOrder().getId());
        Rules.require(v.getState() == VersionState.DRAFT,"Версия уже отправлена");
        Rules.require(order.getState() != OrderState.CANCELLED && order.getState() != OrderState.COMPLETED,"Заказ закрыт");
        Rules.require(order.getSupplier().getStatus() == SupplierStatus.ACTIVE,"Поставщик не активен");
        ContractService.requireCurrent(order.getContract(), LocalDate.now(ZONE));
        Rules.notEmpty(segments.findByOrderVersion_Id(versionId),"Позиции версии");
        if (v.getKind() == OrderVersionKind.CHANGE) {
            Rules.require((order.getCurrentApprovedVersion() != null && v.getBaseVersion().getId().equals(order.getCurrentApprovedVersion().getId())) || (order.getCurrentApprovedVersion() == null && order.getState() == OrderState.DRAFT && v.getBaseVersion().getState() == VersionState.REJECTED),"Базовая версия устарела");
        }
        else Rules.require(order.getCurrentApprovedVersion() == null,"Исходная версия уже согласована");
        v.setContractVersion(order.getContract().getCurrentRegisteredVersion());
        v.setSubmittedAt(Instant.now());
        v.setState(VersionState.PENDING);
        versions.saveAndFlush(v);
        if (order.getCurrentApprovedVersion() == null)order.setState(OrderState.PENDING_APPROVAL);
        audit.record(actor,"ORDER_VERSION_SUBMITTED", v,"OrderVersion", v.getId(), null);
        return v;
    }
    @Transactional
    public OrderVersion decide(UUID managerId, UUID versionId, boolean approve, String comment) {
        User manager = access.requireRole(managerId, RoleCode.PROCUREMENT_MANAGER);
        OrderVersion v = version(versionId);
        PurchaseOrder order = locked(v.getOrder().getId());
        Rules.require(v.getState() == VersionState.PENDING,"Редакция не ожидает согласования");
        ActorAccess.independent(v.getCreatedBy(), manager);
        if (approve) {
            Rules.require(order.getSupplier().getStatus() == SupplierStatus.ACTIVE,"Поставщик не активен");
            ContractService.requireCurrent(order.getContract(), LocalDate.now(ZONE));
            Rules.require(v.getContractVersion() != null && v.getContractVersion().getContract().getId().equals(order.getContract().getId()),
                "Некорректная версия договора");
            if (v.getKind() == OrderVersionKind.CHANGE)Rules.require(
                (order.getCurrentApprovedVersion() != null && v.getBaseVersion().getId().equals(order.getCurrentApprovedVersion().getId())) || (order.getCurrentApprovedVersion() == null && order.getState() == OrderState.PENDING_APPROVAL && v.getBaseVersion().getState() == VersionState.REJECTED),"Базовая версия устарела");
            else Rules.require(order.getCurrentApprovedVersion() == null,"Заказ уже согласован");
        }
        else Rules.text(comment,"Причина отклонения");
        v.setState(approve?VersionState.APPROVED:VersionState.REJECTED);
        v.setDecidedBy(manager);
        v.setDecidedAt(Instant.now());
        v.setDecisionComment(comment);
        versions.saveAndFlush(v);
        // trigger checks approved pointer on order update
        if (approve) {
            order.setCurrentApprovedVersion(v);
            if (order.getState() == OrderState.DRAFT || order.getState() == OrderState.PENDING_APPROVAL)order.setState(OrderState.APPROVED);
        }
        else if (order.getCurrentApprovedVersion() == null)order.setState(OrderState.DRAFT);
        audit.record(manager, approve?"ORDER_VERSION_APPROVED":"ORDER_VERSION_REJECTED", v,"OrderVersion", v.getId(), comment);
        return v;
    }
    /** Resubmit an initially rejected order without modifying its historical rejected version. */
    @Transactional
    public OrderVersion reviseRejected(UUID actorId, UUID orderId, List<Item> corrected, String reason) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        PurchaseOrder order = locked(orderId);
        Rules.require(order.getState() == OrderState.DRAFT && order.getCurrentApprovedVersion() == null,"Переработка доступна только до первого утверждения");
        Rules.require(order.getCreatedBy().getId().equals(actorId),"Исправления вносит автор");
        OrderVersion base = versions.findByOrder_Id(orderId).stream().max(Comparator.comparingInt(OrderVersion::getVersionNo))
            .orElseThrow(()->new BusinessException("Версия заказа отсутствует"));
        Rules.require(base.getState() == VersionState.REJECTED,"Последняя редакция не отклонена");
        Rules.require(versions.findByOrder_Id(orderId).stream().noneMatch(v->v.getKind() == OrderVersionKind.CHANGE && (v.getState() == VersionState.DRAFT || v.getState() == VersionState.PENDING)),"Есть незавершённая редакция");
        List<OrderLine> oldLines = lines.findByOrder_Id(orderId).stream().sorted(Comparator.comparingInt(OrderLine::getLineNo)).toList();
        Rules.require(corrected != null && corrected.size() == oldLines.size(),"Состав строк должен соответствовать исходному заказу");
        OrderVersion newVersion = new OrderVersion();
        newVersion.setOrder(order);
        newVersion.setVersionNo(base.getVersionNo()+1);
        newVersion.setBaseVersion(base);
        newVersion.setKind(OrderVersionKind.CHANGE);
        newVersion.setInitiator(Initiator.BUYER);
        newVersion.setReason(Rules.text(reason,"Причина переработки"));
        newVersion.setState(VersionState.DRAFT);
        newVersion.setCreatedBy(actor);
        newVersion = versions.saveAndFlush(newVersion);
        List<OrderVersionLine> past = segments.findByOrderVersion_Id(base.getId());
        for (int i = 0;i<oldLines.size();i++) {
            OrderLine line = oldLines.get(i);
            Item item = Rules.present(corrected.get(i),"Позиция");
            Rules.require(line.getProduct().getId().equals(item.productId()),"Менять номенклатуру переработанной строки нельзя");
            Rules.positive(item.quantity(),"Количество");
            Rules.nonnegative(item.unitPrice(),"Цена");
            Rules.present(item.dueOn(),"Срок");
            OrderVersionLine source = past.stream().filter(s->s.getOrderLine().getId().equals(line.getId())).findFirst()
                .orElseThrow(()->new BusinessException("Исходный сегмент отсутствует"));
            OrderVersionLine next = copySegment(source, newVersion);
            next.setQuantity(item.quantity());
            next.setUnitPrice(item.unitPrice());
            next.setExecutionDueOn(item.dueOn());
            next.setRatingDueOn(item.dueOn());
            next.setOriginReason(OriginReason.BUYER_CHANGE);
            segments.save(next);
        }
        audit.record(actor,"ORDER_REVISED_AFTER_REJECTION", newVersion,"OrderVersion", newVersion.getId(), reason);
        return newVersion;
    }
    /** A buyer increment creates a distinct commitment segment with its own rating deadline. */
    @Transactional
    public OrderVersion proposeIncrease(UUID actorId, UUID orderId, UUID orderLineId, BigDecimal additional,
        BigDecimal price, LocalDate dueOn, String reason) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        PurchaseOrder o = locked(orderId);
        OrderVersion v = beginChange(o, actor, Initiator.BUYER, reason);
        OrderLine line = lines.findById(orderLineId).orElseThrow(()->new BusinessException("Строка заказа не найдена"));
        Rules.require(line.getOrder().getId().equals(orderId),"Строка другого заказа");
        Rules.positive(additional,"Добавляемое количество");
        Rules.nonnegative(price,"Цена");
        Rules.present(dueOn,"Срок");
        OrderVersionLine source = segments.findByOrderVersion_Id(o.getCurrentApprovedVersion().getId()).stream()
            .filter(s->s.getOrderLine().getId().equals(orderLineId)).findFirst().orElseThrow(()->new BusinessException("В исходной версии нет строки"));
        OrderVersionLine newSegment = copySegment(source, v);
        newSegment.setId(null);
        newSegment.setSegmentKey(UUID.randomUUID().toString());
        newSegment.setSourceVersionLine(null);
        newSegment.setQuantity(additional);
        newSegment.setUnitPrice(price);
        newSegment.setExecutionDueOn(dueOn);
        newSegment.setRatingDueOn(dueOn);
        newSegment.setOriginReason(OriginReason.BUYER_CHANGE);
        segments.save(newSegment);
        audit.record(actor,"ORDER_INCREASE_PROPOSED", v,"OrderVersion", v.getId(), reason);
        return v;
    }
    /** Supplier-requested postponement preserves the original due date for rating. */
    @Transactional
    public OrderVersion proposeSupplierReschedule(
        UUID actorId,
        UUID orderId,
        String segmentKey,
        LocalDate newExecutionDue,
        String reason
    ) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        PurchaseOrder o = locked(orderId);
        Rules.present(newExecutionDue,"Новый срок");
        OrderVersion v = beginChange(o, actor, Initiator.SUPPLIER, reason);
        OrderVersionLine target = segments.findByOrderVersion_Id(v.getId()).stream()
            .filter(s->s.getSegmentKey().equals(segmentKey)).findFirst().orElseThrow(()->new BusinessException("Сегмент не найден"));
        Rules.require(!newExecutionDue.isBefore(target.getExecutionDueOn()),"Срок по заявке поставщика нельзя сократить");
        target.setExecutionDueOn(newExecutionDue);
        target.setOriginReason(OriginReason.SUPPLIER_RESCHEDULE);
        audit.record(actor,"ORDER_RESCHEDULE_PROPOSED", v,"OrderVersion", v.getId(), reason);
        return v;
    }
    private OrderVersion beginChange(PurchaseOrder o, User actor, Initiator initiator, String reason) {
        Rules.require(o.getState() == OrderState.APPROVED || o.getState() == OrderState.IN_PROGRESS,"Заказ не исполняется");
        OrderVersion approved = Rules.present(o.getCurrentApprovedVersion(),"Согласованная версия");
        Rules.require(versions.findByOrder_Id(o.getId()).stream().noneMatch(v->v.getKind() == OrderVersionKind.CHANGE && (v.getState() == VersionState.DRAFT || v.getState() == VersionState.PENDING)),"Уже есть открытое изменение");
        int next = versions.findByOrder_Id(o.getId()).stream().mapToInt(OrderVersion::getVersionNo).max().orElse(0)+1;
        OrderVersion v = new OrderVersion();
        v.setOrder(o);
        v.setVersionNo(next);
        v.setBaseVersion(approved);
        v.setKind(OrderVersionKind.CHANGE);
        v.setInitiator(initiator);
        v.setReason(Rules.text(reason,"Причина изменения"));
        v.setState(VersionState.DRAFT);
        v.setCreatedBy(actor);
        v.setContractVersion(approved.getContractVersion());
        v = versions.saveAndFlush(v);
        for (OrderVersionLine prev:segments.findByOrderVersion_Id(approved.getId()))segments.save(copySegment(prev, v));
        segments.flush();
        return v;
    }
    private static OrderVersionLine copySegment(OrderVersionLine src, OrderVersion target) {
        OrderVersionLine item = new OrderVersionLine();
        item.setOrderVersion(target);
        item.setOrderLine(src.getOrderLine());
        item.setSegmentKey(src.getSegmentKey());
        item.setSourceVersionLine(src);
        item.setQuantity(src.getQuantity());
        item.setUnitPrice(src.getUnitPrice());
        item.setExecutionDueOn(src.getExecutionDueOn());
        item.setRatingDueOn(src.getRatingDueOn());
        item.setOriginReason(src.getOriginReason());
        item.setProductNameSnapshot(src.getProductNameSnapshot());
        item.setCategoryCodeSnapshot(src.getCategoryCodeSnapshot());
        item.setCategoryNameSnapshot(src.getCategoryNameSnapshot());
        item.setUnitCodeSnapshot(src.getUnitCodeSnapshot());
        return item;
    }
    private void createLines(Supplier supplier, PurchaseOrder order, OrderVersion version, List<Item> items) {
        int index = 0;
        for (Item i:items) {
            Rules.present(i,"Позиция");
            Rules.positive(i.quantity(),"Количество");
            Rules.nonnegative(i.unitPrice(),"Цена");
            Rules.present(i.dueOn(),"Срок");
            Product product = products.findById(Rules.present(i.productId(),"Товар")).orElseThrow(()->new BusinessException("Товар не найден"));
            Rules.require(Boolean.TRUE.equals(product.getActive()),"Неактивный товар");
            SupplierProductId key = new SupplierProductId(supplier.getId(), product.getId());
            Rules.require(offers.findById(key).map(p->Boolean.TRUE.equals(p.getActive())).orElse(false),"Товар не предлагается поставщиком");
            OrderLine line = new OrderLine();
            line.setOrder(order);
            line.setLineNo(++index);
            line.setProduct(product);
            line = lines.saveAndFlush(line);
            OrderVersionLine segment = new OrderVersionLine();
            segment.setOrderVersion(version);
            segment.setOrderLine(line);
            segment.setSegmentKey(UUID.randomUUID().toString());
            segment.setQuantity(i.quantity());
            segment.setUnitPrice(i.unitPrice());
            segment.setExecutionDueOn(i.dueOn());
            segment.setRatingDueOn(i.dueOn());
            segment.setOriginReason(OriginReason.INITIAL);
            segment.setProductNameSnapshot(product.getName());
            segment.setCategoryCodeSnapshot(product.getCategory().getCode());
            segment.setCategoryNameSnapshot(product.getCategory().getName());
            segment.setUnitCodeSnapshot(product.getUnit().getCode());
            segments.save(segment);
        }
        segments.flush();
    }
    private PurchaseOrder locked(UUID id) {
        return orders.findByIdForUpdate(id).orElseThrow(()->new BusinessException("Заказ не найден"));
    }
    private OrderVersion version(UUID id) {
        return versions.findById(id).orElseThrow(()->new BusinessException("Редакция заказа не найдена"));
    }
    private OrderVersion initial(UUID orderId) {
        return versions.findByOrder_Id(orderId).stream().filter(v->v.getKind() == OrderVersionKind.INITIAL).findFirst().orElseThrow(()->new BusinessException("Исходная редакция не найдена"));
    }
}
