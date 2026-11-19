#!/usr/bin/env python3
"""Reproducible model code generator for the approved 30-table data model.

The generated Flyway migration, JPA entities, enum types and repository interfaces
are checked in. This script requires only Python 3 standard library. Run from any cwd.
"""

from __future__ import annotations
from pathlib import Path
from collections import OrderedDict
import json
import re
import hashlib

ROOT = Path(__file__).resolve().parents[1]
BASE = ROOT / "src/main/java/by/bsuir/supplier"
RES = ROOT / "src/main/resources/db/migration"

ENUMS = OrderedDict(
    {
        "UserStatus": "ACTIVE BLOCKED",
        "RoleCode": "PROCUREMENT_SPECIALIST PROCUREMENT_MANAGER ADMINISTRATOR",
        "SupplierStatus": "CANDIDATE ACTIVE SUSPENDED TERMINATED",
        "DecisionState": "PENDING APPROVED REJECTED",
        "ContractState": "DRAFT REGISTERED TERMINATED",
        "VersionState": "DRAFT PENDING APPROVED REJECTED",
        "OrderState": "DRAFT PENDING_APPROVAL APPROVED IN_PROGRESS COMPLETED CANCELLED",
        "OrderVersionKind": "INITIAL CHANGE",
        "Initiator": "BUYER SUPPLIER CORRECTION",
        "OriginReason": "INITIAL BUYER_CHANGE SUPPLIER_RESCHEDULE CORRECTION",
        "ClosureAction": "CLOSE REOPEN",
        "ClosureResult": "FULFILLED CLOSED_WITH_REMAINDER",
        "FactState": "DRAFT CONFIRMED CANCELLED",
        "ReturnState": "DRAFT PENDING CONFIRMED REJECTED",
        "InteractionChannel": "EMAIL PHONE MEETING MESSENGER OTHER",
        "ViolationType": "LATE SHORTAGE QUALITY OTHER",
        "ClaimState": "DRAFT SENT IN_RESOLUTION CLOSED CANCELLED",
        "ClaimResult": "FULL PARTIAL NONE",
        "RemedyType": "REPLACEMENT ADDITIONAL_DELIVERY REFUND OTHER",
        "RatingScope": "OVERALL CATEGORY",
        "RatingState": "INSUFFICIENT_DATA PRELIMINARY PUBLISHED",
        "RatingBand": "HIGH ACCEPTABLE ATTENTION LOW",
        "SelectionReason": "DUE_IN_PERIOD OPEN_AT_PERIOD_START",
        "AuditCategory": "BUSINESS SECURITY",
    }
)
ENUMS = OrderedDict((k, v.split()) for k, v in ENUMS.items())

# Each field: SQL name : type [! = not null]. ref:<table> maps to LAZY @ManyToOne.
# Only @EmbeddedId junctions deliberately use a composite PK.
TABLES = OrderedDict()


def t(name, cls, module, f, *, pk=("id",), unique=(), indexes=(), checks=()):
    fields = OrderedDict()
    for item in f.strip().split("\n"):
        if not item.strip() or item.strip().startswith("#"):
            continue
        col, typ = item.strip().split(None, 1)
        fields[col] = typ.strip()
    TABLES[name] = dict(
        name=name,
        cls=cls,
        module=module,
        fields=fields,
        pk=list(pk),
        unique=[list(x) for x in unique],
        indexes=[list(x) for x in indexes],
        checks=list(checks),
    )


t(
    "users",
    "User",
    "auth",
    """
id uuid!
login str:80!
password_hash str:255!
display_name str:200!
status enum:UserStatus!
failed_login_count int!
locked_until instant
created_at instant!
updated_at instant!
lock_version long!
""",
    unique=[("login",)],
    checks=["failed_login_count >= 0"],
)

t(
    "user_role_assignments",
    "UserRoleAssignment",
    "auth",
    """
id uuid!
user_id ref:users!
role_code enum:RoleCode!
assigned_by_id ref:users!
assigned_at instant!
revoked_at instant
""",
    indexes=[("user_id",), ("assigned_at",)],
    checks=["revoked_at IS NULL OR revoked_at >= assigned_at"],
)

t(
    "auth_sessions",
    "AuthSession",
    "auth",
    """
id uuid!
user_id ref:users!
refresh_token_hash str:128!
token_generation long!
created_at instant!
expires_at instant!
revoked_at instant
rotated_at instant
lock_version long!
""",
    unique=[("refresh_token_hash",)],
    indexes=[("user_id",), ("expires_at",)],
    checks=["token_generation >= 0", "expires_at > created_at"],
)

t(
    "product_categories",
    "ProductCategory",
    "catalog",
    """
id uuid!
code str:50!
name str:150!
active bool!
lock_version long!
""",
    unique=[("code",)],
)

t(
    "units",
    "Unit",
    "catalog",
    """
id uuid!
code str:20!
name str:80!
active bool!
""",
    unique=[("code",)],
)

t(
    "products",
    "Product",
    "catalog",
    """
id uuid!
sku str:80!
name str:250!
category_id ref:product_categories!
unit_id ref:units!
active bool!
created_at instant!
updated_at instant!
lock_version long!
""",
    unique=[("sku",)],
    indexes=[("category_id",)],
)

t(
    "suppliers",
    "Supplier",
    "supplier",
    """
id uuid!
name str:250!
contact_name str:150
email str:250
phone str:60
address text
registration_number str:80
status enum:SupplierStatus!
created_by_id ref:users!
created_at instant!
updated_at instant!
lock_version long!
""",
    indexes=[("status",), ("name",)],
)

t(
    "supplier_products",
    "SupplierProduct",
    "supplier",
    """
supplier_id ref:suppliers!
product_id ref:products!
supplier_item_code str:80
active bool!
""",
    pk=("supplier_id", "product_id"),
    indexes=[("product_id",)],
)

t(
    "supplier_status_requests",
    "SupplierStatusRequest",
    "supplier",
    """
id uuid!
supplier_id ref:suppliers!
from_status enum:SupplierStatus!
to_status enum:SupplierStatus!
reason text!
requested_by_id ref:users!
requested_at instant!
state enum:DecisionState!
decided_by_id ref:users
decided_at instant
decision_comment text
""",
    indexes=[("supplier_id", "requested_at")],
    checks=[
        "from_status <> to_status",
        "requested_by_id IS DISTINCT FROM decided_by_id",
        "(state = 'PENDING' AND decided_by_id IS NULL AND decided_at IS NULL) OR (state IN ('APPROVED','REJECTED') AND decided_by_id IS NOT NULL AND decided_at IS NOT NULL)",
    ],
)

t(
    "contracts",
    "Contract",
    "contract",
    """
id uuid!
supplier_id ref:suppliers!
state enum:ContractState!
current_registered_version_id ref:contract_versions
created_by_id ref:users!
created_at instant!
lock_version long!
""",
    indexes=[("supplier_id", "state")],
)

t(
    "contract_versions",
    "ContractVersion",
    "contract",
    """
id uuid!
contract_id ref:contracts!
version_no int!
number str:100!
signed_on date
valid_from date!
valid_to date!
terms text!
change_reason text
state enum:VersionState!
created_by_id ref:users!
created_at instant!
decided_by_id ref:users
decided_at instant
decision_comment text
""",
    unique=[("contract_id", "version_no"), ("id", "contract_id")],
    checks=[
        "version_no > 0",
        "valid_from <= valid_to",
        "created_by_id IS DISTINCT FROM decided_by_id",
        "(state IN ('DRAFT','PENDING') AND decided_by_id IS NULL AND decided_at IS NULL) OR (state IN ('APPROVED','REJECTED') AND decided_by_id IS NOT NULL AND decided_at IS NOT NULL)",
    ],
)

t(
    "purchase_orders",
    "PurchaseOrder",
    "order",
    """
id uuid!
number str:80!
supplier_id ref:suppliers!
contract_id ref:contracts
state enum:OrderState!
current_approved_version_id ref:order_versions
created_by_id ref:users!
created_at instant!
updated_at instant!
lock_version long!
""",
    unique=[("number",)],
    indexes=[("supplier_id", "state"), ("contract_id",)],
)

t(
    "order_lines",
    "OrderLine",
    "order",
    """
id uuid!
order_id ref:purchase_orders!
line_no int!
product_id ref:products!
""",
    unique=[("order_id", "line_no"), ("id", "order_id")],
    checks=["line_no > 0"],
)

t(
    "order_versions",
    "OrderVersion",
    "order",
    """
id uuid!
order_id ref:purchase_orders!
version_no int!
base_version_id ref:order_versions
contract_version_id ref:contract_versions
kind enum:OrderVersionKind!
initiator enum:Initiator
reason text
state enum:VersionState!
created_by_id ref:users!
created_at instant!
submitted_at instant
decided_by_id ref:users
decided_at instant
decision_comment text
""",
    unique=[("order_id", "version_no"), ("id", "order_id")],
    checks=[
        "version_no > 0",
        "created_by_id IS DISTINCT FROM decided_by_id",
        "(state IN ('DRAFT','PENDING') AND decided_by_id IS NULL AND decided_at IS NULL) OR (state IN ('APPROVED','REJECTED') AND decided_by_id IS NOT NULL AND decided_at IS NOT NULL)",
    ],
)

t(
    "order_version_lines",
    "OrderVersionLine",
    "order",
    """
id uuid!
order_version_id ref:order_versions!
order_line_id ref:order_lines!
segment_key str:64!
source_version_line_id ref:order_version_lines
quantity qty!
unit_price money!
execution_due_on date!
rating_due_on date!
origin_reason enum:OriginReason!
product_name_snapshot str:250!
category_code_snapshot str:50!
category_name_snapshot str:150!
unit_code_snapshot str:20!
""",
    unique=[("order_version_id", "segment_key")],
    indexes=[("order_line_id",), ("execution_due_on",)],
    checks=[
        "quantity > 0",
        "unit_price >= 0",
        "source_version_line_id IS DISTINCT FROM id",
    ],
)

t(
    "order_closures",
    "OrderClosure",
    "order",
    """
id uuid!
order_id ref:purchase_orders!
action enum:ClosureAction!
result enum:ClosureResult
reason text
requested_by_id ref:users
decided_by_id ref:users
occurred_at instant!
""",
    indexes=[("order_id", "occurred_at")],
    checks=[
        "(action = 'CLOSE' AND result IS NOT NULL) OR (action = 'REOPEN' AND result IS NULL)",
        "(result IS DISTINCT FROM 'CLOSED_WITH_REMAINDER') OR (reason IS NOT NULL AND decided_by_id IS NOT NULL)",
        "requested_by_id IS DISTINCT FROM decided_by_id",
    ],
)

t(
    "order_closure_lines",
    "OrderClosureLine",
    "order",
    """
closure_id ref:order_closures!
order_line_id ref:order_lines!
ordered_qty qty!
fulfilled_qty qty!
unfulfilled_qty qty!
""",
    pk=("closure_id", "order_line_id"),
    checks=[
        "ordered_qty >= 0",
        "fulfilled_qty >= 0",
        "unfulfilled_qty >= 0",
        "ordered_qty = fulfilled_qty + unfulfilled_qty",
    ],
)

t(
    "deliveries",
    "Delivery",
    "fulfillment",
    """
id uuid!
order_id ref:purchase_orders!
arrived_on date!
registered_at instant!
registered_by_id ref:users!
state enum:FactState!
external_number str:100
supersedes_id ref:deliveries
correction_id ref:fact_corrections
""",
    indexes=[("order_id", "arrived_on")],
    checks=[
        "supersedes_id IS DISTINCT FROM id",
        "(correction_id IS NULL AND supersedes_id IS NULL) OR (correction_id IS NOT NULL AND supersedes_id IS NOT NULL)",
    ],
)

t(
    "delivery_lines",
    "DeliveryLine",
    "fulfillment",
    """
id uuid!
delivery_id ref:deliveries!
order_version_line_id ref:order_version_lines!
quantity qty!
replacement_for_line_id ref:delivery_lines
claim_id ref:claims
""",
    indexes=[("order_version_line_id",)],
    checks=["quantity > 0", "replacement_for_line_id IS DISTINCT FROM id"],
)

t(
    "acceptance_documents",
    "AcceptanceDocument",
    "fulfillment",
    """
id uuid!
delivery_id ref:deliveries!
inspected_on date!
registered_by_id ref:users!
registered_at instant!
state enum:FactState!
supersedes_id ref:acceptance_documents
correction_id ref:fact_corrections
""",
    indexes=[("delivery_id", "inspected_on")],
    checks=[
        "supersedes_id IS DISTINCT FROM id",
        "(correction_id IS NULL AND supersedes_id IS NULL) OR (correction_id IS NOT NULL AND supersedes_id IS NOT NULL)",
    ],
)

t(
    "acceptance_lines",
    "AcceptanceLine",
    "fulfillment",
    """
id uuid!
acceptance_document_id ref:acceptance_documents!
delivery_line_id ref:delivery_lines!
accepted_qty qty!
quality_rejected_qty qty!
other_rejected_qty qty!
other_reason text
""",
    indexes=[("delivery_line_id",)],
    checks=[
        "accepted_qty >= 0",
        "quality_rejected_qty >= 0",
        "other_rejected_qty >= 0",
        "accepted_qty + quality_rejected_qty + other_rejected_qty > 0",
        "other_rejected_qty = 0 OR other_reason IS NOT NULL",
    ],
)

t(
    "supplier_returns",
    "SupplierReturn",
    "fulfillment",
    """
id uuid!
order_id ref:purchase_orders!
returned_on date!
state enum:ReturnState!
reason text!
created_by_id ref:users!
approved_by_id ref:users
approved_at instant
supersedes_id ref:supplier_returns
correction_id ref:fact_corrections
""",
    indexes=[("order_id", "returned_on")],
    checks=[
        "created_by_id IS DISTINCT FROM approved_by_id",
        "supersedes_id IS DISTINCT FROM id",
        "(correction_id IS NULL AND supersedes_id IS NULL) OR (correction_id IS NOT NULL AND supersedes_id IS NOT NULL)",
        "(state IN ('DRAFT','PENDING','REJECTED') AND approved_by_id IS NULL AND approved_at IS NULL) OR (state = 'CONFIRMED' AND approved_by_id IS NOT NULL AND approved_at IS NOT NULL)",
    ],
)

t(
    "supplier_return_lines",
    "SupplierReturnLine",
    "fulfillment",
    """
id uuid!
return_id ref:supplier_returns!
acceptance_line_id ref:acceptance_lines!
quantity qty!
defect_description text!
claim_id ref:claims
""",
    indexes=[("acceptance_line_id",)],
    checks=["quantity > 0"],
)

t(
    "fact_corrections",
    "FactCorrection",
    "fulfillment",
    """
id uuid!
reason text!
state enum:VersionState!
effective_on date!
created_by_id ref:users!
created_at instant!
decided_by_id ref:users
decided_at instant
decision_comment text
""",
    checks=[
        "created_by_id IS DISTINCT FROM decided_by_id",
        "(state IN ('DRAFT','PENDING') AND decided_by_id IS NULL AND decided_at IS NULL) OR (state IN ('APPROVED','REJECTED') AND decided_by_id IS NOT NULL AND decided_at IS NOT NULL)",
    ],
)

t(
    "interactions",
    "Interaction",
    "claim",
    """
id uuid!
supplier_id ref:suppliers!
employee_id ref:users!
occurred_at instant!
channel enum:InteractionChannel!
subject str:250!
content text!
outcome text
claim_id ref:claims
""",
    indexes=[("supplier_id", "occurred_at")],
)

t(
    "claims",
    "Claim",
    "claim",
    """
id uuid!
supplier_id ref:suppliers!
number str:80!
order_line_id ref:order_lines!
delivery_line_id ref:delivery_lines
violation_type enum:ViolationType!
state enum:ClaimState!
description text!
requirements text!
response_due_on date
sent_at instant
send_channel enum:InteractionChannel
created_by_id ref:users!
created_at instant!
result enum:ClaimResult
remedy_type enum:RemedyType
resolution_description text
resolved_at instant
approved_by_id ref:users
lock_version long!
""",
    unique=[("number",)],
    indexes=[("supplier_id", "state"), ("response_due_on",)],
    checks=[
        "(state NOT IN ('CLOSED','CANCELLED')) OR (resolved_at IS NOT NULL)",
        "state <> 'CLOSED' OR result IS NOT NULL",
        "result NOT IN ('PARTIAL','NONE') OR approved_by_id IS NOT NULL",
        "(state <> 'SENT') OR (sent_at IS NOT NULL AND send_channel IS NOT NULL)",
        "created_by_id IS DISTINCT FROM approved_by_id",
    ],
)

t(
    "expert_assessments",
    "ExpertAssessment",
    "rating",
    """
id uuid!
supplier_id ref:suppliers!
period_start date!
period_end date!
applicable_from date!
applicable_to date!
score int!
justification text!
state enum:VersionState!
created_by_id ref:users!
created_at instant!
approved_by_id ref:users
approved_at instant
supersedes_id ref:expert_assessments
""",
    indexes=[("supplier_id", "applicable_from")],
    checks=[
        "period_end >= period_start",
        "applicable_to >= applicable_from",
        "applicable_to - applicable_from <= 29",
        "score IN (0,25,50,75,100)",
        "created_by_id IS DISTINCT FROM approved_by_id",
        "supersedes_id IS DISTINCT FROM id",
        "(state IN ('DRAFT','PENDING','REJECTED') AND approved_by_id IS NULL AND approved_at IS NULL) OR (state = 'APPROVED' AND approved_by_id IS NOT NULL AND approved_at IS NOT NULL)",
    ],
)

t(
    "supplier_ratings",
    "SupplierRating",
    "rating",
    """
id uuid!
supplier_id ref:suppliers!
scope enum:RatingScope!
category_id ref:product_categories
period_start date!
period_end date!
as_of_at instant!
method_version str:40!
rules_snapshot json!
expert_assessment_id ref:expert_assessments
timeliness_score score
completeness_score score
quality_score score
interaction_score score
total_score score
band enum:RatingBand
state enum:RatingState!
missing_reasons json
order_count int!
line_count int!
created_at instant!
supersedes_id ref:supplier_ratings
""",
    indexes=[("supplier_id", "as_of_at"), ("category_id",)],
    checks=[
        "period_end >= period_start",
        "(scope = 'OVERALL' AND category_id IS NULL) OR (scope = 'CATEGORY' AND category_id IS NOT NULL)",
        "order_count >= 0",
        "line_count >= 0",
        "state <> 'PUBLISHED' OR (order_count >= 3 AND line_count >= 1 AND expert_assessment_id IS NOT NULL)",
        "(state = 'PUBLISHED' AND total_score IS NOT NULL AND band IS NOT NULL AND timeliness_score IS NOT NULL AND completeness_score IS NOT NULL AND quality_score IS NOT NULL AND interaction_score IS NOT NULL) OR (state <> 'PUBLISHED' AND total_score IS NULL AND band IS NULL)",
        "supersedes_id IS DISTINCT FROM id",
    ],
)

t(
    "rating_line_inputs",
    "RatingLineInput",
    "rating",
    """
id uuid!
rating_id ref:supplier_ratings!
order_version_line_id ref:order_version_lines!
selection_reason enum:SelectionReason!
ordered_qty qty!
delivered_on_time_qty qty!
fulfilled_qty qty!
quality_checked_qty qty!
quality_good_qty qty!
timeliness_score score!
completeness_score score!
quality_score score
source_cutoff_at instant!
source_hash str:128
""",
    unique=[("rating_id", "order_version_line_id")],
    checks=[
        "ordered_qty > 0",
        "delivered_on_time_qty >= 0",
        "fulfilled_qty >= 0",
        "quality_checked_qty >= 0",
        "quality_good_qty >= 0",
        "quality_good_qty <= quality_checked_qty",
        "delivered_on_time_qty <= ordered_qty",
        "fulfilled_qty <= ordered_qty",
        "(quality_checked_qty = 0 AND quality_score IS NULL) OR (quality_checked_qty > 0 AND quality_score IS NOT NULL)",
    ],
)

t(
    "audit_events",
    "AuditEvent",
    "audit",
    """
id uuid!
actor_id ref:users
occurred_at instant!
category enum:AuditCategory!
action str:120!
entity_type str:100!
entity_id uuid!
before_data json
after_data json
reason text
correlation_id uuid!
""",
    indexes=[("entity_type", "entity_id", "occurred_at"), ("actor_id", "occurred_at")],
)

assert len(TABLES) == 30, f"expected 30, got {len(TABLES)}"


def cname(name):
    return "".join(p.capitalize() for p in name.split("_"))


def jname(col):
    # FKs express object references, not raw UUIDs.
    if col == "return_id":
        return "supplierReturn"
    if col.endswith("_id") and col not in {"entity_id", "correlation_id"}:
        col = col[:-3]
    bits = col.split("_")
    return bits[0] + "".join(x.capitalize() for x in bits[1:])


def sqltype(desc):
    base = desc.rstrip("!")
    if base.startswith("ref:") or base == "uuid":
        return "uuid"
    if base.startswith("str:"):
        return "varchar(" + base.partition(":")[2] + ")"
    if base.startswith("enum:"):
        return "varchar(40)"
    return {
        "text": "text",
        "date": "date",
        "instant": "timestamptz",
        "qty": "numeric(18,3)",
        "money": "numeric(19,2)",
        "score": "numeric(5,1)",
        "int": "integer",
        "long": "bigint",
        "bool": "boolean",
        "json": "jsonb",
    }[base]


def jtype(desc):
    base = desc.rstrip("!")
    if base.startswith("ref:"):
        return TABLES[base.partition(":")[2]]["cls"]
    if base.startswith("enum:"):
        return base.partition(":")[2]
    return {
        "uuid": "UUID",
        "text": "String",
        "date": "LocalDate",
        "instant": "Instant",
        "qty": "BigDecimal",
        "money": "BigDecimal",
        "score": "BigDecimal",
        "int": "Integer",
        "long": "Long",
        "bool": "Boolean",
        "json": "Map<String,Object>",
    }.get(base, "String" if base.startswith("str:") else None)


def safe_name(name):
    if len(name) <= 63:
        return name
    return name[:53] + "_" + hashlib.md5(name.encode()).hexdigest()[:8]


def ident(type_, tbl, cols):
    return safe_name(type_ + "_" + tbl + "_" + "_".join(cols))


def java_path(pkg, filename):
    return BASE / pkg / (filename + ".java")


def put(path, text):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(text.rstrip() + "\n", encoding="utf-8")


def emit_entities():
    for enum, values in ENUMS.items():
        put(
            java_path("entity/enums", enum),
            "package by.bsuir.supplier.entity.enums;\n\n"
            "/** Fixed codes persisted as strings (never ordinal numbers). */\n"
            "public enum " + enum + " { " + ", ".join(values) + " }",
        )
    for tbl, model in TABLES.items():
        cls = model["cls"]
        module = model["module"]
        fields = model["fields"]
        pk = model["pk"]
        is_composite = len(pk) > 1
        imports = """import jakarta.persistence.*;
import java.util.UUID;
import java.util.Map;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Instant;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import by.bsuir.supplier.entity.enums.*;
"""
        refs = sorted(
            {
                TABLES[v.rstrip("!").split(":", 1)[1]]["module"]
                for v in fields.values()
                if v.rstrip("!").startswith("ref:")
                and TABLES[v.rstrip("!").split(":", 1)[1]]["module"] != module
            }
        )
        for other_module in refs:
            imports += "import by.bsuir.supplier.entity." + other_module + ".*;\n"
        meta = '@Entity\n@Table(name = "' + tbl + '")\n'
        source = "package by.bsuir.supplier.entity." + module + ";\n\n" + imports + "\n"
        source += (
            "/** Persisted aggregate/data record for "
            + tbl
            + ". No business decisions belong here. */\n"
            + meta
        )
        source += "public class " + cls + " {\n"
        if is_composite:
            source += "    @EmbeddedId\n    private " + cls + "Id id;\n\n"
        for col, desc in fields.items():
            base = desc.rstrip("!")
            mandatory = desc.endswith("!")
            jn = jname(col)
            jt = jtype(desc)
            if is_composite and col in pk:
                source += '    @MapsId("' + jn + 'Id")\n'
            elif col == "id":
                source += (
                    "    @Id\n    @GeneratedValue(strategy = GenerationType.UUID)\n"
                )
            elif col == "lock_version":
                source += "    @Version\n"
            if base.startswith("ref:"):
                source += (
                    "    @ManyToOne(fetch = FetchType.LAZY, optional = "
                    + str(not mandatory).lower()
                    + ")\n"
                )
                source += (
                    '    @JoinColumn(name = "'
                    + col
                    + '", nullable = '
                    + str(not mandatory).lower()
                    + ")\n"
                )
            else:
                if base.startswith("enum:"):
                    source += "    @Enumerated(EnumType.STRING)\n"
                if base == "json":
                    source += "    @JdbcTypeCode(SqlTypes.JSON)\n"
                parts = [
                    'name = "' + col + '"',
                    "nullable = " + str(not mandatory).lower(),
                ]
                if col == "id":
                    parts.append("updatable = false")
                if base.startswith("str:"):
                    parts.append("length = " + base.split(":")[1])
                if base.startswith("enum:"):
                    parts.append("length = 40")
                if base == "text":
                    parts.append('columnDefinition = "text"')
                if base == "json":
                    parts.append('columnDefinition = "jsonb"')
                if base in ("qty", "money", "score"):
                    ps = {"qty": (18, 3), "money": (19, 2), "score": (5, 1)}[base]
                    parts.extend(["precision = " + str(ps[0]), "scale = " + str(ps[1])])
                source += "    @Column(" + ", ".join(parts) + ")\n"
            source += "    private " + jt + " " + jn + ";\n\n"
        source += "    public " + cls + "() {}\n\n"
        if is_composite:
            source += "    public " + cls + "(" + cls + "Id id) { this.id = id; }\n\n"
            source += (
                "    public "
                + cls
                + "Id getId() { return id; }\n    public void setId("
                + cls
                + "Id value) { this.id = value; }\n\n"
            )
        for col, desc in fields.items():
            jt = jtype(desc)
            jn = jname(col)
            cap = jn[0].upper() + jn[1:]
            source += "    public " + jt + " get" + cap + "() { return " + jn + "; }\n"
            source += (
                "    public void set"
                + cap
                + "("
                + jt
                + " value) { this."
                + jn
                + " = value; }\n\n"
            )
        # Database defaults are a fallback, entities also initialize standard bookkeeping fields.
        if "created_at" in fields or "updated_at" in fields:
            source += "    @PrePersist\n    private void prePersist() {\n"
            if "created_at" in fields:
                source += "        if (createdAt == null) createdAt = Instant.now();\n"
            if "updated_at" in fields:
                source += "        if (updatedAt == null) updatedAt = Instant.now();\n"
            source += "    }\n\n"
            if "updated_at" in fields:
                source += "    @PreUpdate\n    private void preUpdate() { updatedAt = Instant.now(); }\n\n"
        source += "}\n"
        put(java_path("entity/" + module, cls), source)
        if is_composite:
            source = """package by.bsuir.supplier.entity.%s;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

/** Composite identity; both columns are real foreign keys. */
@Embeddable
public class %sId implements Serializable {
    private static final long serialVersionUID = 1L;
""" % (module, cls)
            for col in pk:
                source += '    @Column(name = "' + col + '", nullable = false)\n'
                source += "    private UUID " + jname(col) + "Id;\n\n"
            source += "    public " + cls + "Id() {}\n"
            vals = ", ".join("UUID " + jname(c) + "Id" for c in pk)
            source += "    public " + cls + "Id(" + vals + ") {\n"
            for col in pk:
                source += "        this." + jname(col) + "Id = " + jname(col) + "Id;\n"
            source += "    }\n"
            for col in pk:
                v = jname(col) + "Id"
                cap = v[0].upper() + v[1:]
                source += "    public UUID get" + cap + "() { return " + v + "; }\n"
                source += (
                    "    public void set"
                    + cap
                    + "(UUID value) { this."
                    + v
                    + " = value; }\n"
                )
            source += "    @Override public boolean equals(Object o) {\n"
            source += (
                "        if (this == o) return true;\n        if (!(o instanceof "
                + cls
                + "Id that)) return false;\n"
            )
            source += (
                "        return "
                + " && ".join(
                    "Objects.equals(" + jname(c) + "Id, that." + jname(c) + "Id)"
                    for c in pk
                )
                + ";\n    }\n"
            )
            source += (
                "    @Override public int hashCode() { return Objects.hash("
                + ", ".join(jname(c) + "Id" for c in pk)
                + "); }\n"
            )
            source += "}\n"
            put(java_path("entity/" + module, cls + "Id"), source)


def emit_repositories():
    for tbl, model in TABLES.items():
        cls = model["cls"]
        module = model["module"]
        pk_type = cls + "Id" if len(model["pk"]) > 1 else "UUID"
        source = "package by.bsuir.supplier.repository." + module + ";\n\n"
        source += "import by.bsuir.supplier.entity." + module + "." + cls + ";\n"
        if len(model["pk"]) > 1:
            source += "import by.bsuir.supplier.entity." + module + "." + cls + "Id;\n"
        else:
            source += "import java.util.UUID;\n"
        source += "import org.springframework.data.jpa.repository.JpaRepository;\n"
        if tbl in (
            "purchase_orders",
            "deliveries",
            "acceptance_documents",
            "supplier_returns",
            "auth_sessions",
        ):
            source += "import org.springframework.data.jpa.repository.Lock;\nimport org.springframework.data.jpa.repository.Query;\nimport jakarta.persistence.LockModeType;\nimport org.springframework.data.repository.query.Param;\nimport java.util.Optional;\n"
        if tbl == "users":
            source += "import java.util.Optional;\n"
        if tbl == "suppliers":
            source += "import org.springframework.data.domain.Page;\nimport org.springframework.data.domain.Pageable;\n"
        source += "\n/** Type-safe persistence gateway. Business validation belongs to service transactions. */\n"
        source += (
            "public interface "
            + cls
            + "Repository extends JpaRepository<"
            + cls
            + ", "
            + pk_type
            + "> {\n"
        )
        if tbl == "users":
            source += "    Optional<User> findByLogin(String login);\n"
        if tbl == "suppliers":
            source += "    Page<Supplier> findByNameContainingIgnoreCase(String name, Pageable pageable);\n"
        if tbl in (
            "purchase_orders",
            "deliveries",
            "acceptance_documents",
            "supplier_returns",
            "auth_sessions",
        ):
            source += "    @Lock(LockModeType.PESSIMISTIC_WRITE)\n"
            source += '    @Query("select e from ' + cls + ' e where e.id = :id")\n'
            source += (
                "    Optional<" + cls + '> findByIdForUpdate(@Param("id") UUID id);\n'
            )
        source += "}\n"
        # Repository interfaces grow custom Spring Data query methods in stage 3.
        # Do not destructively overwrite curated files when regenerating the schema.
        repo_path = java_path("repository/" + module, cls + "Repository")
        if not repo_path.exists():
            put(repo_path, source)


def emit_sql():
    blocks = [
        """-- Approved minimal model v2: 30 domain tables. PostgreSQL 16+.
-- Run on a NEW empty database; not an upgrade of the former 43-table V1.
-- Flyway owns DDL; never enable Hibernate create/update.
"""
    ]
    fks = []
    for tbl, model in TABLES.items():
        decl = []
        for col, desc in model["fields"].items():
            base = desc.rstrip("!")
            mandatory = desc.endswith("!")
            bits = [col, sqltype(desc)]
            if mandatory:
                bits.append("NOT NULL")
            if col == "id":
                bits.append("DEFAULT gen_random_uuid()")
            if col in ("lock_version", "failed_login_count", "token_generation"):
                bits.append("DEFAULT 0")
            if col == "active":
                bits.append("DEFAULT true")
            if col in (
                "created_at",
                "updated_at",
                "registered_at",
                "occurred_at",
                "assigned_at",
                "requested_at",
            ):
                bits.append("DEFAULT CURRENT_TIMESTAMP")
            decl.append("    " + " ".join(bits))
            if base.startswith("ref:"):
                dst = base.split(":", 1)[1]
                fks.append(
                    "ALTER TABLE "
                    + tbl
                    + " ADD CONSTRAINT "
                    + ident("fk", tbl, [col])
                    + " FOREIGN KEY ("
                    + col
                    + ") REFERENCES "
                    + dst
                    + " (id) ON DELETE RESTRICT;"
                )
            if base.startswith("enum:"):
                allowed = ", ".join("'" + x + "'" for x in ENUMS[base.split(":", 1)[1]])
                decl.append(
                    "    CONSTRAINT "
                    + ident("ck", tbl, [col])
                    + " CHECK ("
                    + col
                    + " IN ("
                    + allowed
                    + "))"
                )
            if base == "score":
                decl.append(
                    "    CONSTRAINT "
                    + ident("ck", tbl, [col, "range"])
                    + " CHECK ("
                    + col
                    + " BETWEEN 0 AND 100)"
                )
        decl.append(
            "    CONSTRAINT "
            + ident("pk", tbl, model["pk"])
            + " PRIMARY KEY ("
            + ", ".join(model["pk"])
            + ")"
        )
        for uq in model["unique"]:
            decl.append(
                "    CONSTRAINT "
                + ident("uq", tbl, uq)
                + " UNIQUE ("
                + ", ".join(uq)
                + ")"
            )
        for idx, ck in enumerate(model["checks"]):
            decl.append(
                "    CONSTRAINT "
                + ident("ck", tbl, ["rule", str(idx + 1)])
                + " CHECK ("
                + ck
                + ")"
            )
        blocks.append("CREATE TABLE " + tbl + " (\n" + ",\n".join(decl) + "\n);")
    blocks.append(
        "-- Foreign keys added after tables to support deliberate cycles (current versions, claim links)."
    )
    blocks.extend(fks)
    blocks.append("""-- Current version pointers MUST refer to a version of their own document.
ALTER TABLE contracts ADD CONSTRAINT fk_contract_current_same_document
    FOREIGN KEY (current_registered_version_id, id) REFERENCES contract_versions (id, contract_id)
    DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE purchase_orders ADD CONSTRAINT fk_order_current_same_document
    FOREIGN KEY (current_approved_version_id, id) REFERENCES order_versions (id, order_id)
    DEFERRABLE INITIALLY IMMEDIATE;
""")
    blocks.append("""-- Exactly one current role / one open approval request where required.
CREATE UNIQUE INDEX uq_current_user_role ON user_role_assignments (user_id, role_code) WHERE revoked_at IS NULL;
CREATE UNIQUE INDEX uq_pending_supplier_status ON supplier_status_requests (supplier_id) WHERE state = 'PENDING';
CREATE UNIQUE INDEX uq_open_order_change ON order_versions (order_id) WHERE kind = 'CHANGE' AND state IN ('DRAFT','PENDING');
CREATE UNIQUE INDEX uq_initial_order_version ON order_versions (order_id) WHERE kind = 'INITIAL';
""")
    for tbl, model in TABLES.items():
        for idx in model["indexes"]:
            index = ident("idx", tbl, idx)
            blocks.append(
                "CREATE INDEX " + index + " ON " + tbl + " (" + ", ".join(idx) + ");"
            )
    blocks.append("""-- Simple cross-field state constraints applied on the governing document.
ALTER TABLE order_versions ADD CONSTRAINT ck_version_change_requires_base
    CHECK ((kind='INITIAL' AND base_version_id IS NULL) OR
           (kind='CHANGE' AND base_version_id IS NOT NULL AND initiator IS NOT NULL AND reason IS NOT NULL));
ALTER TABLE order_versions ADD CONSTRAINT ck_approved_version_has_contract
    CHECK (state <> 'APPROVED' OR contract_version_id IS NOT NULL);
ALTER TABLE purchase_orders ADD CONSTRAINT ck_submitted_order_has_contract
    CHECK (state = 'DRAFT' OR contract_id IS NOT NULL);
ALTER TABLE purchase_orders ADD CONSTRAINT ck_approved_order_has_version
    CHECK (state NOT IN ('APPROVED','IN_PROGRESS','COMPLETED') OR current_approved_version_id IS NOT NULL);
""")
    blocks.append(CUSTOM_INVARIANTS)
    put(RES / "V1__optimized_schema.sql", "\n\n".join(blocks))


CUSTOM_INVARIANTS = r"""
-- Cross-document consistency not expressible by a plain single-column FK.
CREATE FUNCTION enforce_cross_document_links() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_TABLE_NAME = 'purchase_orders' THEN
        IF NEW.current_approved_version_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM order_versions ov WHERE ov.id = NEW.current_approved_version_id AND ov.order_id = NEW.id AND ov.state = 'APPROVED') THEN RAISE EXCEPTION 'Current order version must be approved and belong to order' USING ERRCODE = '23514'; END IF;
        IF NEW.contract_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM contracts c WHERE c.id = NEW.contract_id AND c.supplier_id = NEW.supplier_id
        ) THEN RAISE EXCEPTION 'Order contract must belong to the same supplier' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'contracts' THEN
        IF NEW.current_registered_version_id IS NOT NULL AND NOT EXISTS (SELECT 1 FROM contract_versions cv WHERE cv.id = NEW.current_registered_version_id AND cv.contract_id = NEW.id AND cv.state = 'APPROVED') THEN RAISE EXCEPTION 'Current contract version must be approved and belong to contract' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'order_versions' THEN
        IF NEW.base_version_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM order_versions p WHERE p.id=NEW.base_version_id AND p.order_id=NEW.order_id
        ) THEN RAISE EXCEPTION 'Base version belongs to another order' USING ERRCODE = '23514'; END IF;
        IF NEW.contract_version_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM purchase_orders o JOIN contract_versions cv ON cv.contract_id=o.contract_id
            WHERE o.id=NEW.order_id AND cv.id=NEW.contract_version_id
        ) THEN RAISE EXCEPTION 'Contract version does not belong to order contract' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'order_version_lines' THEN
        IF NOT EXISTS (
            SELECT 1 FROM order_versions v JOIN order_lines l ON l.order_id=v.order_id
            WHERE v.id=NEW.order_version_id AND l.id=NEW.order_line_id
        ) THEN RAISE EXCEPTION 'Version segment belongs to another order' USING ERRCODE = '23514'; END IF;
        IF NEW.source_version_line_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM order_version_lines source
            JOIN order_versions sv ON sv.id=source.order_version_id
            JOIN order_versions tv ON tv.id=NEW.order_version_id
            WHERE source.id=NEW.source_version_line_id
              AND source.order_line_id=NEW.order_line_id
              AND sv.order_id=tv.order_id AND sv.version_no < tv.version_no
        ) THEN RAISE EXCEPTION 'Segment source must be from earlier version of same line' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'order_closure_lines' THEN
        IF NOT EXISTS (
            SELECT 1 FROM order_closures c JOIN order_lines l ON l.order_id=c.order_id
            WHERE c.id=NEW.closure_id AND l.id=NEW.order_line_id AND c.action='CLOSE'
        ) THEN RAISE EXCEPTION 'Closure line must belong to closing order' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'deliveries' THEN
        IF NEW.supersedes_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM deliveries prev WHERE prev.id=NEW.supersedes_id AND prev.order_id=NEW.order_id
        ) THEN RAISE EXCEPTION 'Corrected delivery must belong to same order' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'delivery_lines' THEN
        IF NOT EXISTS (
            SELECT 1 FROM deliveries d
            JOIN order_versions v ON v.order_id=d.order_id
            JOIN order_version_lines vl ON vl.order_version_id=v.id
            WHERE d.id=NEW.delivery_id AND vl.id=NEW.order_version_line_id AND v.state='APPROVED'
        ) THEN RAISE EXCEPTION 'Delivered segment must be approved and belong to delivery order' USING ERRCODE = '23514'; END IF;
        IF NEW.replacement_for_line_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM delivery_lines previous
            JOIN order_version_lines from_segment ON from_segment.id=previous.order_version_line_id
            JOIN order_version_lines new_segment ON new_segment.id=NEW.order_version_line_id
            WHERE previous.id=NEW.replacement_for_line_id
              AND from_segment.order_line_id=new_segment.order_line_id
        ) THEN RAISE EXCEPTION 'Replacement must target the same order line' USING ERRCODE = '23514'; END IF;
        IF NEW.claim_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM claims c
            JOIN order_lines l ON l.id=c.order_line_id
            JOIN order_version_lines vl ON vl.order_line_id=l.id
            WHERE c.id=NEW.claim_id AND vl.id=NEW.order_version_line_id
        ) THEN RAISE EXCEPTION 'Remedy claim must relate to the same order line' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'acceptance_documents' THEN
        IF NEW.supersedes_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM acceptance_documents prev WHERE prev.id=NEW.supersedes_id AND prev.delivery_id=NEW.delivery_id
        ) THEN RAISE EXCEPTION 'Corrected acceptance belongs to another delivery' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'acceptance_lines' THEN
        IF NOT EXISTS (
            SELECT 1 FROM acceptance_documents a JOIN delivery_lines dl ON dl.delivery_id=a.delivery_id
            WHERE a.id=NEW.acceptance_document_id AND dl.id=NEW.delivery_line_id
        ) THEN RAISE EXCEPTION 'Acceptance line belongs to another delivery' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'supplier_returns' THEN
        IF NEW.supersedes_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM supplier_returns prev WHERE prev.id=NEW.supersedes_id AND prev.order_id=NEW.order_id
        ) THEN RAISE EXCEPTION 'Corrected return belongs to another order' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'supplier_return_lines' THEN
        IF NOT EXISTS (
            SELECT 1 FROM supplier_returns r
            JOIN deliveries d ON d.order_id=r.order_id
            JOIN acceptance_documents a ON a.delivery_id=d.id
            JOIN acceptance_lines al ON al.acceptance_document_id=a.id
            WHERE r.id=NEW.return_id AND al.id=NEW.acceptance_line_id
        ) THEN RAISE EXCEPTION 'Returned acceptance belongs to another order' USING ERRCODE = '23514'; END IF;
        IF NEW.claim_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM claims c
            JOIN supplier_returns r ON r.order_id=(SELECT order_id FROM order_lines WHERE id=c.order_line_id)
            JOIN acceptance_lines al ON al.id=NEW.acceptance_line_id
            JOIN delivery_lines dl ON dl.id=al.delivery_line_id
            JOIN order_version_lines vl ON vl.id=dl.order_version_line_id
            WHERE r.id=NEW.return_id AND c.id=NEW.claim_id AND vl.order_line_id=c.order_line_id
        ) THEN RAISE EXCEPTION 'Return claim belongs to another order line' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'claims' THEN
        IF NOT EXISTS (
            SELECT 1 FROM order_lines l JOIN purchase_orders o ON o.id=l.order_id
            WHERE l.id=NEW.order_line_id AND o.supplier_id=NEW.supplier_id
        ) THEN RAISE EXCEPTION 'Claim line belongs to another supplier' USING ERRCODE = '23514'; END IF;
        IF NEW.delivery_line_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM delivery_lines dl JOIN order_version_lines vl ON vl.id=dl.order_version_line_id
            WHERE dl.id=NEW.delivery_line_id AND vl.order_line_id=NEW.order_line_id
        ) THEN RAISE EXCEPTION 'Claim delivery line differs from claim order line' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'interactions' THEN
        IF NEW.claim_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM claims c WHERE c.id=NEW.claim_id AND c.supplier_id=NEW.supplier_id
        ) THEN RAISE EXCEPTION 'Interaction claim belongs to another supplier' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'supplier_ratings' THEN
        IF NEW.expert_assessment_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM expert_assessments ea WHERE ea.id=NEW.expert_assessment_id AND ea.supplier_id=NEW.supplier_id AND ea.state='APPROVED'
        ) THEN RAISE EXCEPTION 'Rating expert assessment belongs to another supplier' USING ERRCODE = '23514'; END IF;
        IF NEW.supersedes_id IS NOT NULL AND NOT EXISTS (
            SELECT 1 FROM supplier_ratings prev WHERE prev.id=NEW.supersedes_id
            AND prev.supplier_id=NEW.supplier_id AND prev.scope=NEW.scope
            AND prev.category_id IS NOT DISTINCT FROM NEW.category_id
        ) THEN RAISE EXCEPTION 'Superseded rating must cover same supplier and scope' USING ERRCODE = '23514'; END IF;

    ELSIF TG_TABLE_NAME = 'rating_line_inputs' THEN
        IF EXISTS (SELECT 1 FROM supplier_ratings r WHERE r.id=NEW.rating_id AND NEW.source_cutoff_at > r.as_of_at) THEN RAISE EXCEPTION 'Input cutoff cannot exceed rating snapshot date' USING ERRCODE='23514'; END IF;
        IF NOT EXISTS (
            SELECT 1 FROM supplier_ratings r
            JOIN order_versions v ON true
            JOIN order_version_lines vl ON vl.order_version_id=v.id
            JOIN purchase_orders o ON o.id=v.order_id AND o.supplier_id=r.supplier_id
            WHERE r.id=NEW.rating_id AND vl.id=NEW.order_version_line_id AND v.state='APPROVED'
        ) THEN RAISE EXCEPTION 'Rating input belongs to another supplier' USING ERRCODE = '23514'; END IF;
    END IF;
    RETURN NEW;
END $$;

-- Each trigger validates ownership while a record is inserted or changed.
CREATE TRIGGER check_contract_version BEFORE INSERT OR UPDATE ON contracts FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_order_contract BEFORE INSERT OR UPDATE ON purchase_orders FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_order_base BEFORE INSERT OR UPDATE ON order_versions FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_order_segment BEFORE INSERT OR UPDATE ON order_version_lines FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_closure_line BEFORE INSERT OR UPDATE ON order_closure_lines FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_delivery BEFORE INSERT OR UPDATE ON deliveries FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_delivery_line BEFORE INSERT OR UPDATE ON delivery_lines FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_acceptance BEFORE INSERT OR UPDATE ON acceptance_documents FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_acceptance_line BEFORE INSERT OR UPDATE ON acceptance_lines FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_supplier_return BEFORE INSERT OR UPDATE ON supplier_returns FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_supplier_return_line BEFORE INSERT OR UPDATE ON supplier_return_lines FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_claim BEFORE INSERT OR UPDATE ON claims FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_interaction BEFORE INSERT OR UPDATE ON interactions FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_rating BEFORE INSERT OR UPDATE ON supplier_ratings FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();
CREATE TRIGGER check_rating_input BEFORE INSERT OR UPDATE ON rating_line_inputs FOR EACH ROW EXECUTE FUNCTION enforce_cross_document_links();

-- Materialized business decisions cannot be silently rewritten or deleted.
CREATE FUNCTION guard_finalized_record() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'DELETE' THEN
        IF OLD.state IN ('APPROVED','REJECTED','CONFIRMED','CLOSED','CANCELLED') THEN
            RAISE EXCEPTION 'Cannot delete finalized %', TG_TABLE_NAME USING ERRCODE = '23514';
        END IF;
        RETURN OLD;
    END IF;
    IF OLD.state IN ('APPROVED','REJECTED','CONFIRMED','CLOSED','CANCELLED') THEN
        RAISE EXCEPTION 'Cannot modify finalized %', TG_TABLE_NAME USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END $$;
CREATE TRIGGER finalized_contract_version BEFORE UPDATE OR DELETE ON contract_versions FOR EACH ROW EXECUTE FUNCTION guard_finalized_record();
CREATE TRIGGER finalized_order_version BEFORE UPDATE OR DELETE ON order_versions FOR EACH ROW EXECUTE FUNCTION guard_finalized_record();
CREATE TRIGGER finalized_delivery BEFORE UPDATE OR DELETE ON deliveries FOR EACH ROW EXECUTE FUNCTION guard_finalized_record();
CREATE TRIGGER finalized_acceptance BEFORE UPDATE OR DELETE ON acceptance_documents FOR EACH ROW EXECUTE FUNCTION guard_finalized_record();
CREATE TRIGGER finalized_return BEFORE UPDATE OR DELETE ON supplier_returns FOR EACH ROW EXECUTE FUNCTION guard_finalized_record();
CREATE TRIGGER finalized_expert BEFORE UPDATE OR DELETE ON expert_assessments FOR EACH ROW EXECUTE FUNCTION guard_finalized_record();
CREATE TRIGGER finalized_fact_correction BEFORE UPDATE OR DELETE ON fact_corrections FOR EACH ROW EXECUTE FUNCTION guard_finalized_record();
CREATE TRIGGER finalized_claim BEFORE UPDATE OR DELETE ON claims FOR EACH ROW EXECUTE FUNCTION guard_finalized_record();

CREATE FUNCTION guard_terminal_request() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    IF OLD.state IN ('APPROVED','REJECTED') THEN
        RAISE EXCEPTION 'Cannot modify decided supplier status request' USING ERRCODE='23514';
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER finalized_status_request BEFORE UPDATE OR DELETE ON supplier_status_requests FOR EACH ROW EXECUTE FUNCTION guard_terminal_request();

CREATE FUNCTION guard_approved_children() RETURNS trigger LANGUAGE plpgsql AS $$
DECLARE parent_state text;
DECLARE parent_id uuid;
BEGIN
    IF TG_TABLE_NAME = 'order_version_lines' THEN
        IF TG_OP = 'UPDATE' THEN
            IF OLD.order_version_id <> NEW.order_version_id THEN
                RAISE EXCEPTION 'Changing owning document is forbidden' USING ERRCODE='23514';
            END IF;
        END IF;
        parent_id := CASE WHEN TG_OP='DELETE' THEN OLD.order_version_id ELSE NEW.order_version_id END;
        SELECT state INTO parent_state FROM order_versions WHERE id=parent_id;
        IF parent_state IN ('PENDING','APPROVED','REJECTED') THEN
            RAISE EXCEPTION 'Cannot change lines of submitted order version' USING ERRCODE='23514';
        END IF;
    ELSIF TG_TABLE_NAME = 'delivery_lines' THEN
        IF TG_OP = 'UPDATE' THEN
            IF OLD.delivery_id <> NEW.delivery_id THEN
                RAISE EXCEPTION 'Changing owning document is forbidden' USING ERRCODE='23514';
            END IF;
        END IF;
        parent_id := CASE WHEN TG_OP='DELETE' THEN OLD.delivery_id ELSE NEW.delivery_id END;
        SELECT state INTO parent_state FROM deliveries WHERE id=parent_id;
        IF parent_state = 'CONFIRMED' THEN
            RAISE EXCEPTION 'Cannot change lines of confirmed delivery' USING ERRCODE='23514';
        END IF;
    ELSIF TG_TABLE_NAME = 'acceptance_lines' THEN
        IF TG_OP = 'UPDATE' THEN
            IF OLD.acceptance_document_id <> NEW.acceptance_document_id THEN
                RAISE EXCEPTION 'Changing owning document is forbidden' USING ERRCODE='23514';
            END IF;
        END IF;
        parent_id := CASE WHEN TG_OP='DELETE' THEN OLD.acceptance_document_id ELSE NEW.acceptance_document_id END;
        SELECT state INTO parent_state FROM acceptance_documents WHERE id=parent_id;
        IF parent_state = 'CONFIRMED' THEN
            RAISE EXCEPTION 'Cannot change lines of confirmed acceptance' USING ERRCODE='23514';
        END IF;
    ELSIF TG_TABLE_NAME = 'supplier_return_lines' THEN
        IF TG_OP = 'UPDATE' THEN
            IF OLD.return_id <> NEW.return_id THEN
                RAISE EXCEPTION 'Changing owning document is forbidden' USING ERRCODE='23514';
            END IF;
        END IF;
        parent_id := CASE WHEN TG_OP='DELETE' THEN OLD.return_id ELSE NEW.return_id END;
        SELECT state INTO parent_state FROM supplier_returns WHERE id=parent_id;
        IF parent_state IN ('PENDING','CONFIRMED','REJECTED') THEN
            RAISE EXCEPTION 'Cannot change lines of submitted return' USING ERRCODE='23514';
        END IF;
    END IF;
    IF TG_OP='DELETE' THEN RETURN OLD; ELSE RETURN NEW; END IF;
END $$;
CREATE TRIGGER lock_version_lines BEFORE INSERT OR UPDATE OR DELETE ON order_version_lines FOR EACH ROW EXECUTE FUNCTION guard_approved_children();
CREATE TRIGGER lock_delivery_lines BEFORE INSERT OR UPDATE OR DELETE ON delivery_lines FOR EACH ROW EXECUTE FUNCTION guard_approved_children();
CREATE TRIGGER lock_acceptance_lines BEFORE INSERT OR UPDATE OR DELETE ON acceptance_lines FOR EACH ROW EXECUTE FUNCTION guard_approved_children();
CREATE TRIGGER lock_return_lines BEFORE INSERT OR UPDATE OR DELETE ON supplier_return_lines FOR EACH ROW EXECUTE FUNCTION guard_approved_children();

CREATE FUNCTION reject_append_only_mutation() RETURNS trigger LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'Append-only table % cannot be modified or deleted', TG_TABLE_NAME USING ERRCODE='23514';
END $$;
CREATE TRIGGER immutable_closures BEFORE UPDATE OR DELETE ON order_closures FOR EACH ROW EXECUTE FUNCTION reject_append_only_mutation();
CREATE TRIGGER immutable_closure_lines BEFORE UPDATE OR DELETE ON order_closure_lines FOR EACH ROW EXECUTE FUNCTION reject_append_only_mutation();
CREATE TRIGGER immutable_ratings BEFORE UPDATE OR DELETE ON supplier_ratings FOR EACH ROW EXECUTE FUNCTION reject_append_only_mutation();
CREATE TRIGGER immutable_rating_inputs BEFORE UPDATE OR DELETE ON rating_line_inputs FOR EACH ROW EXECUTE FUNCTION reject_append_only_mutation();
CREATE TRIGGER immutable_audit BEFORE UPDATE OR DELETE ON audit_events FOR EACH ROW EXECUTE FUNCTION reject_append_only_mutation();
"""

if __name__ == "__main__":
    emit_entities()
    emit_repositories()
    emit_sql()
    manifest = {
        "schema_version": 2,
        "tables": TABLES,
        "enums": ENUMS,
        "required_extra_entities": [
            "supplier_status_requests",
            "order_closure_lines",
            "fact_corrections",
            "rating_line_inputs",
        ],
    }
    put(
        ROOT / "docs/schema-manifest.json",
        json.dumps(manifest, ensure_ascii=False, indent=2),
    )
    print(
        "Generated",
        len(TABLES),
        "tables/entities",
        len(ENUMS),
        "enums,",
        sum(
            1
            for model in TABLES.values()
            for v in model["fields"].values()
            if v.rstrip("!").startswith("ref:")
        ),
        "foreign keys",
    )
