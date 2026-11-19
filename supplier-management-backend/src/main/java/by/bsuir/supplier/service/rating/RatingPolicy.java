package by.bsuir.supplier.service.rating;

import by.bsuir.supplier.entity.enums.RatingBand;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

/**
 * Pure rating rules; deterministic and unit-testable without a database (Strategy: weighted
 * criteria).
 */
public final class RatingPolicy {
    public enum Criterion {
        TIMELINESS,
        COMPLETENESS,
        QUALITY,
        INTERACTION
    }

    private static final Map<Criterion, BigDecimal> WEIGHTS =
            Map.of(
                    Criterion.TIMELINESS,
                    new BigDecimal("0.35"),
                    Criterion.COMPLETENESS,
                    new BigDecimal("0.25"),
                    Criterion.QUALITY,
                    new BigDecimal("0.30"),
                    Criterion.INTERACTION,
                    new BigDecimal("0.10"));

    private RatingPolicy() {}

    public static BigDecimal percent(BigDecimal part, BigDecimal whole) {
        if (whole == null || whole.signum() <= 0) return null;
        BigDecimal p = part.max(BigDecimal.ZERO).min(whole);
        return p.multiply(new BigDecimal("100")).divide(whole, 1, RoundingMode.HALF_UP);
    }

    public static BigDecimal mean(Collection<BigDecimal> values) {
        if (values == null || values.isEmpty()) return null;
        return values.stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .divide(BigDecimal.valueOf(values.size()), 1, RoundingMode.HALF_UP);
    }

    public static BigDecimal weighted(Map<Criterion, BigDecimal> scores) {
        if (scores.size() != 4
                || Arrays.stream(Criterion.values()).anyMatch(k -> scores.get(k) == null))
            return null;
        BigDecimal sum = BigDecimal.ZERO;
        for (Criterion key : Criterion.values())
            sum = sum.add(scores.get(key).multiply(WEIGHTS.get(key)));
        return sum.setScale(1, RoundingMode.HALF_UP);
    }

    public static RatingBand band(BigDecimal score) {
        if (score == null) return null;
        if (score.compareTo(new BigDecimal("85.0")) >= 0) return RatingBand.HIGH;
        if (score.compareTo(new BigDecimal("70.0")) >= 0) return RatingBand.ACCEPTABLE;
        if (score.compareTo(new BigDecimal("50.0")) >= 0) return RatingBand.ATTENTION;
        return RatingBand.LOW;
    }

    public static Map<String, Object> rules() {
        return Map.of(
                "method",
                "WEIGHTED_V1",
                "timeliness",
                0.35,
                "completeness",
                0.25,
                "quality",
                0.30,
                "interaction",
                0.10,
                "windowDays",
                90,
                "minimumOrders",
                3);
    }
}
