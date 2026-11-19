package by.bsuir.supplier;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.net.URI;
import java.net.http.*;
import java.time.LocalDate;
import java.util.*;

/** Real HTTP -> JWT -> Controller -> Service -> JPA -> PostgreSQL smoke test. Requires Docker. */
@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = {
            "JWT_PRIVATE_KEY_PATH=src/test/resources/jwt-test-private.pem",
            "JWT_PUBLIC_KEY_PATH=src/test/resources/jwt-test-public.pem",
            "BOOTSTRAP_ADMIN_LOGIN=stage4testadmin",
            "BOOTSTRAP_ADMIN_PASSWORD=stage4-strong-test-password-ONLY"
        })
@Testcontainers
class ApiSecurityWorkflowIT {
    private static final String JSON = "application/json";
    private static final String XLSX =
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";

    @Container
    static final PostgreSQLContainer<?> pg =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("supplier_api_test")
                    .withUsername("supplier")
                    .withPassword("test_password");

    @DynamicPropertySource
    static void db(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", pg::getJdbcUrl);
        r.add("spring.datasource.username", pg::getUsername);
        r.add("spring.datasource.password", pg::getPassword);
    }

    @LocalServerPort int port;
    @Autowired ObjectMapper json;
    final HttpClient client = HttpClient.newHttpClient();

    record Result(int status, JsonNode body, byte[] bytes) {}

    Result call(String method, String path, Object request, String bearer) throws Exception {
        return call(method, path, request, bearer, JSON);
    }

    Result call(String method, String path, Object request, String bearer, String accept)
            throws Exception {
        HttpRequest.Builder b =
                HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                        .header("Accept", accept);
        if (bearer != null) b.header("Authorization", "Bearer " + bearer);
        if (request != null) b.header("Content-Type", JSON);
        byte[] payload = request == null ? new byte[0] : json.writeValueAsBytes(request);
        b.method(method, HttpRequest.BodyPublishers.ofByteArray(payload));
        HttpResponse<byte[]> resp = client.send(b.build(), HttpResponse.BodyHandlers.ofByteArray());
        JsonNode body = null;
        if (resp.headers().firstValue("Content-Type").orElse("").contains("json"))
            body = json.readTree(resp.body());
        return new Result(resp.statusCode(), body, resp.body());
    }

    String value(Result r, String field) {
        return r.body().get(field).asText();
    }

    String token(String login, String password) throws Exception {
        Result r =
                call(
                        "POST",
                        "/api/v1/auth/login",
                        Map.of("login", login, "password", password),
                        null);
        assertEquals(200, r.status());
        return value(r, "accessToken");
    }

    String createUser(String admin, String role) throws Exception {
        String login =
                "test-"
                        + role.toLowerCase(Locale.ROOT)
                        + "-"
                        + UUID.randomUUID().toString().substring(0, 10);
        Result r =
                call(
                        "POST",
                        "/api/v1/admin/users",
                        Map.of(
                                "login",
                                login,
                                "displayName",
                                role,
                                "password",
                                "strong-test-password-12345",
                                "roles",
                                List.of(role)),
                        admin);
        assertEquals(201, r.status());
        return login;
    }

    @Test
    void endToEndAuthenticatedProcurementAndReplayProtection() throws Exception {
        assertEquals(401, call("GET", "/api/v1/suppliers", null, null).status());
        String admin = token("stage4testadmin", "stage4-strong-test-password-ONLY");
        assertEquals(
                403,
                call("POST", "/api/v1/suppliers", Map.of("name", "Недоступно"), admin).status());
        String managerLogin = createUser(admin, "PROCUREMENT_MANAGER");
        String specialistLogin = createUser(admin, "PROCUREMENT_SPECIALIST");
        String manager = token(managerLogin, "strong-test-password-12345");
        String specialist = token(specialistLogin, "strong-test-password-12345");
        assertEquals(403, call("GET", "/api/v1/admin/users", null, specialist).status());
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Result cat =
                call(
                        "POST",
                        "/api/v1/catalog/categories",
                        Map.of("code", "C" + suffix, "name", "Категория"),
                        manager);
        assertEquals(201, cat.status());
        Result unit =
                call(
                        "POST",
                        "/api/v1/catalog/units",
                        Map.of("code", "U" + suffix, "name", "Штуки"),
                        manager);
        assertEquals(201, unit.status());
        Result product =
                call(
                        "POST",
                        "/api/v1/catalog/products",
                        Map.of(
                                "sku",
                                "P" + suffix,
                                "name",
                                "Товар",
                                "categoryId",
                                value(cat, "id"),
                                "unitId",
                                value(unit, "id")),
                        specialist);
        assertEquals(201, product.status());
        Result supplier =
                call(
                        "POST",
                        "/api/v1/suppliers",
                        Map.of("name", "Поставщик " + suffix),
                        specialist);
        assertEquals(201, supplier.status());
        String supplierId = value(supplier, "id");
        assertEquals(
                200,
                call(
                                "POST",
                                "/api/v1/suppliers/" + supplierId + "/products",
                                Map.of("productId", value(product, "id")),
                                specialist)
                        .status());
        Result status =
                call(
                        "POST",
                        "/api/v1/suppliers/" + supplierId + "/status-requests",
                        Map.of("status", "ACTIVE", "reason", "Начало сотрудничества"),
                        specialist);
        assertEquals(201, status.status());
        assertEquals(
                200,
                call(
                                "POST",
                                "/api/v1/suppliers/status-requests/"
                                        + value(status, "id")
                                        + "/decision",
                                Map.of("approve", true, "comment", "Согласовано"),
                                manager)
                        .status());
        LocalDate today = LocalDate.now();
        Result contract =
                call(
                        "POST",
                        "/api/v1/contracts",
                        Map.of(
                                "supplierId",
                                supplierId,
                                "terms",
                                Map.of(
                                        "number",
                                        "C-" + suffix,
                                        "validFrom",
                                        today.minusDays(1).toString(),
                                        "validTo",
                                        today.plusDays(100).toString(),
                                        "terms",
                                        "Поставка товара")),
                        specialist);
        assertEquals(201, contract.status());
        String contractId = value(contract, "id");
        Result versions =
                call("GET", "/api/v1/contracts/" + contractId + "/versions", null, specialist);
        String contractVersionId = versions.body().get(0).get("id").asText();
        assertEquals(
                200,
                call(
                                "POST",
                                "/api/v1/contracts/versions/" + contractVersionId + "/submit",
                                null,
                                specialist)
                        .status());
        assertEquals(
                200,
                call(
                                "POST",
                                "/api/v1/contracts/versions/" + contractVersionId + "/decision",
                                Map.of("approve", true, "comment", "Зарегистрировано"),
                                manager)
                        .status());
        Result order =
                call(
                        "POST",
                        "/api/v1/orders",
                        Map.of(
                                "number",
                                "O-" + suffix,
                                "supplierId",
                                supplierId,
                                "contractId",
                                contractId,
                                "items",
                                List.of(
                                        Map.of(
                                                "productId",
                                                value(product, "id"),
                                                "quantity",
                                                10,
                                                "unitPrice",
                                                15,
                                                "dueOn",
                                                today.toString()))),
                        specialist);
        assertEquals(201, order.status());
        String orderId = value(order, "id");
        Result orderVersions =
                call("GET", "/api/v1/orders/" + orderId + "/versions", null, specialist);
        String orderVersionId = orderVersions.body().get(0).get("id").asText();
        assertEquals(
                200,
                call(
                                "POST",
                                "/api/v1/orders/versions/" + orderVersionId + "/submit",
                                null,
                                specialist)
                        .status());
        assertEquals(
                200,
                call(
                                "POST",
                                "/api/v1/orders/versions/" + orderVersionId + "/decision",
                                Map.of("approve", true, "comment", "Одобрено"),
                                manager)
                        .status());
        String segmentId =
                call(
                                "GET",
                                "/api/v1/orders/versions/" + orderVersionId + "/lines",
                                null,
                                specialist)
                        .body()
                        .get(0)
                        .get("id")
                        .asText();
        Result delivery =
                call(
                        "POST",
                        "/api/v1/fulfillment/orders/" + orderId + "/deliveries",
                        Map.of(
                                "arrivedOn",
                                today.toString(),
                                "items",
                                List.of(Map.of("segmentId", segmentId, "quantity", 10))),
                        specialist);
        assertEquals(201, delivery.status());
        String deliveryId = value(delivery, "id");
        assertEquals(
                200,
                call(
                                "POST",
                                "/api/v1/fulfillment/deliveries/" + deliveryId + "/confirm",
                                null,
                                specialist)
                        .status());
        String deliveryLine =
                call(
                                "GET",
                                "/api/v1/fulfillment/deliveries/" + deliveryId + "/lines",
                                null,
                                specialist)
                        .body()
                        .get(0)
                        .get("id")
                        .asText();
        Result inspection =
                call(
                        "POST",
                        "/api/v1/fulfillment/deliveries/" + deliveryId + "/acceptances",
                        Map.of(
                                "inspectedOn",
                                today.toString(),
                                "items",
                                List.of(
                                        Map.of(
                                                "deliveryLineId",
                                                deliveryLine,
                                                "accepted",
                                                10,
                                                "qualityRejected",
                                                0,
                                                "otherRejected",
                                                0))),
                        specialist);
        assertEquals(201, inspection.status());
        assertEquals(
                200,
                call(
                                "POST",
                                "/api/v1/fulfillment/acceptances/"
                                        + value(inspection, "id")
                                        + "/confirm",
                                null,
                                specialist)
                        .status());
        Result rating =
                call(
                        "POST",
                        "/api/v1/ratings/calculate",
                        Map.of("supplierId", supplierId, "scope", "OVERALL"),
                        manager);
        assertEquals(200, rating.status());
        assertEquals("PRELIMINARY", value(rating, "state"));
        assertEquals(406, call("GET", "/api/v1/reports/suppliers.xlsx", null, manager).status());
        Result exported = call("GET", "/api/v1/reports/suppliers.xlsx", null, manager, XLSX);
        assertEquals(200, exported.status());
        assertEquals('P', exported.bytes()[0]);
        assertEquals('K', exported.bytes()[1]);
        Result loggedIn =
                call(
                        "POST",
                        "/api/v1/auth/login",
                        Map.of("login", specialistLogin, "password", "strong-test-password-12345"),
                        null);
        String refresh = value(loggedIn, "refreshToken");
        Result rotated =
                call("POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh), null);
        assertEquals(200, rotated.status());
        assertEquals(
                401,
                call("POST", "/api/v1/auth/refresh", Map.of("refreshToken", refresh), null)
                        .status());
        assertEquals(
                401,
                call("GET", "/api/v1/suppliers", null, value(rotated, "accessToken")).status());
    }
}
