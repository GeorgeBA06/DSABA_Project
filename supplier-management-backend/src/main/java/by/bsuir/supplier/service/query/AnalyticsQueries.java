package by.bsuir.supplier.service.query;

import by.bsuir.supplier.dto.response.AdminDashboard;
import by.bsuir.supplier.dto.response.BusinessDashboard;
import by.bsuir.supplier.dto.response.SupplierReportRow;
import by.bsuir.supplier.entity.enums.RatingScope;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.rating.SupplierRating;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.repository.auth.UserRepository;
import by.bsuir.supplier.repository.catalog.ProductRepository;
import by.bsuir.supplier.repository.claim.ClaimRepository;
import by.bsuir.supplier.repository.contract.ContractRepository;
import by.bsuir.supplier.repository.fulfillment.DeliveryRepository;
import by.bsuir.supplier.repository.order.PurchaseOrderRepository;
import by.bsuir.supplier.repository.rating.SupplierRatingRepository;
import by.bsuir.supplier.repository.supplier.SupplierRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.BusinessException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class AnalyticsQueries {

    private final ActorAccess access;
    private final SupplierRepository suppliers;
    private final ProductRepository products;
    private final ContractRepository contracts;
    private final PurchaseOrderRepository orders;
    private final DeliveryRepository deliveries;
    private final ClaimRepository claims;
    private final SupplierRatingRepository ratings;
    private final UserRepository users;

    public AnalyticsQueries(
            ActorAccess access,
            SupplierRepository suppliers,
            ProductRepository products,
            ContractRepository contracts,
            PurchaseOrderRepository orders,
            DeliveryRepository deliveries,
            ClaimRepository claims,
            SupplierRatingRepository ratings,
            UserRepository users) {

        this.access = access;
        this.suppliers = suppliers;
        this.products = products;
        this.contracts = contracts;
        this.orders = orders;
        this.deliveries = deliveries;
        this.claims = claims;
        this.ratings = ratings;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public BusinessDashboard business(UUID actorId) {

        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);

        return new BusinessDashboard(
                suppliers.count(),
                products.count(),
                contracts.count(),
                orders.count(),
                deliveries.count(),
                claims.count(),
                ratings.count());
    }

    @Transactional(readOnly = true)
    public AdminDashboard admin(UUID actorId) {

        access.requireRole(actorId, RoleCode.ADMINISTRATOR);

        return new AdminDashboard(users.count());
    }

    /** Fetches suppliers in batches. Export size is capped to bound memory and response size. */
    @Transactional(readOnly = true)
    public List<SupplierReportRow> report(UUID actorId) {

        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);

        var rows = new ArrayList<SupplierReportRow>();

        for (int page = 0; ; page++) {

            Page<Supplier> batch = suppliers.findAll(PageRequest.of(page, 100, Sort.by("id")));

            List<Supplier> supplierBatch = batch.getContent();

            List<UUID> supplierIds = supplierBatch.stream().map(Supplier::getId).toList();

            Map<UUID, SupplierRating> latestRatings = new HashMap<>();

            if (!supplierIds.isEmpty()) {

                // The query is ordered newest-first,
                // so the first row per supplier is its latest overall rating.
                for (SupplierRating rating :
                        ratings.findReportRatings(supplierIds, RatingScope.OVERALL)) {

                    latestRatings.putIfAbsent(rating.getSupplier().getId(), rating);
                }
            }

            for (Supplier supplier : supplierBatch) {

                if (rows.size() >= 10000) {
                    throw new BusinessException("Экспорт ограничен 10 000 поставщиками");
                }

                SupplierRating latest = latestRatings.get(supplier.getId());

                rows.add(
                        new SupplierReportRow(
                                supplier.getId(),
                                supplier.getName(),
                                supplier.getStatus(),
                                latest == null ? null : latest.getTotalScore(),
                                latest == null ? null : latest.getBand(),
                                latest == null ? null : latest.getState()));
            }

            if (batch.isLast()) {
                break;
            }
        }

        return rows;
    }
}
