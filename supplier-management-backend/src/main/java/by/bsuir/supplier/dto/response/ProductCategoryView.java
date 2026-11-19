package by.bsuir.supplier.dto.response;

import java.util.UUID;

public record ProductCategoryView(UUID id, String code, String name, Boolean active) {}
