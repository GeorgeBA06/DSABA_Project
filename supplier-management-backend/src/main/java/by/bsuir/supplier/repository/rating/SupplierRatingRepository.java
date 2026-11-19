package by.bsuir.supplier.repository.rating;

import by.bsuir.supplier.entity.enums.RatingScope;
import by.bsuir.supplier.entity.rating.SupplierRating;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface SupplierRatingRepository extends JpaRepository<SupplierRating, UUID> {

    List<SupplierRating> findBySupplier_Id(UUID supplierId);

    @Query(
            """
            select rating
            from SupplierRating rating
            join fetch rating.supplier
            where rating.supplier.id in :supplierIds
              and rating.scope = :scope
            order by rating.asOfAt desc, rating.id desc
            """)
    List<SupplierRating> findReportRatings(
            @Param("supplierIds") List<UUID> supplierIds, @Param("scope") RatingScope scope);
}
