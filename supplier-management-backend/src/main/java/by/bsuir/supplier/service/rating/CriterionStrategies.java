package by.bsuir.supplier.service.rating;

import by.bsuir.supplier.service.rating.RatingPolicy.Criterion;

import java.math.BigDecimal;
import java.util.List;

/**
 * Strategy pattern: criteria have independently swappable calculation policies. New policies are
 * versioned via SupplierRating.methodVersion, not selected by end users.
 */
public final class CriterionStrategies {
    public record Facts(
            BigDecimal ordered,
            BigDecimal onTime,
            BigDecimal fulfilled,
            BigDecimal qualityChecked,
            BigDecimal qualityGood,
            Integer expertScore) {}

    public interface CriterionStrategy {
        Criterion criterion();

        BigDecimal calculate(Facts facts);
    }

    private static final List<CriterionStrategy> IMPLEMENTATIONS =
            List.of(
                    new CriterionStrategy() {
                        public Criterion criterion() {
                            return Criterion.TIMELINESS;
                        }

                        public BigDecimal calculate(Facts f) {
                            return RatingPolicy.percent(f.onTime(), f.ordered());
                        }
                    },
                    new CriterionStrategy() {
                        public Criterion criterion() {
                            return Criterion.COMPLETENESS;
                        }

                        public BigDecimal calculate(Facts f) {
                            return RatingPolicy.percent(f.fulfilled(), f.ordered());
                        }
                    },
                    new CriterionStrategy() {
                        public Criterion criterion() {
                            return Criterion.QUALITY;
                        }

                        public BigDecimal calculate(Facts f) {
                            return RatingPolicy.percent(f.qualityGood(), f.qualityChecked());
                        }
                    },
                    new CriterionStrategy() {
                        public Criterion criterion() {
                            return Criterion.INTERACTION;
                        }

                        public BigDecimal calculate(Facts f) {
                            return f.expertScore() == null
                                    ? null
                                    : BigDecimal.valueOf(f.expertScore()).setScale(1);
                        }
                    });

    private CriterionStrategies() {}

    public static BigDecimal evaluate(Criterion criterion, Facts facts) {
        return IMPLEMENTATIONS.stream()
                .filter(p -> p.criterion() == criterion)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unknown criterion: " + criterion))
                .calculate(facts);
    }
}
