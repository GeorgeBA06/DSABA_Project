package by.bsuir.supplier.service;

import static org.junit.jupiter.api.Assertions.*;

import by.bsuir.supplier.entity.enums.RatingBand;
import by.bsuir.supplier.service.rating.RatingPolicy;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Map;

class RatingPolicyTest {
    private static BigDecimal d(String v) {
        return new BigDecimal(v);
    }

    @Test
    void weightedScoresAndThresholds() {
        BigDecimal total =
                RatingPolicy.weighted(
                        Map.of(
                                RatingPolicy.Criterion.TIMELINESS,
                                d("100.0"),
                                RatingPolicy.Criterion.COMPLETENESS,
                                d("80.0"),
                                RatingPolicy.Criterion.QUALITY,
                                d("90.0"),
                                RatingPolicy.Criterion.INTERACTION,
                                d("75.0")));
        assertEquals(d("89.5"), total);
        assertEquals(RatingBand.HIGH, RatingPolicy.band(total));
        assertEquals(RatingBand.ACCEPTABLE, RatingPolicy.band(d("84.9")));
        assertEquals(RatingBand.ATTENTION, RatingPolicy.band(d("69.9")));
        assertEquals(RatingBand.LOW, RatingPolicy.band(d("49.9")));
    }

    @Test
    void missingMetricDoesNotBecomeZero() {
        assertEquals(null, RatingPolicy.percent(d("0"), d("0")));
        assertEquals(
                null, RatingPolicy.weighted(Map.of(RatingPolicy.Criterion.TIMELINESS, d("100"))));
        assertEquals(d("100.0"), RatingPolicy.percent(d("200"), d("100")));
        assertEquals(d("33.3"), RatingPolicy.percent(d("1"), d("3")));
    }
}
