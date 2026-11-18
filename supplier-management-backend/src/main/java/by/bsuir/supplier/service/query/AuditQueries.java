package by.bsuir.supplier.service.query;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.response.AuditEventView;
import by.bsuir.supplier.entity.audit.AuditEvent;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.mapper.AuditViewMapper;
import by.bsuir.supplier.repository.audit.AuditEventRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.BusinessException;

@Service
public class AuditQueries {
    private final ActorAccess access;
    private final AuditEventRepository auditEventRepo;
    public AuditQueries(ActorAccess access, AuditEventRepository auditEventRepo) {
        this.access = access;
        this.auditEventRepo = auditEventRepo;
    }
    private static Pageable pageRequest(int page, int size) {
        if (page<0 || size<1 || size>100)throw new BusinessException("Недопустимые параметры страницы (size: 1–100)");
        return PageRequest.of(page, size, Sort.by("id"));
    }
    @Transactional(readOnly = true)
    public AuditEventView getAuditEvent(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.ADMINISTRATOR);
        return AuditViewMapper.toView(auditEventRepo.findById(id).orElseThrow(()->new BusinessException("Объект не найден")));
    }
    @Transactional(readOnly = true)
    public PageResponse<AuditEventView> listAuditEvent(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.ADMINISTRATOR);
        Page<AuditEvent> result = auditEventRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(AuditViewMapper::toView));
    }
}
