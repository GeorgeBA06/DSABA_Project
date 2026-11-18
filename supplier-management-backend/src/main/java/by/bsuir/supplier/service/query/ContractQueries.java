package by.bsuir.supplier.service.query;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.dto.PageResponse;
import by.bsuir.supplier.dto.response.ContractVersionView;
import by.bsuir.supplier.dto.response.ContractView;
import by.bsuir.supplier.entity.contract.Contract;
import by.bsuir.supplier.entity.contract.ContractVersion;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.mapper.ContractViewMapper;
import by.bsuir.supplier.repository.contract.ContractRepository;
import by.bsuir.supplier.repository.contract.ContractVersionRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.BusinessException;

@Service
public class ContractQueries {
    private final ActorAccess access;
    private final ContractRepository contractRepo;
    private final ContractVersionRepository contractVersionRepo;
    public ContractQueries(ActorAccess access, ContractRepository contractRepo, ContractVersionRepository contractVersionRepo) {
        this.access = access;
        this.contractRepo = contractRepo;
        this.contractVersionRepo = contractVersionRepo;
    }
    private static Pageable pageRequest(int page, int size) {
        if (page<0 || size<1 || size>100)throw new BusinessException("Недопустимые параметры страницы (size: 1–100)");
        return PageRequest.of(page, size, Sort.by("id"));
    }
    @Transactional(readOnly = true)
    public ContractView getContract(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return ContractViewMapper.toView(contractRepo.findById(id).orElseThrow(()->new BusinessException("Объект не найден")));
    }
    @Transactional(readOnly = true)
    public PageResponse<ContractView> listContract(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<Contract> result = contractRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(ContractViewMapper::toView));
    }
    @Transactional(readOnly = true)
    public ContractVersionView getContractVersion(UUID actorId, UUID id) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        return ContractViewMapper.toView(contractVersionRepo.findById(id).orElseThrow(()->new BusinessException("Объект не найден")));
    }
    @Transactional(readOnly = true)
    public PageResponse<ContractVersionView> listContractVersion(UUID actorId, int page, int size) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Page<ContractVersion> result = contractVersionRepo.findAll(pageRequest(page, size));
        return PageResponse.from(result.map(ContractViewMapper::toView));
    }
    @Transactional(readOnly = true)
    public List<ContractVersionView> versions(UUID actorId, UUID contractId) {
        access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        contractRepo.findById(contractId).orElseThrow(()->new BusinessException("Договор не найден"));
        return contractVersionRepo.findByContract_Id(contractId).stream().map(ContractViewMapper::toView).toList();
    }
}
