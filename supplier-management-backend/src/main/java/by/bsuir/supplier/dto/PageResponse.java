package by.bsuir.supplier.dto;

import org.springframework.data.domain.Page;

import java.util.List;

/** Stable pagination contract, independent of Hibernate/JPA serialization. */
public record PageResponse<T>(
        List<T> items, long totalElements, int totalPages, int page, int size) {
    public static <T> PageResponse<T> from(Page<T> data) {
        return new PageResponse<>(
                data.getContent(),
                data.getTotalElements(),
                data.getTotalPages(),
                data.getNumber(),
                data.getSize());
    }
}
