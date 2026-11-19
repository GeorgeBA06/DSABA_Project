package by.bsuir.supplier.dto.response;

import java.util.UUID;

public record SupplierOfferView(
        UUID supplierId,
        UUID productId,
        String productName,
        String supplierItemCode,
        Boolean active) {}
