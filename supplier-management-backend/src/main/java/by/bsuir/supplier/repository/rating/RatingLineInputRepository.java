package by.bsuir.supplier.repository.rating;

import by.bsuir.supplier.entity.rating.RatingLineInput;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

/** Type-safe persistence gateway. Business validation belongs to service transactions. */
public interface RatingLineInputRepository extends JpaRepository<RatingLineInput, UUID> {
    java.util.List<by.bsuir.supplier.entity.rating.RatingLineInput> findByRating_Id(
            java.util.UUID ratingId);
}
