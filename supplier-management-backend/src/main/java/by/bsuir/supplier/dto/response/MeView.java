package by.bsuir.supplier.dto.response;

import by.bsuir.supplier.entity.enums.RoleCode;

import java.util.List;

/** Profile and effective roles of the signed-in user. */
public record MeView(UserView user, List<RoleCode> roles) {}
