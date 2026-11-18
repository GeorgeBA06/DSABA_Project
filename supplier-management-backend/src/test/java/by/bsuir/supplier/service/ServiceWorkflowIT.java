package by.bsuir.supplier.service;
import by.bsuir.supplier.entity.auth.*;
import by.bsuir.supplier.entity.catalog.*;
import by.bsuir.supplier.entity.contract.*;
import by.bsuir.supplier.entity.enums.*;
import by.bsuir.supplier.entity.fulfillment.*;
import by.bsuir.supplier.entity.order.*;
import by.bsuir.supplier.entity.supplier.*;
import by.bsuir.supplier.entity.rating.*;
import by.bsuir.supplier.repository.auth.*;
import by.bsuir.supplier.repository.contract.*;
import by.bsuir.supplier.repository.order.*;
import by.bsuir.supplier.repository.fulfillment.*;
import by.bsuir.supplier.repository.rating.*;
import by.bsuir.supplier.repository.supplier.*;
import by.bsuir.supplier.service.catalog.CatalogService;
import by.bsuir.supplier.service.supplier.SupplierService;
import by.bsuir.supplier.service.contract.ContractService;
import by.bsuir.supplier.service.order.*;
import by.bsuir.supplier.service.fulfillment.*;
import by.bsuir.supplier.service.rating.*;
import by.bsuir.supplier.service.common.BusinessException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import static org.junit.jupiter.api.Assertions.*;
@SpringBootTest(properties={
  "JWT_PRIVATE_KEY_PATH=src/test/resources/jwt-test-private.pem",
  "JWT_PUBLIC_KEY_PATH=src/test/resources/jwt-test-public.pem",
  "BOOTSTRAP_ADMIN_LOGIN=stage4testadmin",
  "BOOTSTRAP_ADMIN_PASSWORD=stage4-strong-test-password-ONLY"
})
@Testcontainers
class ServiceWorkflowIT {
    @Container static final PostgreSQLContainer<?> pg=new PostgreSQLContainer<>("postgres:16-alpine")
        .withDatabaseName("supplier_service_test").withUsername("supplier").withPassword("test_password");
    @DynamicPropertySource static void db(DynamicPropertyRegistry registry){
        registry.add("spring.datasource.url",pg::getJdbcUrl);registry.add("spring.datasource.username",pg::getUsername);
        registry.add("spring.datasource.password",pg::getPassword);
    }
    @Autowired UserRepository users;
    @Autowired UserRoleAssignmentRepository roles;
    @Autowired CatalogService catalog;
    @Autowired SupplierService suppliers;
    @Autowired ContractService contracts;
    @Autowired OrderService orders;
    @Autowired ReceivingService receiving;
    @Autowired ReturnService returns;
    @Autowired OrderClosureService closures;
    @Autowired ExpertAssessmentService experts;
    @Autowired RatingService ratings;
    @Autowired ContractVersionRepository contractVersions;
    @Autowired OrderVersionRepository orderVersions;
    @Autowired OrderVersionLineRepository segments;
    @Autowired DeliveryLineRepository deliveryLines;
    @Autowired AcceptanceLineRepository acceptanceLines;
    @Autowired SupplierReturnLineRepository returnLines;
    @Autowired SupplierRatingRepository ratingRepo;
    @Autowired SupplierProductRepository supplierProducts;
    @Autowired CorrectionService corrections;
    @Autowired FulfillmentLedger ledger;
    @Autowired ExpertAssessmentRepository expertRepository;
    record Fixture(UUID specialist,UUID manager,UUID supplier,UUID product,UUID contract,UUID order,UUID segment){}
    private User user(String prefix){
        User u=new User();u.setLogin(prefix+UUID.randomUUID());u.setDisplayName(prefix);u.setPasswordHash("$2a$10$not-used-for-identity-service-testing");
        u.setStatus(UserStatus.ACTIVE);u.setFailedLoginCount(0);return users.saveAndFlush(u);
    }
    private void role(User u,RoleCode code,User admin){
        UserRoleAssignment r=new UserRoleAssignment();r.setUser(u);r.setRoleCode(code);r.setAssignedBy(admin);
        r.setAssignedAt(Instant.now());roles.saveAndFlush(r);
    }
    private Fixture createFixture(BigDecimal quantity){
        User author=user("buyer-");User manager=user("manager-");role(author,RoleCode.PROCUREMENT_SPECIALIST,manager);role(manager,RoleCode.PROCUREMENT_MANAGER,manager);
        ProductCategory c=catalog.createCategory(manager.getId(),"C"+UUID.randomUUID(),"Категория");
        Unit unit=catalog.createUnit(manager.getId(),"U"+UUID.randomUUID().toString().substring(0,8),"Штуки");
        Product product=catalog.createProduct(author.getId(),"P"+UUID.randomUUID(),"Тестовый товар",c.getId(),unit.getId());
        Supplier s=suppliers.create(author.getId(),new SupplierService.Details("Поставщик", "Контакт", null,null,null,null));
        suppliers.offerProduct(author.getId(),s.getId(),product.getId(),null);
        var status=suppliers.requestStatus(author.getId(),s.getId(),SupplierStatus.ACTIVE,"Начало сотрудничества");
        suppliers.decideStatus(manager.getId(),status.getId(),true,"Согласовано");
        Contract con=contracts.create(author.getId(),s.getId(),new ContractService.Terms(
            "C-"+UUID.randomUUID(),LocalDate.now(),LocalDate.now().minusDays(1),LocalDate.now().plusDays(180),"Условия"));
        ContractVersion cv=contractVersions.findByContract_Id(con.getId()).get(0);
        contracts.submit(author.getId(),cv.getId());contracts.decide(manager.getId(),cv.getId(),true,"Зарегистрирован");
        PurchaseOrder order=orders.create(author.getId(),"O-"+UUID.randomUUID(),s.getId(),con.getId(),
            List.of(new OrderService.Item(product.getId(),quantity,new BigDecimal("10.00"),LocalDate.now())));
        OrderVersion v=orderVersions.findByOrder_Id(order.getId()).get(0);
        orders.submit(author.getId(),v.getId());orders.decide(manager.getId(),v.getId(),true,"Согласовано");
        UUID segment=segments.findByOrderVersion_Id(v.getId()).get(0).getId();
        return new Fixture(author.getId(),manager.getId(),s.getId(),product.getId(),con.getId(),order.getId(),segment);
    }
    @Test void supplierContractOrderReceiptInspectionAndReturn(){
        Fixture f=createFixture(new BigDecimal("10.000"));
        Delivery d=receiving.registerDelivery(f.specialist(),f.order(),LocalDate.now(),"D-1",
            List.of(new ReceivingService.ReceivedItem(f.segment(),new BigDecimal("10.000"))));
        receiving.confirmDelivery(f.specialist(),d.getId());
        UUID deliveredLine=deliveryLines.findByDelivery_Id(d.getId()).get(0).getId();
        AcceptanceDocument a=receiving.registerInspection(f.specialist(),d.getId(),LocalDate.now(),List.of(
            new ReceivingService.InspectionItem(deliveredLine,new BigDecimal("8"),new BigDecimal("2"),BigDecimal.ZERO,null)));
        receiving.confirmInspection(f.specialist(),a.getId());
        UUID acceptedLine=acceptanceLines.findByAcceptanceDocument_Id(a.getId()).get(0).getId();
        SupplierReturn r=returns.create(f.specialist(),f.order(),LocalDate.now(),"Выявлен дефект",List.of(
            new ReturnService.ReturnItem(acceptedLine,new BigDecimal("3"),"Дефект упаковки")));
        returns.submit(f.specialist(),r.getId());returns.decide(f.manager(),r.getId(),true);
        SupplierReturn tooMuch=returns.create(f.specialist(),f.order(),LocalDate.now(),"Повторный дефект",List.of(
            new ReturnService.ReturnItem(acceptedLine,new BigDecimal("6"),"Недостаток")));
        returns.submit(f.specialist(),tooMuch.getId());
        assertThrows(BusinessException.class,()->returns.decide(f.manager(),tooMuch.getId(),true));
        returns.decide(f.manager(),tooMuch.getId(),false);
        OrderClosure closed=closures.close(f.manager(),f.order(),"Остаток не поставлен");
        assertEquals(ClosureResult.CLOSED_WITH_REMAINDER,closed.getResult());
    }
    @Test void ratingRemainsPreliminaryWhenThereAreTooFewOrders(){
        Fixture f=createFixture(new BigDecimal("5"));
        SupplierRating rating=ratings.calculate(f.manager(),f.supplier(),RatingScope.OVERALL,null);
        assertEquals(RatingState.PRELIMINARY,rating.getState());assertEquals(null,rating.getTotalScore());
        assertEquals(1,rating.getOrderCount());
    }
    @Test void ratingPublishesAfterThreeFullyInspectedOrdersAndIndependentExpertApproval(){
        Fixture f=createFixture(new BigDecimal("10"));
        LocalDate today=LocalDate.now(ZoneId.of("Europe/Minsk"));
        ExpertAssessment e=experts.create(f.specialist(),f.supplier(),today.minusDays(29),today,
            today.minusDays(29),today,75,"Установленная дисциплина общения");
        experts.submit(f.specialist(),e.getId());experts.decide(f.manager(),e.getId(),true);
        for(int i=0;i<3;i++){
            UUID orderId;UUID segment;
            if(i==0){orderId=f.order();segment=f.segment();}
            else {
                PurchaseOrder o=orders.create(f.specialist(),"R-"+UUID.randomUUID(),f.supplier(),f.contract(),
                    List.of(new OrderService.Item(f.product(),new BigDecimal("10"),new BigDecimal("1.00"),today)));
                OrderVersion v=orderVersions.findByOrder_Id(o.getId()).get(0);
                orders.submit(f.specialist(),v.getId());orders.decide(f.manager(),v.getId(),true,"Согласовано");
                orderId=o.getId();segment=segments.findByOrderVersion_Id(v.getId()).get(0).getId();
            }
            Delivery d=receiving.registerDelivery(f.specialist(),orderId,today,"P-"+i,
                List.of(new ReceivingService.ReceivedItem(segment,new BigDecimal("10"))));
            receiving.confirmDelivery(f.specialist(),d.getId());
            UUID dl=deliveryLines.findByDelivery_Id(d.getId()).get(0).getId();
            AcceptanceDocument a=receiving.registerInspection(f.specialist(),d.getId(),today,
                List.of(new ReceivingService.InspectionItem(dl,new BigDecimal("10"),BigDecimal.ZERO,BigDecimal.ZERO,null)));
            receiving.confirmInspection(f.specialist(),a.getId());
        }
        SupplierRating r=ratings.calculate(f.manager(),f.supplier(),RatingScope.OVERALL,null);
        assertEquals(RatingState.PUBLISHED,r.getState());
        assertEquals(new BigDecimal("97.5"),r.getTotalScore());
        assertEquals(3,r.getOrderCount());assertEquals(RatingBand.HIGH,r.getBand());
    }
    @Test void receiptCorrectionReplacesOriginalWithoutDoubleCounting(){
        Fixture f=createFixture(new BigDecimal("10"));
        LocalDate today=LocalDate.now(ZoneId.of("Europe/Minsk"));
        Delivery d=receiving.registerDelivery(f.specialist(),f.order(),today,"WRONG",
            List.of(new ReceivingService.ReceivedItem(f.segment(),new BigDecimal("10"))));
        receiving.confirmDelivery(f.specialist(),d.getId());
        FactCorrection c=corrections.proposeReceipt(f.specialist(),d.getId(),today,"Ошибочно внесённое количество",
            List.of(new CorrectionService.ReceiptFix(f.segment(),new BigDecimal("9"))));
        corrections.submit(f.specialist(),c.getId());corrections.decide(f.manager(),c.getId(),true,"Исправлено");
        assertEquals(new BigDecimal("9"),ledger.snapshot(f.order()).deliveredForSegment(
            segments.findByOrderVersion_Id(orderVersions.findByOrder_Id(f.order()).get(0).getId()).get(0).getSegmentKey(),null));
    }
    @Test void rejectedFirstOrderIsResubmittedAsAnImmutableRevision(){
        Fixture f=createFixture(new BigDecimal("10"));
        PurchaseOrder another=orders.create(f.specialist(),"REJ-"+UUID.randomUUID(),f.supplier(),f.contract(),
            List.of(new OrderService.Item(f.product(),new BigDecimal("8"),new BigDecimal("3.00"),LocalDate.now())));
        OrderVersion initial=orderVersions.findByOrder_Id(another.getId()).get(0);
        orders.submit(f.specialist(),initial.getId());
        orders.decide(f.manager(),initial.getId(),false,"Пересмотреть количество");
        OrderVersion revised=orders.reviseRejected(f.specialist(),another.getId(),List.of(
            new OrderService.Item(f.product(),new BigDecimal("9"),new BigDecimal("3.00"),LocalDate.now())),"Исправлен черновик");
        assertEquals(VersionState.REJECTED,orderVersions.findById(initial.getId()).orElseThrow().getState());
        orders.submit(f.specialist(),revised.getId());orders.decide(f.manager(),revised.getId(),true,"Согласовано");
        assertEquals(VersionState.APPROVED,orderVersions.findById(revised.getId()).orElseThrow().getState());
    }
    @Test void supplierRescheduleKeepsRatingDeadline(){
        Fixture f=createFixture(new BigDecimal("10"));
        OrderVersion before=orderVersions.findByOrder_Id(f.order()).stream().filter(v->v.getState()==VersionState.APPROVED).findFirst().orElseThrow();
        OrderVersion change=orders.proposeSupplierReschedule(f.specialist(),f.order(),
            segments.findByOrderVersion_Id(before.getId()).get(0).getSegmentKey(),LocalDate.now().plusDays(5),"Перенос поставщиком");
        assertEquals(VersionState.DRAFT,change.getState());orders.submit(f.specialist(),change.getId());
        orders.decide(f.manager(),change.getId(),true,"Согласовано");
        OrderVersionLine revised=segments.findByOrderVersion_Id(change.getId()).get(0);
        assertEquals(LocalDate.now(),revised.getRatingDueOn());
        assertEquals(LocalDate.now().plusDays(5),revised.getExecutionDueOn());
    }
}
