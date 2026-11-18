package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record SupplierReturnLineView(
    UUID id,
    UUID supplierReturnId,
    UUID acceptanceLineId,
    BigDecimal quantity,
    String defectDescription,
    UUID claimId
) {}
