package by.bsuir.supplier.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.request.ActivityRequest;
import by.bsuir.supplier.dto.request.CategoryRequest;
import by.bsuir.supplier.dto.request.ProductRequest;
import by.bsuir.supplier.dto.request.UnitRequest;
import by.bsuir.supplier.dto.response.ProductCategoryView;
import by.bsuir.supplier.dto.response.ProductView;
import by.bsuir.supplier.dto.response.UnitView;
import by.bsuir.supplier.mapper.CatalogViewMapper;
import by.bsuir.supplier.service.catalog.CatalogService;
import by.bsuir.supplier.service.query.CatalogQueries;

@RestController
@RequestMapping("/api/v1/catalog")
public class CatalogController {
    private final CatalogService service;
    private final CatalogQueries read;
    public CatalogController(CatalogService service, CatalogQueries read) {
        this.service = service;
        this.read = read;
    }
    @GetMapping("/products")
    public PageResponse<ProductView> products(
        Authentication a,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "25") int size
    ) {
        return read.listProduct(ApiActor.id(a), page, size);
    }
    @GetMapping("/products/{id}")
    public ProductView product(Authentication a, @PathVariable UUID id) {
        return read.getProduct(ApiActor.id(a), id);
    }
    @GetMapping("/categories")
    public PageResponse<ProductCategoryView> categories(
        Authentication a,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return read.listProductCategory(ApiActor.id(a), page, size);
    }
    @GetMapping("/units")
    public PageResponse<UnitView> units(
        Authentication a,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        return read.listUnit(ApiActor.id(a), page, size);
    }
    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView createProduct(Authentication a, @Valid @RequestBody ProductRequest d) {
        return CatalogViewMapper.toView(service.createProduct(ApiActor.id(a), d.sku(), d.name(), d.categoryId(), d.unitId()));
    }
    @PostMapping("/categories")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductCategoryView category(Authentication a, @Valid @RequestBody CategoryRequest d) {
        return CatalogViewMapper.toView(service.createCategory(ApiActor.id(a), d.code(), d.name()));
    }
    @PostMapping("/units")
    @ResponseStatus(HttpStatus.CREATED)
    public UnitView unit(Authentication a, @Valid @RequestBody UnitRequest d) {
        return CatalogViewMapper.toView(service.createUnit(ApiActor.id(a), d.code(), d.name()));
    }
    @PatchMapping("/products/{id}/active")
    public ProductView active(Authentication a, @PathVariable UUID id, @Valid @RequestBody ActivityRequest d) {
        return CatalogViewMapper.toView(service.changeActive(ApiActor.id(a), id, d.active()));
    }
}
