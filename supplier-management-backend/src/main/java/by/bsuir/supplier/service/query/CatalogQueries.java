package by.bsuir.supplier.service.query;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.response.ProductCategoryView;
import by.bsuir.supplier.dto.response.ProductView;
import by.bsuir.supplier.dto.response.UnitView;
import by.bsuir.supplier.entity.catalog.Product;
import by.bsuir.supplier.entity.catalog.ProductCategory;
import by.bsuir.supplier.entity.catalog.Unit;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.mapper.CatalogViewMapper;
import by.bsuir.supplier.repository.catalog.ProductCategoryRepository;
import by.bsuir.supplier.repository.catalog.ProductRepository;
import by.bsuir.supplier.repository.catalog.UnitRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.BusinessException;

@Service
public class CatalogQueries {
    private final ActorAccess access;
    private final ProductRepository productRepo;
    private final ProductCategoryRepository productCategoryRepo;
    private final UnitRepository unitRepo;
    public CatalogQueries(ActorAccess access, ProductRepository productRepo, ProductCategoryRepository productCategoryRepo, UnitRepository unitRepo) {
        this.access = access;
        this.productRepo = productRepo;
        this.productCategoryRepo = productCategoryRepo;
        this.unitRepo = unitRepo;
    }
    private static Pageable pageRequest(int page, int size) {
        if (page<0 || size<1 || size>100)throw new BusinessException("Недопустимые параметры страницы (size: 1–100)");
        return PageRequest.of(page, size, Sort.by("id"));
    }
    @Transactional(readOnly = true)
    public ProductView getProduct(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER, RoleCode.ADMINISTRATOR);
        return CatalogViewMapper.toView(productRepo.findById(id).orElseThrow(()->new BusinessException("Объект не найден")));
    }
    @Transactional(readOnly = true)
    public PageResponse<ProductView> listProduct(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER, RoleCode.ADMINISTRATOR);
        Page<Product> result = productRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(CatalogViewMapper::toView));
    }
    @Transactional(readOnly = true)
    public ProductCategoryView getProductCategory(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER, RoleCode.ADMINISTRATOR);
        return CatalogViewMapper.toView(productCategoryRepo.findById(id).orElseThrow(()->new BusinessException("Объект не найден")));
    }
    @Transactional(readOnly = true)
    public PageResponse<ProductCategoryView> listProductCategory(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER, RoleCode.ADMINISTRATOR);
        Page<ProductCategory> result = productCategoryRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(CatalogViewMapper::toView));
    }
    @Transactional(readOnly = true)
    public UnitView getUnit(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER, RoleCode.ADMINISTRATOR);
        return CatalogViewMapper.toView(unitRepo.findById(id).orElseThrow(()->new BusinessException("Объект не найден")));
    }
    @Transactional(readOnly = true)
    public PageResponse<UnitView> listUnit(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER, RoleCode.ADMINISTRATOR);
        Page<Unit> result = unitRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(CatalogViewMapper::toView));
    }
}
