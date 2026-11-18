package by.bsuir.supplier.mapper;

import by.bsuir.supplier.dto.response.ProductCategoryView;
import by.bsuir.supplier.dto.response.ProductView;
import by.bsuir.supplier.dto.response.UnitView;
import by.bsuir.supplier.entity.catalog.Product;
import by.bsuir.supplier.entity.catalog.ProductCategory;
import by.bsuir.supplier.entity.catalog.Unit;

/** Maps persistence entities to immutable public response DTOs. */
public final class CatalogViewMapper {
    private CatalogViewMapper() {
    }
    public static ProductView toView(Product e) {
        return new ProductView(
            e.getId(),
            e.getSku(),
            e.getName(),
            e.getCategory() == null?null:e.getCategory().getId(),
            e.getUnit() == null?null:e.getUnit().getId(),
            e.getActive(),
            e.getCreatedAt(),
            e.getUpdatedAt()
        );
    }
    public static UnitView toView(Unit e) {
        return new UnitView(
            e.getId(),
            e.getCode(),
            e.getName(),
            e.getActive()
        );
    }
    public static ProductCategoryView toView(ProductCategory e) {
        return new ProductCategoryView(
            e.getId(),
            e.getCode(),
            e.getName(),
            e.getActive()
        );
    }
}
