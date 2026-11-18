package by.bsuir.supplier.service.query;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.response.ClaimView;
import by.bsuir.supplier.dto.response.InteractionView;
import by.bsuir.supplier.entity.claim.Claim;
import by.bsuir.supplier.entity.claim.Interaction;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.mapper.ClaimViewMapper;
import by.bsuir.supplier.repository.claim.ClaimRepository;
import by.bsuir.supplier.repository.claim.InteractionRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.BusinessException;

@Service
public class ClaimQueries {
    private final ActorAccess access;
    private final ClaimRepository claimRepo;
    private final InteractionRepository interactionRepo;
    public ClaimQueries(ActorAccess access, ClaimRepository claimRepo, InteractionRepository interactionRepo) {
        this.access = access;
        this.claimRepo = claimRepo;
        this.interactionRepo = interactionRepo;
    }
    private static Pageable pageRequest(int page, int size) {
        if (page<0 || size<1 || size>100)throw new BusinessException("Недопустимые параметры страницы (size: 1–100)");
        return PageRequest.of(page, size, Sort.by("id"));
    }
    @Transactional(readOnly = true)
    public ClaimView getClaim(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return ClaimViewMapper.toView(claimRepo.findById(id).orElseThrow(()->new BusinessException("Объект не найден")));
    }
    @Transactional(readOnly = true)
    public PageResponse<ClaimView> listClaim(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<Claim> result = claimRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(ClaimViewMapper::toView));
    }
    @Transactional(readOnly = true)
    public InteractionView getInteraction(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return ClaimViewMapper.toView(interactionRepo.findById(id).orElseThrow(()->new BusinessException("Объект не найден")));
    }
    @Transactional(readOnly = true)
    public PageResponse<InteractionView> listInteraction(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<Interaction> result = interactionRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(ClaimViewMapper::toView));
    }
}
