import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.entity.fulfillment.*;
import by.bsuir.supplier.entity.order.*;
import by.bsuir.supplier.service.fulfillment.FulfillmentLedger;
import by.bsuir.supplier.service.rating.RatingPolicy;
import by.bsuir.supplier.service.rating.CriterionStrategies;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Run without Spring/Hibernate/PostgreSQL to validate pure domain arithmetic. */
public class PurePolicyChecks {
    private static BigDecimal d(String s){return new BigDecimal(s);}
    private static void eq(Object a,Object b){if(!Objects.equals(a,b))throw new AssertionError("expected="+b+", got="+a);}
    public static void main(String[] args){
        eq(RatingPolicy.percent(d("1"),d("3")),d("33.3"));
        eq(RatingPolicy.percent(d("200"),d("100")),d("100.0"));
        eq(RatingPolicy.percent(BigDecimal.ZERO,BigDecimal.ZERO),null);
        eq(RatingPolicy.weighted(Map.of(
           RatingPolicy.Criterion.TIMELINESS,d("100.0"),RatingPolicy.Criterion.COMPLETENESS,d("80.0"),
           RatingPolicy.Criterion.QUALITY,d("90.0"),RatingPolicy.Criterion.INTERACTION,d("75.0"))),d("89.5"));
        eq(CriterionStrategies.evaluate(RatingPolicy.Criterion.TIMELINESS,
            new CriterionStrategies.Facts(d("100"),d("85"),d("20"),d("70"),d("65"),75)),d("85.0"));
        eq(CriterionStrategies.evaluate(RatingPolicy.Criterion.INTERACTION,
            new CriterionStrategies.Facts(null,null,null,null,null,75)),d("75.0"));
        eq(RatingPolicy.band(d("85.0")),RatingBand.HIGH);
        eq(RatingPolicy.band(d("84.9")),RatingBand.ACCEPTABLE);
        eq(RatingPolicy.band(d("69.9")),RatingBand.ATTENTION);
        eq(RatingPolicy.band(d("49.9")),RatingBand.LOW);
        UUID lineId=UUID.randomUUID();
        OrderLine orderLine=new OrderLine();orderLine.setId(lineId);
        OrderVersionLine segment=new OrderVersionLine();segment.setSegmentKey("SEG-A");segment.setOrderLine(orderLine);
        Delivery arrival=new Delivery();arrival.setId(UUID.randomUUID());arrival.setArrivedOn(LocalDate.now());
        DeliveryLine received=new DeliveryLine();received.setId(UUID.randomUUID());received.setDelivery(arrival);
        received.setOrderVersionLine(segment);received.setQuantity(d("100"));
        AcceptanceDocument acceptance=new AcceptanceDocument();acceptance.setId(UUID.randomUUID());acceptance.setInspectedOn(LocalDate.now());
        AcceptanceLine inspected=new AcceptanceLine();inspected.setId(UUID.randomUUID());inspected.setAcceptanceDocument(acceptance);
        inspected.setDeliveryLine(received);inspected.setAcceptedQty(d("90"));
        inspected.setQualityRejectedQty(d("10"));inspected.setOtherRejectedQty(d("0"));
        SupplierReturn returnedDoc=new SupplierReturn();returnedDoc.setReturnedOn(LocalDate.now());
        SupplierReturnLine returned=new SupplierReturnLine();returned.setAcceptanceLine(inspected);returned.setSupplierReturn(returnedDoc);
        returned.setQuantity(d("5"));
        FulfillmentLedger.Snapshot snap=new FulfillmentLedger.Snapshot(List.of(received),List.of(inspected),List.of(returned));
        eq(snap.delivered(received.getId()),d("100"));
        eq(snap.inspected(received.getId()),d("100"));
        eq(snap.fulfilled(lineId),d("85"));
        eq(snap.netBySegment("SEG-A"),d("85"));
        eq(snap.deliveredForSegment("SEG-A",LocalDate.now()),d("100"));
        eq(RatingPolicy.percent(d("85"),d("100")),d("85.0"));
        System.out.println("PASS: 16 executable business arithmetic and ledger checks (no database)");
    }
}
