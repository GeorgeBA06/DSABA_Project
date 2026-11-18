package by.bsuir.supplier.dto.response;

import java.util.List;

import by.bsuir.supplier.entity.enums.RoleCode;

/** Profile and effective roles of the signed-in user. */
public record MeView(UserView user, List<RoleCode> roles) {}
