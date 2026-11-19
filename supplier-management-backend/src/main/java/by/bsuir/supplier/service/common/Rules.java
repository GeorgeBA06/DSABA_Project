package by.bsuir.supplier.service.common;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collection;

public final class Rules {
    private Rules() {}

    public static void require(boolean condition, String message) {
        if (!condition) throw new BusinessException(message);
    }

    public static String text(String value, String name) {
        require(value != null && !value.isBlank(), name + " обязателен");
        return value.trim();
    }

    public static <T> T present(T value, String name) {
        require(value != null, name + " обязателен");
        return value;
    }

    public static BigDecimal positive(BigDecimal value, String name) {
        require(value != null && value.signum() > 0, name + " должно быть положительным");
        return value;
    }

    public static BigDecimal nonnegative(BigDecimal value, String name) {
        require(value != null && value.signum() >= 0, name + " не может быть отрицательным");
        return value;
    }

    public static void dates(LocalDate from, LocalDate to) {
        present(from, "Начало периода");
        present(to, "Конец периода");
        require(!from.isAfter(to), "Начало периода позже конца");
    }

    public static void notEmpty(Collection<?> values, String name) {
        require(values != null && !values.isEmpty(), name + " не может быть пустым");
    }
}
