package by.bsuir.supplier.dto.response;

public record BusinessDashboard(
        long suppliers,
        long products,
        long contracts,
        long orders,
        long deliveries,
        long claims,
        long ratings) {}
