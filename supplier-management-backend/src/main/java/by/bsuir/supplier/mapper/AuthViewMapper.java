package by.bsuir.supplier.mapper;

import by.bsuir.supplier.dto.response.UserView;
import by.bsuir.supplier.entity.auth.User;

/** Maps persistence entities to immutable public response DTOs. */
public final class AuthViewMapper {
    private AuthViewMapper() {}

    public static UserView toView(User e) {
        return new UserView(
                e.getId(),
                e.getLogin(),
                e.getDisplayName(),
                e.getStatus(),
                e.getCreatedAt(),
                e.getUpdatedAt());
    }
}
