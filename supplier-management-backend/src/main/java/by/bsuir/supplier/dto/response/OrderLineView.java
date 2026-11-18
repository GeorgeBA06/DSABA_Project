package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record OrderLineView(
    UUID id,
    UUID orderId,
    Integer lineNo,
    UUID productId
) {}
