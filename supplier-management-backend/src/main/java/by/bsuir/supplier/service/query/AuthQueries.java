package by.bsuir.supplier.service.query;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.response.MeView;
import by.bsuir.supplier.dto.response.UserView;
import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.mapper.AuthViewMapper;
import by.bsuir.supplier.repository.auth.UserRepository;
import by.bsuir.supplier.repository.auth.UserRoleAssignmentRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.BusinessException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class AuthQueries {
    private final ActorAccess access;
    private final UserRoleAssignmentRepository grants;
    private final UserRepository userRepo;

    public AuthQueries(
            ActorAccess access, UserRoleAssignmentRepository grants, UserRepository userRepo) {
        this.access = access;
        this.grants = grants;
        this.userRepo = userRepo;
    }

    @Transactional(readOnly = true)
    public MeView me(UUID actorId) {
        var user =
                userRepo.findById(actorId)
                        .orElseThrow(() -> new BusinessException("Пользователь не найден"));
        var roles =
                grants.findByUser_IdAndRevokedAtIsNull(actorId).stream()
                        .map(a -> a.getRoleCode())
                        .toList();
        return new MeView(AuthViewMapper.toView(user), roles);
    }

    @Transactional(readOnly = true)
    public List<RoleCode> activeRoles(UUID actorId, UUID userId) {
        access.requireRole(actorId, RoleCode.ADMINISTRATOR);
        userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("Пользователь не найден"));
        return grants.findByUser_IdAndRevokedAtIsNull(userId).stream()
                .map(a -> a.getRoleCode())
                .toList();
    }

    private static Pageable pageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100)
            throw new BusinessException("Недопустимые параметры страницы (size: 1–100)");
        return PageRequest.of(page, size, Sort.by("id"));
    }

    @Transactional(readOnly = true)
    public UserView getUser(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.ADMINISTRATOR);
        return AuthViewMapper.toView(
                userRepo.findById(id).orElseThrow(() -> new BusinessException("Объект не найден")));
    }

    @Transactional(readOnly = true)
    public PageResponse<UserView> listUser(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.ADMINISTRATOR);
        Page<User> result = userRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(AuthViewMapper::toView));
    }
}
