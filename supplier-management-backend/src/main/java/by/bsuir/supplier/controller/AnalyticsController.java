package by.bsuir.supplier.controller;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import by.bsuir.supplier.dto.response.AdminDashboard;
import by.bsuir.supplier.dto.response.BusinessDashboard;
import by.bsuir.supplier.dto.response.SupplierReportRow;
import by.bsuir.supplier.service.query.*;

@RestController
@RequestMapping("/api/v1")
public class AnalyticsController {
    private final AnalyticsQueries queries;
    public AnalyticsController(AnalyticsQueries queries) {
        this.queries = queries;
    }
    @GetMapping("/dashboard")
    public BusinessDashboard dashboard(Authentication a) {
        return queries.business(ApiActor.id(a));
    }
    @GetMapping("/admin/dashboard")
    public AdminDashboard admin(Authentication a) {
        return queries.admin(ApiActor.id(a));
    }
    @GetMapping("/reports/suppliers")
    public List<SupplierReportRow> suppliers(Authentication a) {
        return queries.report(ApiActor.id(a));
    }
    @GetMapping(value = "/reports/suppliers.xlsx", produces = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    public ResponseEntity<byte[]> suppliersXlsx(Authentication a) {
        byte[] data = SmallXlsxWriter.supplierReport(queries.report(ApiActor.id(a)));
        return ResponseEntity.ok().header(HttpHeaders.CONTENT_DISPOSITION,"attachment; filename=suppliers.xlsx")
            .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(data);
    }
}
