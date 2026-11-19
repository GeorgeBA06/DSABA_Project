import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.service.fulfillment.FulfillmentLedger;
import by.bsuir.supplier.service.rating.CriterionStrategies;
import by.bsuir.supplier.service.rating.RatingPolicy;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Run without Spring/Hibernate/PostgreSQL to validate pure domain arithmetic. */
public class PurePolicyChecks {
    private static BigDecimal d(String s) {
        return new BigDecimal(s);
    }

    private static void eq(Object a, Object b) {
        if (!Objects.equals(a, b)) throw new AssertionError("expected=" + b + ", got=" + a);
    }

    public static void main(String[] args) {
        eq(RatingPolicy.percent(d("1"), d("3")), d("33.3"));
        eq(RatingPolicy.percent(d("200"), d("100")), d("100.0"));
        eq(RatingPolicy.percent(BigDecimal.ZERO, BigDecimal.ZERO), null);
        eq(
                RatingPolicy.weighted(
                        Map.of(
                                RatingPolicy.Criterion.TIMELINESS,
                                d("100.0"),
                                RatingPolicy.Criterion.COMPLETENESS,
                                d("80.0"),
                                RatingPolicy.Criterion.QUALITY,
                                d("90.0"),
                                RatingPolicy.Criterion.INTERACTION,
                                d("75.0"))),
                d("89.5"));
        eq(
                CriterionStrategies.evaluate(
                        RatingPolicy.Criterion.TIMELINESS,
                        new CriterionStrategies.Facts(
                                d("100"), d("85"), d("20"), d("70"), d("65"), 75)),
                d("85.0"));
        eq(
                CriterionStrategies.evaluate(
                        RatingPolicy.Criterion.INTERACTION,
                        new CriterionStrategies.Facts(null, null, null, null, null, 75)),
                d("75.0"));
        eq(RatingPolicy.band(d("85.0")), RatingBand.HIGH);
        eq(RatingPolicy.band(d("84.9")), RatingBand.ACCEPTABLE);
        eq(RatingPolicy.band(d("69.9")), RatingBand.ATTENTION);
        eq(RatingPolicy.band(d("49.9")), RatingBand.LOW);
        UUID lineId = UUID.randomUUID();
        UUID segmentId = UUID.randomUUID();
        UUID deliveryLineId = UUID.randomUUID();
        UUID acceptanceLineId = UUID.randomUUID();
        String segmentKey = "SEG-A";
        LocalDate today = LocalDate.now();
        var received =
                new FulfillmentLedger.DeliveryFact(
                        deliveryLineId, segmentId, lineId, segmentKey, d("100"), today);
        var inspected =
                new FulfillmentLedger.AcceptanceFact(
                        acceptanceLineId,
                        UUID.randomUUID(),
                        deliveryLineId,
                        segmentId,
                        lineId,
                        segmentKey,
                        d("90"),
                        d("10"),
                        BigDecimal.ZERO,
                        today);
        var returned =
                new FulfillmentLedger.ReturnFact(
                        UUID.randomUUID(),
                        acceptanceLineId,
                        deliveryLineId,
                        segmentId,
                        lineId,
                        segmentKey,
                        d("5"),
                        today);
        var snap =
                new FulfillmentLedger.Snapshot(
                        List.of(received), List.of(inspected), List.of(returned));
        eq(snap.delivered(deliveryLineId), d("100"));
        eq(snap.inspected(deliveryLineId), d("100"));
        eq(snap.fulfilled(lineId), d("85"));
        eq(snap.netBySegment(segmentKey), d("85"));
        eq(snap.deliveredForSegment(segmentKey, today), d("100"));
        eq(RatingPolicy.percent(d("85"), d("100")), d("85.0"));
        System.out.println(
                "PASS: 16 executable business arithmetic and ledger checks (no database)");
    }
}
