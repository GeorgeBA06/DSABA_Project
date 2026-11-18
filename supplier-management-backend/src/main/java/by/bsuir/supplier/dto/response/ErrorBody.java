package by.bsuir.supplier.dto.response;

import java.time.Instant;

/** Public error contract, without internal exception details. */
public record ErrorBody(String code, String message, Instant timestamp) {}
