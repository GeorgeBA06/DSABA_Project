package by.bsuir.supplier.service.catalog;

import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.catalog.Product;
import by.bsuir.supplier.entity.catalog.ProductCategory;
import by.bsuir.supplier.entity.catalog.Unit;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.repository.catalog.*;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;

@Service
public class CatalogService {
    private final ActorAccess access;
    private final ProductCategoryRepository categories;
    private final UnitRepository units;
    private final ProductRepository products;
    private final AuditTrail audit;
    public CatalogService(ActorAccess access, ProductCategoryRepository categories, UnitRepository units,
        ProductRepository products, AuditTrail audit) {
        this.access = access;
        this.categories = categories;
        this.units = units;
        this.products = products;
        this.audit = audit;
    }
    @Transactional
    public ProductCategory createCategory(UUID actorId, String code, String name) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_MANAGER);
        ProductCategory category = new ProductCategory();
        category.setCode(Rules.text(code,"Код категории"));
        category.setName(Rules.text(name,"Название категории"));
        category.setActive(true);
        category = categories.saveAndFlush(category);
        audit.record(actor,"CATEGORY_CREATED", category,"ProductCategory", category.getId(), null);
        return category;
    }
    @Transactional
    public Unit createUnit(UUID actorId, String code, String name) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_MANAGER);
        Unit unit = new Unit();
        unit.setCode(Rules.text(code,"Код единицы"));
        unit.setName(Rules.text(name,"Единица измерения"));
        unit.setActive(true);
        unit = units.saveAndFlush(unit);
        audit.record(actor,"UNIT_CREATED", unit,"Unit", unit.getId(), null);
        return unit;
    }
    @Transactional
    public Product createProduct(UUID actorId, String sku, String name, UUID categoryId, UUID unitId) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        ProductCategory category = categories.findById(Rules.present(categoryId,"Категория")).orElseThrow(()->new BusinessException("Категория не найдена"));
        Unit unit = units.findById(Rules.present(unitId,"Единица")).orElseThrow(()->new BusinessException("Единица не найдена"));
        Rules.require(Boolean.TRUE.equals(category.getActive()) && Boolean.TRUE.equals(unit.getActive()),"Выберите активные справочники");
        Product item = new Product();
        item.setSku(Rules.text(sku,"Артикул"));
        item.setName(Rules.text(name,"Товар"));
        item.setCategory(category);
        item.setUnit(unit);
        item.setActive(true);
        item = products.saveAndFlush(item);
        audit.record(actor,"PRODUCT_CREATED", item,"Product", item.getId(), null);
        return item;
    }
    @Transactional
    public Product changeActive(UUID actorId, UUID productId, boolean active) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_MANAGER);
        Product product = products.findById(productId).orElseThrow(()->new BusinessException("Товар не найден"));
        product.setActive(active);
        audit.record(actor,"PRODUCT_ACTIVITY_CHANGED", product,"Product", product.getId(), Boolean.toString(active));
        return product;
    }
}
