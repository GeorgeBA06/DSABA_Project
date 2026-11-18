package by.bsuir.supplier.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

public record BusinessDashboard(
    long suppliers,
    long products,
    long contracts,
    long orders,
    long deliveries,
    long claims,
    long ratings
) {}
