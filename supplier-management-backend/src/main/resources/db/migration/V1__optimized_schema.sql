-- Approved minimal model v2: 30 domain tables. PostgreSQL 16+.
-- Run on a NEW empty database; not an upgrade of the former 43-table V1.
-- Flyway owns DDL; never enable Hibernate create/update.


CREATE TABLE users (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    login varchar(80) NOT NULL,
    password_hash varchar(255) NOT NULL,
    display_name varchar(200) NOT NULL,
    status varchar(40) NOT NULL,
    CONSTRAINT ck_users_status CHECK (status IN ('ACTIVE', 'BLOCKED')),
    failed_login_count integer NOT NULL DEFAULT 0,
    locked_until timestamptz,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_users_id PRIMARY KEY (id),
    CONSTRAINT uq_users_login UNIQUE (login),
    CONSTRAINT ck_users_rule_1 CHECK (failed_login_count >= 0)
);

CREATE TABLE user_role_assignments (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    role_code varchar(40) NOT NULL,
    CONSTRAINT ck_user_role_assignments_role_code CHECK (role_code IN ('PROCUREMENT_SPECIALIST', 'PROCUREMENT_MANAGER', 'ADMINISTRATOR')),
    assigned_by_id uuid NOT NULL,
    assigned_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    revoked_at timestamptz,
    CONSTRAINT pk_user_role_assignments_id PRIMARY KEY (id),
    CONSTRAINT ck_user_role_assignments_rule_1 CHECK (revoked_at IS NULL OR revoked_at >= assigned_at)
);

CREATE TABLE auth_sessions (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    user_id uuid NOT NULL,
    refresh_token_hash varchar(128) NOT NULL,
    token_generation bigint NOT NULL DEFAULT 0,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at timestamptz NOT NULL,
    revoked_at timestamptz,
    rotated_at timestamptz,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_auth_sessions_id PRIMARY KEY (id),
    CONSTRAINT uq_auth_sessions_refresh_token_hash UNIQUE (refresh_token_hash),
    CONSTRAINT ck_auth_sessions_rule_1 CHECK (token_generation >= 0),
    CONSTRAINT ck_auth_sessions_rule_2 CHECK (expires_at > created_at)
);

CREATE TABLE product_categories (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    code varchar(50) NOT NULL,
    name varchar(150) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_product_categories_id PRIMARY KEY (id),
    CONSTRAINT uq_product_categories_code UNIQUE (code)
);

CREATE TABLE units (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    code varchar(20) NOT NULL,
    name varchar(80) NOT NULL,
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_units_id PRIMARY KEY (id),
    CONSTRAINT uq_units_code UNIQUE (code)
);

CREATE TABLE products (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    sku varchar(80) NOT NULL,
    name varchar(250) NOT NULL,
    category_id uuid NOT NULL,
    unit_id uuid NOT NULL,
    active boolean NOT NULL DEFAULT true,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_products_id PRIMARY KEY (id),
    CONSTRAINT uq_products_sku UNIQUE (sku)
);

CREATE TABLE suppliers (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    name varchar(250) NOT NULL,
    contact_name varchar(150),
    email varchar(250),
    phone varchar(60),
    address text,
    registration_number varchar(80),
    status varchar(40) NOT NULL,
    CONSTRAINT ck_suppliers_status CHECK (status IN ('CANDIDATE', 'ACTIVE', 'SUSPENDED', 'TERMINATED')),
    created_by_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_suppliers_id PRIMARY KEY (id)
);

CREATE TABLE supplier_products (
    supplier_id uuid NOT NULL,
    product_id uuid NOT NULL,
    supplier_item_code varchar(80),
    active boolean NOT NULL DEFAULT true,
    CONSTRAINT pk_supplier_products_supplier_id_product_id PRIMARY KEY (supplier_id, product_id)
);

CREATE TABLE supplier_status_requests (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    supplier_id uuid NOT NULL,
    from_status varchar(40) NOT NULL,
    CONSTRAINT ck_supplier_status_requests_from_status CHECK (from_status IN ('CANDIDATE', 'ACTIVE', 'SUSPENDED', 'TERMINATED')),
    to_status varchar(40) NOT NULL,
    CONSTRAINT ck_supplier_status_requests_to_status CHECK (to_status IN ('CANDIDATE', 'ACTIVE', 'SUSPENDED', 'TERMINATED')),
    reason text NOT NULL,
    requested_by_id uuid NOT NULL,
    requested_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_supplier_status_requests_state CHECK (state IN ('PENDING', 'APPROVED', 'REJECTED')),
    decided_by_id uuid,
    decided_at timestamptz,
    decision_comment text,
    CONSTRAINT pk_supplier_status_requests_id PRIMARY KEY (id),
    CONSTRAINT ck_supplier_status_requests_rule_1 CHECK (from_status <> to_status),
    CONSTRAINT ck_supplier_status_requests_rule_2 CHECK (requested_by_id IS DISTINCT FROM decided_by_id),
    CONSTRAINT ck_supplier_status_requests_rule_3 CHECK ((state = 'PENDING' AND decided_by_id IS NULL AND decided_at IS NULL) OR (state IN ('APPROVED','REJECTED') AND decided_by_id IS NOT NULL AND decided_at IS NOT NULL))
);

CREATE TABLE contracts (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    supplier_id uuid NOT NULL,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_contracts_state CHECK (state IN ('DRAFT', 'REGISTERED', 'TERMINATED')),
    current_registered_version_id uuid,
    created_by_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_contracts_id PRIMARY KEY (id)
);

CREATE TABLE contract_versions (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    contract_id uuid NOT NULL,
    version_no integer NOT NULL,
    number varchar(100) NOT NULL,
    signed_on date,
    valid_from date NOT NULL,
    valid_to date NOT NULL,
    terms text NOT NULL,
    change_reason text,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_contract_versions_state CHECK (state IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
    created_by_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_by_id uuid,
    decided_at timestamptz,
    decision_comment text,
    CONSTRAINT pk_contract_versions_id PRIMARY KEY (id),
    CONSTRAINT uq_contract_versions_contract_id_version_no UNIQUE (contract_id, version_no),
    CONSTRAINT uq_contract_versions_id_contract_id UNIQUE (id, contract_id),
    CONSTRAINT ck_contract_versions_rule_1 CHECK (version_no > 0),
    CONSTRAINT ck_contract_versions_rule_2 CHECK (valid_from <= valid_to),
    CONSTRAINT ck_contract_versions_rule_3 CHECK (created_by_id IS DISTINCT FROM decided_by_id),
    CONSTRAINT ck_contract_versions_rule_4 CHECK ((state IN ('DRAFT','PENDING') AND decided_by_id IS NULL AND decided_at IS NULL) OR (state IN ('APPROVED','REJECTED') AND decided_by_id IS NOT NULL AND decided_at IS NOT NULL))
);

CREATE TABLE purchase_orders (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    number varchar(80) NOT NULL,
    supplier_id uuid NOT NULL,
    contract_id uuid,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_purchase_orders_state CHECK (state IN ('DRAFT', 'PENDING_APPROVAL', 'APPROVED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
    current_approved_version_id uuid,
    created_by_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_purchase_orders_id PRIMARY KEY (id),
    CONSTRAINT uq_purchase_orders_number UNIQUE (number)
);

CREATE TABLE order_lines (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL,
    line_no integer NOT NULL,
    product_id uuid NOT NULL,
    CONSTRAINT pk_order_lines_id PRIMARY KEY (id),
    CONSTRAINT uq_order_lines_order_id_line_no UNIQUE (order_id, line_no),
    CONSTRAINT uq_order_lines_id_order_id UNIQUE (id, order_id),
    CONSTRAINT ck_order_lines_rule_1 CHECK (line_no > 0)
);

CREATE TABLE order_versions (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL,
    version_no integer NOT NULL,
    base_version_id uuid,
    contract_version_id uuid,
    kind varchar(40) NOT NULL,
    CONSTRAINT ck_order_versions_kind CHECK (kind IN ('INITIAL', 'CHANGE')),
    initiator varchar(40),
    CONSTRAINT ck_order_versions_initiator CHECK (initiator IN ('BUYER', 'SUPPLIER', 'CORRECTION')),
    reason text,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_order_versions_state CHECK (state IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
    created_by_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    submitted_at timestamptz,
    decided_by_id uuid,
    decided_at timestamptz,
    decision_comment text,
    CONSTRAINT pk_order_versions_id PRIMARY KEY (id),
    CONSTRAINT uq_order_versions_order_id_version_no UNIQUE (order_id, version_no),
    CONSTRAINT uq_order_versions_id_order_id UNIQUE (id, order_id),
    CONSTRAINT ck_order_versions_rule_1 CHECK (version_no > 0),
    CONSTRAINT ck_order_versions_rule_2 CHECK (created_by_id IS DISTINCT FROM decided_by_id),
    CONSTRAINT ck_order_versions_rule_3 CHECK ((state IN ('DRAFT','PENDING') AND decided_by_id IS NULL AND decided_at IS NULL) OR (state IN ('APPROVED','REJECTED') AND decided_by_id IS NOT NULL AND decided_at IS NOT NULL))
);

CREATE TABLE order_version_lines (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    order_version_id uuid NOT NULL,
    order_line_id uuid NOT NULL,
    segment_key varchar(64) NOT NULL,
    source_version_line_id uuid,
    quantity numeric(18,3) NOT NULL,
    unit_price numeric(19,2) NOT NULL,
    execution_due_on date NOT NULL,
    rating_due_on date NOT NULL,
    origin_reason varchar(40) NOT NULL,
    CONSTRAINT ck_order_version_lines_origin_reason CHECK (origin_reason IN ('INITIAL', 'BUYER_CHANGE', 'SUPPLIER_RESCHEDULE', 'CORRECTION')),
    product_name_snapshot varchar(250) NOT NULL,
    category_code_snapshot varchar(50) NOT NULL,
    category_name_snapshot varchar(150) NOT NULL,
    unit_code_snapshot varchar(20) NOT NULL,
    CONSTRAINT pk_order_version_lines_id PRIMARY KEY (id),
    CONSTRAINT uq_order_version_lines_order_version_id_segment_key UNIQUE (order_version_id, segment_key),
    CONSTRAINT ck_order_version_lines_rule_1 CHECK (quantity > 0),
    CONSTRAINT ck_order_version_lines_rule_2 CHECK (unit_price >= 0),
    CONSTRAINT ck_order_version_lines_rule_3 CHECK (source_version_line_id IS DISTINCT FROM id)
);

CREATE TABLE order_closures (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL,
    action varchar(40) NOT NULL,
    CONSTRAINT ck_order_closures_action CHECK (action IN ('CLOSE', 'REOPEN')),
    result varchar(40),
    CONSTRAINT ck_order_closures_result CHECK (result IN ('FULFILLED', 'CLOSED_WITH_REMAINDER')),
    reason text,
    requested_by_id uuid,
    decided_by_id uuid,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_order_closures_id PRIMARY KEY (id),
    CONSTRAINT ck_order_closures_rule_1 CHECK ((action = 'CLOSE' AND result IS NOT NULL) OR (action = 'REOPEN' AND result IS NULL)),
    CONSTRAINT ck_order_closures_rule_2 CHECK ((result IS DISTINCT FROM 'CLOSED_WITH_REMAINDER') OR (reason IS NOT NULL AND decided_by_id IS NOT NULL)),
    CONSTRAINT ck_order_closures_rule_3 CHECK (requested_by_id IS DISTINCT FROM decided_by_id)
);

CREATE TABLE order_closure_lines (
    closure_id uuid NOT NULL,
    order_line_id uuid NOT NULL,
    ordered_qty numeric(18,3) NOT NULL,
    fulfilled_qty numeric(18,3) NOT NULL,
    unfulfilled_qty numeric(18,3) NOT NULL,
    CONSTRAINT pk_order_closure_lines_closure_id_order_line_id PRIMARY KEY (closure_id, order_line_id),
    CONSTRAINT ck_order_closure_lines_rule_1 CHECK (ordered_qty >= 0),
    CONSTRAINT ck_order_closure_lines_rule_2 CHECK (fulfilled_qty >= 0),
    CONSTRAINT ck_order_closure_lines_rule_3 CHECK (unfulfilled_qty >= 0),
    CONSTRAINT ck_order_closure_lines_rule_4 CHECK (ordered_qty = fulfilled_qty + unfulfilled_qty)
);

CREATE TABLE deliveries (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL,
    arrived_on date NOT NULL,
    registered_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    registered_by_id uuid NOT NULL,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_deliveries_state CHECK (state IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    external_number varchar(100),
    supersedes_id uuid,
    correction_id uuid,
    CONSTRAINT pk_deliveries_id PRIMARY KEY (id),
    CONSTRAINT ck_deliveries_rule_1 CHECK (supersedes_id IS DISTINCT FROM id),
    CONSTRAINT ck_deliveries_rule_2 CHECK ((correction_id IS NULL AND supersedes_id IS NULL) OR (correction_id IS NOT NULL AND supersedes_id IS NOT NULL))
);

CREATE TABLE delivery_lines (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    delivery_id uuid NOT NULL,
    order_version_line_id uuid NOT NULL,
    quantity numeric(18,3) NOT NULL,
    replacement_for_line_id uuid,
    claim_id uuid,
    CONSTRAINT pk_delivery_lines_id PRIMARY KEY (id),
    CONSTRAINT ck_delivery_lines_rule_1 CHECK (quantity > 0),
    CONSTRAINT ck_delivery_lines_rule_2 CHECK (replacement_for_line_id IS DISTINCT FROM id)
);

CREATE TABLE acceptance_documents (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    delivery_id uuid NOT NULL,
    inspected_on date NOT NULL,
    registered_by_id uuid NOT NULL,
    registered_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_acceptance_documents_state CHECK (state IN ('DRAFT', 'CONFIRMED', 'CANCELLED')),
    supersedes_id uuid,
    correction_id uuid,
    CONSTRAINT pk_acceptance_documents_id PRIMARY KEY (id),
    CONSTRAINT ck_acceptance_documents_rule_1 CHECK (supersedes_id IS DISTINCT FROM id),
    CONSTRAINT ck_acceptance_documents_rule_2 CHECK ((correction_id IS NULL AND supersedes_id IS NULL) OR (correction_id IS NOT NULL AND supersedes_id IS NOT NULL))
);

CREATE TABLE acceptance_lines (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    acceptance_document_id uuid NOT NULL,
    delivery_line_id uuid NOT NULL,
    accepted_qty numeric(18,3) NOT NULL,
    quality_rejected_qty numeric(18,3) NOT NULL,
    other_rejected_qty numeric(18,3) NOT NULL,
    other_reason text,
    CONSTRAINT pk_acceptance_lines_id PRIMARY KEY (id),
    CONSTRAINT ck_acceptance_lines_rule_1 CHECK (accepted_qty >= 0),
    CONSTRAINT ck_acceptance_lines_rule_2 CHECK (quality_rejected_qty >= 0),
    CONSTRAINT ck_acceptance_lines_rule_3 CHECK (other_rejected_qty >= 0),
    CONSTRAINT ck_acceptance_lines_rule_4 CHECK (accepted_qty + quality_rejected_qty + other_rejected_qty > 0),
    CONSTRAINT ck_acceptance_lines_rule_5 CHECK (other_rejected_qty = 0 OR other_reason IS NOT NULL)
);

CREATE TABLE supplier_returns (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    order_id uuid NOT NULL,
    returned_on date NOT NULL,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_supplier_returns_state CHECK (state IN ('DRAFT', 'PENDING', 'CONFIRMED', 'REJECTED')),
    reason text NOT NULL,
    created_by_id uuid NOT NULL,
    approved_by_id uuid,
    approved_at timestamptz,
    supersedes_id uuid,
    correction_id uuid,
    CONSTRAINT pk_supplier_returns_id PRIMARY KEY (id),
    CONSTRAINT ck_supplier_returns_rule_1 CHECK (created_by_id IS DISTINCT FROM approved_by_id),
    CONSTRAINT ck_supplier_returns_rule_2 CHECK (supersedes_id IS DISTINCT FROM id),
    CONSTRAINT ck_supplier_returns_rule_3 CHECK ((correction_id IS NULL AND supersedes_id IS NULL) OR (correction_id IS NOT NULL AND supersedes_id IS NOT NULL)),
    CONSTRAINT ck_supplier_returns_rule_4 CHECK ((state IN ('DRAFT','PENDING','REJECTED') AND approved_by_id IS NULL AND approved_at IS NULL) OR (state = 'CONFIRMED' AND approved_by_id IS NOT NULL AND approved_at IS NOT NULL))
);

CREATE TABLE supplier_return_lines (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    return_id uuid NOT NULL,
    acceptance_line_id uuid NOT NULL,
    quantity numeric(18,3) NOT NULL,
    defect_description text NOT NULL,
    claim_id uuid,
    CONSTRAINT pk_supplier_return_lines_id PRIMARY KEY (id),
    CONSTRAINT ck_supplier_return_lines_rule_1 CHECK (quantity > 0)
);

CREATE TABLE fact_corrections (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    reason text NOT NULL,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_fact_corrections_state CHECK (state IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
    effective_on date NOT NULL,
    created_by_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    decided_by_id uuid,
    decided_at timestamptz,
    decision_comment text,
    CONSTRAINT pk_fact_corrections_id PRIMARY KEY (id),
    CONSTRAINT ck_fact_corrections_rule_1 CHECK (created_by_id IS DISTINCT FROM decided_by_id),
    CONSTRAINT ck_fact_corrections_rule_2 CHECK ((state IN ('DRAFT','PENDING') AND decided_by_id IS NULL AND decided_at IS NULL) OR (state IN ('APPROVED','REJECTED') AND decided_by_id IS NOT NULL AND decided_at IS NOT NULL))
);

CREATE TABLE interactions (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    supplier_id uuid NOT NULL,
    employee_id uuid NOT NULL,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    channel varchar(40) NOT NULL,
    CONSTRAINT ck_interactions_channel CHECK (channel IN ('EMAIL', 'PHONE', 'MEETING', 'MESSENGER', 'OTHER')),
    subject varchar(250) NOT NULL,
    content text NOT NULL,
    outcome text,
    claim_id uuid,
    CONSTRAINT pk_interactions_id PRIMARY KEY (id)
);

CREATE TABLE claims (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    supplier_id uuid NOT NULL,
    number varchar(80) NOT NULL,
    order_line_id uuid NOT NULL,
    delivery_line_id uuid,
    violation_type varchar(40) NOT NULL,
    CONSTRAINT ck_claims_violation_type CHECK (violation_type IN ('LATE', 'SHORTAGE', 'QUALITY', 'OTHER')),
    state varchar(40) NOT NULL,
    CONSTRAINT ck_claims_state CHECK (state IN ('DRAFT', 'SENT', 'IN_RESOLUTION', 'CLOSED', 'CANCELLED')),
    description text NOT NULL,
    requirements text NOT NULL,
    response_due_on date,
    sent_at timestamptz,
    send_channel varchar(40),
    CONSTRAINT ck_claims_send_channel CHECK (send_channel IN ('EMAIL', 'PHONE', 'MEETING', 'MESSENGER', 'OTHER')),
    created_by_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    result varchar(40),
    CONSTRAINT ck_claims_result CHECK (result IN ('FULL', 'PARTIAL', 'NONE')),
    remedy_type varchar(40),
    CONSTRAINT ck_claims_remedy_type CHECK (remedy_type IN ('REPLACEMENT', 'ADDITIONAL_DELIVERY', 'REFUND', 'OTHER')),
    resolution_description text,
    resolved_at timestamptz,
    approved_by_id uuid,
    lock_version bigint NOT NULL DEFAULT 0,
    CONSTRAINT pk_claims_id PRIMARY KEY (id),
    CONSTRAINT uq_claims_number UNIQUE (number),
    CONSTRAINT ck_claims_rule_1 CHECK ((state NOT IN ('CLOSED','CANCELLED')) OR (resolved_at IS NOT NULL)),
    CONSTRAINT ck_claims_rule_2 CHECK (state <> 'CLOSED' OR result IS NOT NULL),
    CONSTRAINT ck_claims_rule_3 CHECK (result NOT IN ('PARTIAL','NONE') OR approved_by_id IS NOT NULL),
    CONSTRAINT ck_claims_rule_4 CHECK ((state <> 'SENT') OR (sent_at IS NOT NULL AND send_channel IS NOT NULL)),
    CONSTRAINT ck_claims_rule_5 CHECK (created_by_id IS DISTINCT FROM approved_by_id)
);

CREATE TABLE expert_assessments (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    supplier_id uuid NOT NULL,
    period_start date NOT NULL,
    period_end date NOT NULL,
    applicable_from date NOT NULL,
    applicable_to date NOT NULL,
    score integer NOT NULL,
    justification text NOT NULL,
    state varchar(40) NOT NULL,
    CONSTRAINT ck_expert_assessments_state CHECK (state IN ('DRAFT', 'PENDING', 'APPROVED', 'REJECTED')),
    created_by_id uuid NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    approved_by_id uuid,
    approved_at timestamptz,
    supersedes_id uuid,
    CONSTRAINT pk_expert_assessments_id PRIMARY KEY (id),
    CONSTRAINT ck_expert_assessments_rule_1 CHECK (period_end >= period_start),
    CONSTRAINT ck_expert_assessments_rule_2 CHECK (applicable_to >= applicable_from),
    CONSTRAINT ck_expert_assessments_rule_3 CHECK (applicable_to - applicable_from <= 29),
    CONSTRAINT ck_expert_assessments_rule_4 CHECK (score IN (0,25,50,75,100)),
    CONSTRAINT ck_expert_assessments_rule_5 CHECK (created_by_id IS DISTINCT FROM approved_by_id),
    CONSTRAINT ck_expert_assessments_rule_6 CHECK (supersedes_id IS DISTINCT FROM id),
    CONSTRAINT ck_expert_assessments_rule_7 CHECK ((state IN ('DRAFT','PENDING','REJECTED') AND approved_by_id IS NULL AND approved_at IS NULL) OR (state = 'APPROVED' AND approved_by_id IS NOT NULL AND approved_at IS NOT NULL))
);

CREATE TABLE supplier_ratings (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    supplier_id uuid NOT NULL,
    scope varchar(40) NOT NULL,
    CONSTRAINT ck_supplier_ratings_scope CHECK (scope IN ('OVERALL', 'CATEGORY')),
    category_id uuid,
    period_start date NOT NULL,
    period_end date NOT NULL,
    as_of_at timestamptz NOT NULL,
    method_version varchar(40) NOT NULL,
    rules_snapshot jsonb NOT NULL,
    expert_assessment_id uuid,
    timeliness_score numeric(5,1),
    CONSTRAINT ck_supplier_ratings_timeliness_score_range CHECK (timeliness_score BETWEEN 0 AND 100),
    completeness_score numeric(5,1),
    CONSTRAINT ck_supplier_ratings_completeness_score_range CHECK (completeness_score BETWEEN 0 AND 100),
    quality_score numeric(5,1),
    CONSTRAINT ck_supplier_ratings_quality_score_range CHECK (quality_score BETWEEN 0 AND 100),
    interaction_score numeric(5,1),
    CONSTRAINT ck_supplier_ratings_interaction_score_range CHECK (interaction_score BETWEEN 0 AND 100),
    total_score numeric(5,1),
    CONSTRAINT ck_supplier_ratings_total_score_range CHECK (total_score BETWEEN 0 AND 100),
    band varchar(40),
    CONSTRAINT ck_supplier_ratings_band CHECK (band IN ('HIGH', 'ACCEPTABLE', 'ATTENTION', 'LOW')),
    state varchar(40) NOT NULL,
    CONSTRAINT ck_supplier_ratings_state CHECK (state IN ('INSUFFICIENT_DATA', 'PRELIMINARY', 'PUBLISHED')),
    missing_reasons jsonb,
    order_count integer NOT NULL,
    line_count integer NOT NULL,
    created_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    supersedes_id uuid,
    CONSTRAINT pk_supplier_ratings_id PRIMARY KEY (id),
    CONSTRAINT ck_supplier_ratings_rule_1 CHECK (period_end >= period_start),
    CONSTRAINT ck_supplier_ratings_rule_2 CHECK ((scope = 'OVERALL' AND category_id IS NULL) OR (scope = 'CATEGORY' AND category_id IS NOT NULL)),
    CONSTRAINT ck_supplier_ratings_rule_3 CHECK (order_count >= 0),
    CONSTRAINT ck_supplier_ratings_rule_4 CHECK (line_count >= 0),
    CONSTRAINT ck_supplier_ratings_rule_5 CHECK (state <> 'PUBLISHED' OR (order_count >= 3 AND line_count >= 1 AND expert_assessment_id IS NOT NULL)),
    CONSTRAINT ck_supplier_ratings_rule_6 CHECK ((state = 'PUBLISHED' AND total_score IS NOT NULL AND band IS NOT NULL AND timeliness_score IS NOT NULL AND completeness_score IS NOT NULL AND quality_score IS NOT NULL AND interaction_score IS NOT NULL) OR (state <> 'PUBLISHED' AND total_score IS NULL AND band IS NULL)),
    CONSTRAINT ck_supplier_ratings_rule_7 CHECK (supersedes_id IS DISTINCT FROM id)
);

CREATE TABLE rating_line_inputs (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    rating_id uuid NOT NULL,
    order_version_line_id uuid NOT NULL,
    selection_reason varchar(40) NOT NULL,
    CONSTRAINT ck_rating_line_inputs_selection_reason CHECK (selection_reason IN ('DUE_IN_PERIOD', 'OPEN_AT_PERIOD_START')),
    ordered_qty numeric(18,3) NOT NULL,
    delivered_on_time_qty numeric(18,3) NOT NULL,
    fulfilled_qty numeric(18,3) NOT NULL,
    quality_checked_qty numeric(18,3) NOT NULL,
    quality_good_qty numeric(18,3) NOT NULL,
    timeliness_score numeric(5,1) NOT NULL,
    CONSTRAINT ck_rating_line_inputs_timeliness_score_range CHECK (timeliness_score BETWEEN 0 AND 100),
    completeness_score numeric(5,1) NOT NULL,
    CONSTRAINT ck_rating_line_inputs_completeness_score_range CHECK (completeness_score BETWEEN 0 AND 100),
    quality_score numeric(5,1),
    CONSTRAINT ck_rating_line_inputs_quality_score_range CHECK (quality_score BETWEEN 0 AND 100),
    source_cutoff_at timestamptz NOT NULL,
    source_hash varchar(128),
    CONSTRAINT pk_rating_line_inputs_id PRIMARY KEY (id),
    CONSTRAINT uq_rating_line_inputs_rating_id_order_version_line_id UNIQUE (rating_id, order_version_line_id),
    CONSTRAINT ck_rating_line_inputs_rule_1 CHECK (ordered_qty > 0),
    CONSTRAINT ck_rating_line_inputs_rule_2 CHECK (delivered_on_time_qty >= 0),
    CONSTRAINT ck_rating_line_inputs_rule_3 CHECK (fulfilled_qty >= 0),
    CONSTRAINT ck_rating_line_inputs_rule_4 CHECK (quality_checked_qty >= 0),
    CONSTRAINT ck_rating_line_inputs_rule_5 CHECK (quality_good_qty >= 0),
    CONSTRAINT ck_rating_line_inputs_rule_6 CHECK (quality_good_qty <= quality_checked_qty),
    CONSTRAINT ck_rating_line_inputs_rule_7 CHECK (delivered_on_time_qty <= ordered_qty),
    CONSTRAINT ck_rating_line_inputs_rule_8 CHECK (fulfilled_qty <= ordered_qty),
    CONSTRAINT ck_rating_line_inputs_rule_9 CHECK ((quality_checked_qty = 0 AND quality_score IS NULL) OR (quality_checked_qty > 0 AND quality_score IS NOT NULL))
);

CREATE TABLE audit_events (
    id uuid NOT NULL DEFAULT gen_random_uuid(),
    actor_id uuid,
    occurred_at timestamptz NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category varchar(40) NOT NULL,
    CONSTRAINT ck_audit_events_category CHECK (category IN ('BUSINESS', 'SECURITY')),
    action varchar(120) NOT NULL,
    entity_type varchar(100) NOT NULL,
    entity_id uuid NOT NULL,
    before_data jsonb,
    after_data jsonb,
    reason text,
    correlation_id uuid NOT NULL,
    CONSTRAINT pk_audit_events_id PRIMARY KEY (id)
);

-- Foreign keys added after tables to support deliberate cycles (current versions, claim links).

ALTER TABLE user_role_assignments ADD CONSTRAINT fk_user_role_assignments_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE user_role_assignments ADD CONSTRAINT fk_user_role_assignments_assigned_by_id FOREIGN KEY (assigned_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE auth_sessions ADD CONSTRAINT fk_auth_sessions_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE products ADD CONSTRAINT fk_products_category_id FOREIGN KEY (category_id) REFERENCES product_categories (id) ON DELETE RESTRICT;

ALTER TABLE products ADD CONSTRAINT fk_products_unit_id FOREIGN KEY (unit_id) REFERENCES units (id) ON DELETE RESTRICT;

ALTER TABLE suppliers ADD CONSTRAINT fk_suppliers_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE supplier_products ADD CONSTRAINT fk_supplier_products_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT;

ALTER TABLE supplier_products ADD CONSTRAINT fk_supplier_products_product_id FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT;

ALTER TABLE supplier_status_requests ADD CONSTRAINT fk_supplier_status_requests_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT;

ALTER TABLE supplier_status_requests ADD CONSTRAINT fk_supplier_status_requests_requested_by_id FOREIGN KEY (requested_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE supplier_status_requests ADD CONSTRAINT fk_supplier_status_requests_decided_by_id FOREIGN KEY (decided_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE contracts ADD CONSTRAINT fk_contracts_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT;

ALTER TABLE contracts ADD CONSTRAINT fk_contracts_current_registered_version_id FOREIGN KEY (current_registered_version_id) REFERENCES contract_versions (id) ON DELETE RESTRICT;

ALTER TABLE contracts ADD CONSTRAINT fk_contracts_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE contract_versions ADD CONSTRAINT fk_contract_versions_contract_id FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE RESTRICT;

ALTER TABLE contract_versions ADD CONSTRAINT fk_contract_versions_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE contract_versions ADD CONSTRAINT fk_contract_versions_decided_by_id FOREIGN KEY (decided_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT;

ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_contract_id FOREIGN KEY (contract_id) REFERENCES contracts (id) ON DELETE RESTRICT;

ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_current_approved_version_id FOREIGN KEY (current_approved_version_id) REFERENCES order_versions (id) ON DELETE RESTRICT;

ALTER TABLE purchase_orders ADD CONSTRAINT fk_purchase_orders_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE order_lines ADD CONSTRAINT fk_order_lines_order_id FOREIGN KEY (order_id) REFERENCES purchase_orders (id) ON DELETE RESTRICT;

ALTER TABLE order_lines ADD CONSTRAINT fk_order_lines_product_id FOREIGN KEY (product_id) REFERENCES products (id) ON DELETE RESTRICT;

ALTER TABLE order_versions ADD CONSTRAINT fk_order_versions_order_id FOREIGN KEY (order_id) REFERENCES purchase_orders (id) ON DELETE RESTRICT;

ALTER TABLE order_versions ADD CONSTRAINT fk_order_versions_base_version_id FOREIGN KEY (base_version_id) REFERENCES order_versions (id) ON DELETE RESTRICT;

ALTER TABLE order_versions ADD CONSTRAINT fk_order_versions_contract_version_id FOREIGN KEY (contract_version_id) REFERENCES contract_versions (id) ON DELETE RESTRICT;

ALTER TABLE order_versions ADD CONSTRAINT fk_order_versions_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE order_versions ADD CONSTRAINT fk_order_versions_decided_by_id FOREIGN KEY (decided_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE order_version_lines ADD CONSTRAINT fk_order_version_lines_order_version_id FOREIGN KEY (order_version_id) REFERENCES order_versions (id) ON DELETE RESTRICT;

ALTER TABLE order_version_lines ADD CONSTRAINT fk_order_version_lines_order_line_id FOREIGN KEY (order_line_id) REFERENCES order_lines (id) ON DELETE RESTRICT;

ALTER TABLE order_version_lines ADD CONSTRAINT fk_order_version_lines_source_version_line_id FOREIGN KEY (source_version_line_id) REFERENCES order_version_lines (id) ON DELETE RESTRICT;

ALTER TABLE order_closures ADD CONSTRAINT fk_order_closures_order_id FOREIGN KEY (order_id) REFERENCES purchase_orders (id) ON DELETE RESTRICT;

ALTER TABLE order_closures ADD CONSTRAINT fk_order_closures_requested_by_id FOREIGN KEY (requested_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE order_closures ADD CONSTRAINT fk_order_closures_decided_by_id FOREIGN KEY (decided_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE order_closure_lines ADD CONSTRAINT fk_order_closure_lines_closure_id FOREIGN KEY (closure_id) REFERENCES order_closures (id) ON DELETE RESTRICT;

ALTER TABLE order_closure_lines ADD CONSTRAINT fk_order_closure_lines_order_line_id FOREIGN KEY (order_line_id) REFERENCES order_lines (id) ON DELETE RESTRICT;

ALTER TABLE deliveries ADD CONSTRAINT fk_deliveries_order_id FOREIGN KEY (order_id) REFERENCES purchase_orders (id) ON DELETE RESTRICT;

ALTER TABLE deliveries ADD CONSTRAINT fk_deliveries_registered_by_id FOREIGN KEY (registered_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE deliveries ADD CONSTRAINT fk_deliveries_supersedes_id FOREIGN KEY (supersedes_id) REFERENCES deliveries (id) ON DELETE RESTRICT;

ALTER TABLE deliveries ADD CONSTRAINT fk_deliveries_correction_id FOREIGN KEY (correction_id) REFERENCES fact_corrections (id) ON DELETE RESTRICT;

ALTER TABLE delivery_lines ADD CONSTRAINT fk_delivery_lines_delivery_id FOREIGN KEY (delivery_id) REFERENCES deliveries (id) ON DELETE RESTRICT;

ALTER TABLE delivery_lines ADD CONSTRAINT fk_delivery_lines_order_version_line_id FOREIGN KEY (order_version_line_id) REFERENCES order_version_lines (id) ON DELETE RESTRICT;

ALTER TABLE delivery_lines ADD CONSTRAINT fk_delivery_lines_replacement_for_line_id FOREIGN KEY (replacement_for_line_id) REFERENCES delivery_lines (id) ON DELETE RESTRICT;

ALTER TABLE delivery_lines ADD CONSTRAINT fk_delivery_lines_claim_id FOREIGN KEY (claim_id) REFERENCES claims (id) ON DELETE RESTRICT;

ALTER TABLE acceptance_documents ADD CONSTRAINT fk_acceptance_documents_delivery_id FOREIGN KEY (delivery_id) REFERENCES deliveries (id) ON DELETE RESTRICT;

ALTER TABLE acceptance_documents ADD CONSTRAINT fk_acceptance_documents_registered_by_id FOREIGN KEY (registered_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE acceptance_documents ADD CONSTRAINT fk_acceptance_documents_supersedes_id FOREIGN KEY (supersedes_id) REFERENCES acceptance_documents (id) ON DELETE RESTRICT;

ALTER TABLE acceptance_documents ADD CONSTRAINT fk_acceptance_documents_correction_id FOREIGN KEY (correction_id) REFERENCES fact_corrections (id) ON DELETE RESTRICT;

ALTER TABLE acceptance_lines ADD CONSTRAINT fk_acceptance_lines_acceptance_document_id FOREIGN KEY (acceptance_document_id) REFERENCES acceptance_documents (id) ON DELETE RESTRICT;

ALTER TABLE acceptance_lines ADD CONSTRAINT fk_acceptance_lines_delivery_line_id FOREIGN KEY (delivery_line_id) REFERENCES delivery_lines (id) ON DELETE RESTRICT;

ALTER TABLE supplier_returns ADD CONSTRAINT fk_supplier_returns_order_id FOREIGN KEY (order_id) REFERENCES purchase_orders (id) ON DELETE RESTRICT;

ALTER TABLE supplier_returns ADD CONSTRAINT fk_supplier_returns_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE supplier_returns ADD CONSTRAINT fk_supplier_returns_approved_by_id FOREIGN KEY (approved_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE supplier_returns ADD CONSTRAINT fk_supplier_returns_supersedes_id FOREIGN KEY (supersedes_id) REFERENCES supplier_returns (id) ON DELETE RESTRICT;

ALTER TABLE supplier_returns ADD CONSTRAINT fk_supplier_returns_correction_id FOREIGN KEY (correction_id) REFERENCES fact_corrections (id) ON DELETE RESTRICT;

ALTER TABLE supplier_return_lines ADD CONSTRAINT fk_supplier_return_lines_return_id FOREIGN KEY (return_id) REFERENCES supplier_returns (id) ON DELETE RESTRICT;

ALTER TABLE supplier_return_lines ADD CONSTRAINT fk_supplier_return_lines_acceptance_line_id FOREIGN KEY (acceptance_line_id) REFERENCES acceptance_lines (id) ON DELETE RESTRICT;

ALTER TABLE supplier_return_lines ADD CONSTRAINT fk_supplier_return_lines_claim_id FOREIGN KEY (claim_id) REFERENCES claims (id) ON DELETE RESTRICT;

ALTER TABLE fact_corrections ADD CONSTRAINT fk_fact_corrections_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE fact_corrections ADD CONSTRAINT fk_fact_corrections_decided_by_id FOREIGN KEY (decided_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE interactions ADD CONSTRAINT fk_interactions_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT;

ALTER TABLE interactions ADD CONSTRAINT fk_interactions_employee_id FOREIGN KEY (employee_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE interactions ADD CONSTRAINT fk_interactions_claim_id FOREIGN KEY (claim_id) REFERENCES claims (id) ON DELETE RESTRICT;

ALTER TABLE claims ADD CONSTRAINT fk_claims_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT;

ALTER TABLE claims ADD CONSTRAINT fk_claims_order_line_id FOREIGN KEY (order_line_id) REFERENCES order_lines (id) ON DELETE RESTRICT;

ALTER TABLE claims ADD CONSTRAINT fk_claims_delivery_line_id FOREIGN KEY (delivery_line_id) REFERENCES delivery_lines (id) ON DELETE RESTRICT;

ALTER TABLE claims ADD CONSTRAINT fk_claims_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE claims ADD CONSTRAINT fk_claims_approved_by_id FOREIGN KEY (approved_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE expert_assessments ADD CONSTRAINT fk_expert_assessments_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT;

ALTER TABLE expert_assessments ADD CONSTRAINT fk_expert_assessments_created_by_id FOREIGN KEY (created_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE expert_assessments ADD CONSTRAINT fk_expert_assessments_approved_by_id FOREIGN KEY (approved_by_id) REFERENCES users (id) ON DELETE RESTRICT;

ALTER TABLE expert_assessments ADD CONSTRAINT fk_expert_assessments_supersedes_id FOREIGN KEY (supersedes_id) REFERENCES expert_assessments (id) ON DELETE RESTRICT;

ALTER TABLE supplier_ratings ADD CONSTRAINT fk_supplier_ratings_supplier_id FOREIGN KEY (supplier_id) REFERENCES suppliers (id) ON DELETE RESTRICT;

ALTER TABLE supplier_ratings ADD CONSTRAINT fk_supplier_ratings_category_id FOREIGN KEY (category_id) REFERENCES product_categories (id) ON DELETE RESTRICT;

ALTER TABLE supplier_ratings ADD CONSTRAINT fk_supplier_ratings_expert_assessment_id FOREIGN KEY (expert_assessment_id) REFERENCES expert_assessments (id) ON DELETE RESTRICT;

ALTER TABLE supplier_ratings ADD CONSTRAINT fk_supplier_ratings_supersedes_id FOREIGN KEY (supersedes_id) REFERENCES supplier_ratings (id) ON DELETE RESTRICT;

ALTER TABLE rating_line_inputs ADD CONSTRAINT fk_rating_line_inputs_rating_id FOREIGN KEY (rating_id) REFERENCES supplier_ratings (id) ON DELETE RESTRICT;

ALTER TABLE rating_line_inputs ADD CONSTRAINT fk_rating_line_inputs_order_version_line_id FOREIGN KEY (order_version_line_id) REFERENCES order_version_lines (id) ON DELETE RESTRICT;

ALTER TABLE audit_events ADD CONSTRAINT fk_audit_events_actor_id FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE RESTRICT;

-- Current version pointers MUST refer to a version of their own document.
ALTER TABLE contracts ADD CONSTRAINT fk_contract_current_same_document
    FOREIGN KEY (current_registered_version_id, id) REFERENCES contract_versions (id, contract_id)
    DEFERRABLE INITIALLY IMMEDIATE;
ALTER TABLE purchase_orders ADD CONSTRAINT fk_order_current_same_document
    FOREIGN KEY (current_approved_version_id, id) REFERENCES order_versions (id, order_id)
    DEFERRABLE INITIALLY IMMEDIATE;


-- Exactly one current role / one open approval request where required.
CREATE UNIQUE INDEX uq_current_user_role ON user_role_assignments (user_id, role_code) WHERE revoked_at IS NULL;
CREATE UNIQUE INDEX uq_pending_supplier_status ON supplier_status_requests (supplier_id) WHERE state = 'PENDING';
CREATE UNIQUE INDEX uq_open_order_change ON order_versions (order_id) WHERE kind = 'CHANGE' AND state IN ('DRAFT','PENDING');
CREATE UNIQUE INDEX uq_initial_order_version ON order_versions (order_id) WHERE kind = 'INITIAL';


CREATE INDEX idx_user_role_assignments_user_id ON user_role_assignments (user_id);

CREATE INDEX idx_user_role_assignments_assigned_at ON user_role_assignments (assigned_at);

CREATE INDEX idx_auth_sessions_user_id ON auth_sessions (user_id);

CREATE INDEX idx_auth_sessions_expires_at ON auth_sessions (expires_at);

CREATE INDEX idx_products_category_id ON products (category_id);

CREATE INDEX idx_suppliers_status ON suppliers (status);

CREATE INDEX idx_suppliers_name ON suppliers (name);

CREATE INDEX idx_supplier_products_product_id ON supplier_products (product_id);

CREATE INDEX idx_supplier_status_requests_supplier_id_requested_at ON supplier_status_requests (supplier_id, requested_at);

CREATE INDEX idx_contracts_supplier_id_state ON contracts (supplier_id, state);

CREATE INDEX idx_purchase_orders_supplier_id_state ON purchase_orders (supplier_id, state);

CREATE INDEX idx_purchase_orders_contract_id ON purchase_orders (contract_id);

CREATE INDEX idx_order_version_lines_order_line_id ON order_version_lines (order_line_id);

CREATE INDEX idx_order_version_lines_execution_due_on ON order_version_lines (execution_due_on);

CREATE INDEX idx_order_closures_order_id_occurred_at ON order_closures (order_id, occurred_at);

CREATE INDEX idx_deliveries_order_id_arrived_on ON deliveries (order_id, arrived_on);

CREATE INDEX idx_delivery_lines_order_version_line_id ON delivery_lines (order_version_line_id);

CREATE INDEX idx_acceptance_documents_delivery_id_inspected_on ON acceptance_documents (delivery_id, inspected_on);

CREATE INDEX idx_acceptance_lines_delivery_line_id ON acceptance_lines (delivery_line_id);

CREATE INDEX idx_supplier_returns_order_id_returned_on ON supplier_returns (order_id, returned_on);

CREATE INDEX idx_supplier_return_lines_acceptance_line_id ON supplier_return_lines (acceptance_line_id);

CREATE INDEX idx_interactions_supplier_id_occurred_at ON interactions (supplier_id, occurred_at);

CREATE INDEX idx_claims_supplier_id_state ON claims (supplier_id, state);

CREATE INDEX idx_claims_response_due_on ON claims (response_due_on);

CREATE INDEX idx_expert_assessments_supplier_id_applicable_from ON expert_assessments (supplier_id, applicable_from);

CREATE INDEX idx_supplier_ratings_supplier_id_as_of_at ON supplier_ratings (supplier_id, as_of_at);

CREATE INDEX idx_supplier_ratings_category_id ON supplier_ratings (category_id);

CREATE INDEX idx_audit_events_entity_type_entity_id_occurred_at ON audit_events (entity_type, entity_id, occurred_at);

CREATE INDEX idx_audit_events_actor_id_occurred_at ON audit_events (actor_id, occurred_at);

-- Simple cross-field state constraints applied on the governing document.
ALTER TABLE order_versions ADD CONSTRAINT ck_version_change_requires_base
    CHECK ((kind='INITIAL' AND base_version_id IS NULL) OR
           (kind='CHANGE' AND base_version_id IS NOT NULL AND initiator IS NOT NULL AND reason IS NOT NULL));
ALTER TABLE order_versions ADD CONSTRAINT ck_approved_version_has_contract
    CHECK (state <> 'APPROVED' OR contract_version_id IS NOT NULL);
ALTER TABLE purchase_orders ADD CONSTRAINT ck_submitted_order_has_contract
    CHECK (state = 'DRAFT' OR contract_id IS NOT NULL);
ALTER TABLE purchase_orders ADD CONSTRAINT ck_approved_order_has_version
    CHECK (state NOT IN ('APPROVED','IN_PROGRESS','COMPLETED') OR current_approved_version_id IS NOT NULL);



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
