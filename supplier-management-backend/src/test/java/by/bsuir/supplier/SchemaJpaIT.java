package by.bsuir.supplier;

import static org.junit.jupiter.api.Assertions.*;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.catalog.Product;
import by.bsuir.supplier.entity.enums.UserStatus;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.entity.supplier.SupplierProduct;
import by.bsuir.supplier.entity.supplier.SupplierProductId;
import by.bsuir.supplier.repository.auth.UserRepository;
import by.bsuir.supplier.repository.supplier.SupplierProductRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.List;
import java.util.UUID;

@SpringBootTest(
        properties = {
            "JWT_PRIVATE_KEY_PATH=src/test/resources/jwt-test-private.pem",
            "JWT_PUBLIC_KEY_PATH=src/test/resources/jwt-test-public.pem",
            "BOOTSTRAP_ADMIN_LOGIN=stage4testadmin",
            "BOOTSTRAP_ADMIN_PASSWORD=stage4-strong-test-password-ONLY"
        })
@Testcontainers
class SchemaJpaIT {
    @Container
    static final PostgreSQLContainer<?> pg =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("supplier_management_test")
                    .withUsername("supplier")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void configure(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", pg::getJdbcUrl);
        registry.add("spring.datasource.username", pg::getUsername);
        registry.add("spring.datasource.password", pg::getPassword);
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
    }

    @Autowired JdbcTemplate jdbc;
    @Autowired UserRepository users;
    @Autowired SupplierProductRepository offers;
    @PersistenceContext EntityManager em;

    @Test
    void flywayCreatedExactlyThirtyTablesAndJpaValidatedTheMappings() {
        List<String> names =
                jdbc.queryForList(
                        "SELECT table_name FROM information_schema.tables "
                                + "WHERE table_schema='public' AND table_type='BASE TABLE' "
                                + "AND table_name <> 'flyway_schema_history' ORDER BY table_name",
                        String.class);
        assertEquals(30, names.size());
        for (String required :
                List.of(
                        "supplier_status_requests",
                        "order_closure_lines",
                        "fact_corrections",
                        "rating_line_inputs")) {
            assertTrue(names.contains(required));
        }
    }

    @Test
    void jpaRepositoryPersistsAndFindsTypedUser() {
        User user = new User();
        String login = "user-" + UUID.randomUUID();
        user.setLogin(login);
        user.setPasswordHash("not-a-real-password-hash-used-only-for-mapping-test");
        user.setDisplayName("Тестовый пользователь");
        user.setStatus(UserStatus.ACTIVE);
        user.setFailedLoginCount(0);
        UUID id = users.saveAndFlush(user).getId();
        assertNotNull(id);
        assertEquals(id, users.findByLogin(login).orElseThrow().getId());
    }

    @Test
    void databaseRejectsDuplicatePendingSupplierStatusRequests() {
        UUID creator = UUID.randomUUID(), supplier = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users(id,login,password_hash,display_name,status,failed_login_count,created_at,updated_at,lock_version) "
                        + "VALUES (?, ?, 'hash', 'Requester', 'ACTIVE', 0,now(),now(),0)",
                creator,
                "creator-" + creator);
        jdbc.update(
                "INSERT INTO suppliers(id,name,status,created_by_id,created_at,updated_at,lock_version) "
                        + "VALUES (?,'Test supplier','CANDIDATE',?,now(),now(),0)",
                supplier,
                creator);
        String sql =
                "INSERT INTO supplier_status_requests(supplier_id,from_status,to_status,reason,requested_by_id,requested_at,state) "
                        + "VALUES (?,'CANDIDATE','ACTIVE','Activate',?,now(),'PENDING')";
        jdbc.update(sql, supplier, creator);
        assertThrows(
                org.springframework.dao.DataAccessException.class,
                () -> jdbc.update(sql, supplier, creator));
    }

    @Test
    void ratingCannotBePublishedWithoutFourDeterminedCriteria() {
        UUID creator = UUID.randomUUID(),
                manager = UUID.randomUUID(),
                supplier = UUID.randomUUID(),
                assessment = UUID.randomUUID();
        for (UUID user : List.of(creator, manager)) {
            jdbc.update(
                    "INSERT INTO users(id,login,password_hash,display_name,status,failed_login_count,created_at,updated_at,lock_version) "
                            + "VALUES (?, ?, 'hash', 'Requester', 'ACTIVE', 0,now(),now(),0)",
                    user,
                    "rating-user-" + user);
        }
        jdbc.update(
                "INSERT INTO suppliers(id,name,status,created_by_id,created_at,updated_at,lock_version) "
                        + "VALUES (?,'Rating supplier','ACTIVE',?,now(),now(),0)",
                supplier,
                creator);
        jdbc.update(
                "INSERT INTO expert_assessments(id,supplier_id,period_start,period_end,applicable_from,applicable_to,score,justification,state,created_by_id,created_at,approved_by_id,approved_at) "
                        + "VALUES (?,?,current_date-29,current_date,current_date-29,current_date,75,'Evidence','APPROVED',?,now(),?,now())",
                assessment,
                supplier,
                creator,
                manager);
        String sql =
                "INSERT INTO supplier_ratings(supplier_id,scope,period_start,period_end,as_of_at,method_version, "
                        + "rules_snapshot,expert_assessment_id,state,order_count,line_count,created_at,total_score,band) "
                        + "VALUES (?,'OVERALL',current_date-89,current_date,now(),'v1','{}'::jsonb,?,'PUBLISHED',3,3,now(),90.0,'HIGH')";
        assertThrows(
                org.springframework.dao.DataAccessException.class,
                () -> jdbc.update(sql, supplier, assessment));
    }

    @Test
    void orderCannotUseContractOfAnotherSupplier() {
        UUID creator = UUID.randomUUID(),
                a = UUID.randomUUID(),
                b = UUID.randomUUID(),
                contract = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users(id,login,password_hash,display_name,status,failed_login_count,created_at,updated_at,lock_version) "
                        + "VALUES (?, ?, 'hash', 'Requester', 'ACTIVE', 0,now(),now(),0)",
                creator,
                "crossdoc-" + creator);
        for (UUID supplier : List.of(a, b)) {
            jdbc.update(
                    "INSERT INTO suppliers(id,name,status,created_by_id,created_at,updated_at,lock_version) "
                            + "VALUES (?,'Supplier','CANDIDATE',?,now(),now(),0)",
                    supplier,
                    creator);
        }
        jdbc.update(
                "INSERT INTO contracts(id,supplier_id,state,created_by_id,created_at,lock_version) "
                        + "VALUES (?,?,'DRAFT',?,now(),0)",
                contract,
                a,
                creator);
        assertThrows(
                org.springframework.dao.DataAccessException.class,
                () ->
                        jdbc.update(
                                "INSERT INTO purchase_orders(number,supplier_id,contract_id,state,created_by_id,created_at,updated_at,lock_version) "
                                        + "VALUES (?,?,?,'DRAFT',?,now(),now(),0)",
                                "PO-" + UUID.randomUUID(),
                                b,
                                contract,
                                creator));
    }

    @Test
    void approvedContractVersionCannotBeSilentlyOverwritten() {
        UUID creator = UUID.randomUUID(),
                manager = UUID.randomUUID(),
                supplier = UUID.randomUUID(),
                contract = UUID.randomUUID(),
                version = UUID.randomUUID();
        for (UUID u : List.of(creator, manager)) {
            jdbc.update(
                    "INSERT INTO users(id,login,password_hash,display_name,status,failed_login_count,created_at,updated_at,lock_version) "
                            + "VALUES (?, ?, 'hash', 'User', 'ACTIVE', 0,now(),now(),0)",
                    u,
                    "decision-" + u);
        }
        jdbc.update(
                "INSERT INTO suppliers(id,name,status,created_by_id,created_at,updated_at,lock_version) "
                        + "VALUES (?,'Supplier','CANDIDATE',?,now(),now(),0)",
                supplier,
                creator);
        jdbc.update(
                "INSERT INTO contracts(id,supplier_id,state,created_by_id,created_at,lock_version) "
                        + "VALUES (?,?,'DRAFT',?,now(),0)",
                contract,
                supplier,
                creator);
        jdbc.update(
                "INSERT INTO contract_versions(id,contract_id,version_no,number,valid_from,valid_to,terms,state,created_by_id,created_at,decided_by_id,decided_at) "
                        + "VALUES (?,?,1,'A-001',current_date,current_date+30,'terms','APPROVED',?,now(),?,now())",
                version,
                contract,
                creator,
                manager);
        assertThrows(
                org.springframework.dao.DataAccessException.class,
                () ->
                        jdbc.update(
                                "UPDATE contract_versions SET terms='rewritten' WHERE id=?",
                                version));
    }

    @Test
    @Transactional
    void compositeSupplierProductIdentityIsMappedByJpa() {
        UUID creator = UUID.randomUUID(),
                supplier = UUID.randomUUID(),
                category = UUID.randomUUID(),
                unit = UUID.randomUUID(),
                product = UUID.randomUUID();
        jdbc.update(
                "INSERT INTO users(id,login,password_hash,display_name,status,failed_login_count,created_at,updated_at,lock_version) "
                        + "VALUES (?, ?, 'hash', 'Requester', 'ACTIVE', 0,now(),now(),0)",
                creator,
                "composite-" + creator);
        jdbc.update(
                "INSERT INTO suppliers(id,name,status,created_by_id,created_at,updated_at,lock_version) "
                        + "VALUES (?,'Supplier','CANDIDATE',?,now(),now(),0)",
                supplier,
                creator);
        jdbc.update(
                "INSERT INTO product_categories(id,code,name,active,lock_version) VALUES (?,?, 'Category', true,0)",
                category,
                "CAT-" + category);
        jdbc.update("INSERT INTO units(id,code,name,active) VALUES (?,'KG','Kilogram',true)", unit);
        jdbc.update(
                "INSERT INTO products(id,sku,name,category_id,unit_id,active,created_at,updated_at,lock_version) "
                        + "VALUES (?,?, 'Product',?,?,true,now(),now(),0)",
                product,
                "SKU-" + product,
                category,
                unit);
        SupplierProduct offer = new SupplierProduct();
        offer.setId(new SupplierProductId(supplier, product));
        offer.setSupplier(em.getReference(Supplier.class, supplier));
        offer.setProduct(em.getReference(Product.class, product));
        offer.setActive(true);
        offers.saveAndFlush(offer);
        assertTrue(offers.existsById(new SupplierProductId(supplier, product)));
    }
}
