package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierOfferView(
    UUID supplierId,
    UUID productId,
    String productName,
    String supplierItemCode,
    Boolean active
) {}
