package by.bsuir.supplier.service.contract;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import by.bsuir.supplier.entity.auth.User;
import by.bsuir.supplier.entity.contract.Contract;
import by.bsuir.supplier.entity.contract.ContractVersion;
import by.bsuir.supplier.entity.enums.ContractState;
import by.bsuir.supplier.entity.enums.RoleCode;
import by.bsuir.supplier.entity.enums.SupplierStatus;
import by.bsuir.supplier.entity.enums.VersionState;
import by.bsuir.supplier.entity.supplier.Supplier;
import by.bsuir.supplier.repository.contract.*;
import by.bsuir.supplier.repository.supplier.SupplierRepository;
import by.bsuir.supplier.service.auth.ActorAccess;
import by.bsuir.supplier.service.common.*;

@Service
public class ContractService {
    private final ActorAccess access;
    private final ContractRepository contracts;
    private final ContractVersionRepository versions;
    private final SupplierRepository suppliers;
    private final AuditTrail audit;
    public ContractService(ActorAccess access, ContractRepository contracts, ContractVersionRepository versions,
        SupplierRepository suppliers, AuditTrail audit) {
        this.access = access;
        this.contracts = contracts;
        this.versions = versions;
        this.suppliers = suppliers;
        this.audit = audit;
    }
    public record Terms(String number, LocalDate signedOn, LocalDate from, LocalDate to, String text) {
    }
    @Transactional
    public Contract create(UUID actorId, UUID supplierId, Terms terms) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Supplier supplier = suppliers.findById(supplierId).orElseThrow(()->new BusinessException("Поставщик не найден"));
        Rules.require(supplier.getStatus() != SupplierStatus.TERMINATED,"Сотрудничество прекращено");
        Contract contract = new Contract();
        contract.setSupplier(supplier);
        contract.setCreatedBy(actor);
        contract.setState(ContractState.DRAFT);
        contract = contracts.saveAndFlush(contract);
        ContractVersion v = buildVersion(contract, 1, actor, terms, null);
        versions.saveAndFlush(v);
        audit.record(actor,"CONTRACT_CREATED", contract,"Contract", contract.getId(), null);
        return contract;
    }
    @Transactional
    public ContractVersion createRevision(UUID actorId, UUID contractId, Terms terms, String reason) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        Contract c = locked(contractId);
        Rules.require(c.getState() != ContractState.TERMINATED,"Договор прекращён");
        // A rejected first draft may be replaced with a new version; history remains immutable.
        Rules.require(c.getCurrentRegisteredVersion() != null || versions.findByContract_Id(contractId).stream().anyMatch(v->v.getState() == VersionState.REJECTED),"У договора нет версии для переработки");
        Rules.require(versions.findByContract_Id(contractId).stream().noneMatch(v->v.getState() == VersionState.PENDING || v.getState() == VersionState.DRAFT),"У договора уже есть незавершённая редакция");
        int max = versions.findByContract_Id(contractId).stream().mapToInt(v->v.getVersionNo()).max().orElse(0);
        ContractVersion v = buildVersion(c, max+1, actor, terms, Rules.text(reason,"Причина редакции"));
        v = versions.saveAndFlush(v);
        audit.record(actor,"CONTRACT_REVISION_CREATED", v,"ContractVersion", v.getId(), reason);
        return v;
    }
    @Transactional
    public ContractVersion submit(UUID actorId, UUID versionId) {
        User actor = access.requireRole(actorId, RoleCode.PROCUREMENT_SPECIALIST, RoleCode.PROCUREMENT_MANAGER);
        ContractVersion v = versions.findById(versionId).orElseThrow(()->new BusinessException("Редакция не найдена"));
        Contract c = locked(v.getContract().getId());
        Rules.require(v.getState() == VersionState.DRAFT,"Редакция не является черновиком");
        Rules.require(c.getState() != ContractState.TERMINATED,"Договор прекращён");
        v.setState(VersionState.PENDING);
        audit.record(actor,"CONTRACT_SUBMITTED", v,"ContractVersion", v.getId(), null);
        return v;
    }
    @Transactional
    public ContractVersion decide(UUID actorId, UUID versionId, boolean approve, String comment) {
        User manager = access.requireRole(actorId, RoleCode.PROCUREMENT_MANAGER);
        ContractVersion v = versions.findById(versionId).orElseThrow(()->new BusinessException("Редакция не найдена"));
        Contract c = locked(v.getContract().getId());
        Rules.require(v.getState() == VersionState.PENDING,"Редакция не ожидает решения");
        ActorAccess.independent(v.getCreatedBy(), manager);
        Rules.require(c.getState() != ContractState.TERMINATED,"Договор прекращён");
        if (approve) {
            Rules.require(c.getSupplier().getStatus() == SupplierStatus.ACTIVE,"Поставщик не активен");
        }
        else Rules.text(comment,"Причина отклонения");
        v.setState(approve?VersionState.APPROVED:VersionState.REJECTED);
        v.setDecidedBy(manager);
        v.setDecidedAt(Instant.now());
        v.setDecisionComment(comment);
        versions.saveAndFlush(v);
        // SQL trigger requires approved version before pointer update
        if (approve) {
            c.setCurrentRegisteredVersion(v);
            c.setState(ContractState.REGISTERED);
        }
        audit.record(manager, approve?"CONTRACT_APPROVED":"CONTRACT_REJECTED", v,"ContractVersion", v.getId(), comment);
        return v;
    }
    @Transactional
    public Contract terminate(UUID actorId, UUID contractId, String reason) {
        User manager = access.requireRole(actorId, RoleCode.PROCUREMENT_MANAGER);
        Contract c = locked(contractId);
        Rules.require(c.getState() == ContractState.REGISTERED,"Договор не зарегистрирован");
        ActorAccess.independent(c.getCreatedBy(), manager);
        c.setState(ContractState.TERMINATED);
        audit.record(manager,"CONTRACT_TERMINATED", c,"Contract", c.getId(), Rules.text(reason,"Причина прекращения"));
        return c;
    }
    private Contract locked(UUID id) {
        return contracts.findByIdForUpdate(id).orElseThrow(()->new BusinessException("Договор не найден"));
    }
    private static ContractVersion buildVersion(Contract c, int no, User user, Terms t, String reason) {
        Rules.present(t,"Условия договора");
        Rules.dates(t.from(), t.to());
        ContractVersion v = new ContractVersion();
        v.setContract(c);
        v.setVersionNo(no);
        v.setNumber(Rules.text(t.number(),"Номер договора"));
        v.setSignedOn(t.signedOn());
        v.setValidFrom(t.from());
        v.setValidTo(t.to());
        v.setTerms(Rules.text(t.text(),"Условия"));
        v.setChangeReason(reason);
        v.setState(VersionState.DRAFT);
        v.setCreatedBy(user);
        return v;
    }
    public static void requireCurrent(Contract c, LocalDate day) {
        Rules.require(c != null && c.getState() == ContractState.REGISTERED && c.getCurrentRegisteredVersion() != null,"Требуется зарегистрированный договор");
        ContractVersion v = c.getCurrentRegisteredVersion();
        Rules.require(!day.isBefore(v.getValidFrom()) && !day.isAfter(v.getValidTo()),"Договор не действует на указанную дату");
    }
}
