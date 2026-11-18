# Supplier Management v2 — карта связей целевой модели

Статус: проектное представление 30 таблиц, согласованное с `FINAL_OPTIMIZED_DATA_MODEL.md`. Диаграммы Mermaid концептуальны: перечисляют основные зависимости, но не заменяют SQL-ограничения.

## А. Пользователи, каталог, поставщики и договоры

```mermaid
erDiagram
  users ||--o{ user_role_assignments : receives
  users ||--o{ auth_sessions : opens
  users ||--o{ supplier_status_requests : requests
  product_categories ||--o{ products : groups
  units ||--o{ products : measures
  suppliers ||--o{ supplier_products : offers
  products ||--o{ supplier_products : offered_by
  suppliers ||--o{ supplier_status_requests : status_changes
  suppliers ||--o{ contracts : agrees
  contracts ||--o{ contract_versions : versions
```

## Б. Заказ, исполнение и корректировки

```mermaid
erDiagram
  suppliers ||--o{ purchase_orders : receives
  contracts ||--o{ purchase_orders : governs
  contract_versions ||--o{ order_versions : contract_snapshot
  purchase_orders ||--|{ order_lines : contains
  purchase_orders ||--o{ order_versions : has
  order_lines ||--o{ order_version_lines : carried_in
  order_versions ||--o{ order_version_lines : snapshots
  purchase_orders ||--o{ order_closures : decisions
  order_closures ||--o{ order_closure_lines : snapshots
  order_lines ||--o{ order_closure_lines : closed_quantities
  purchase_orders ||--o{ deliveries : receives
  deliveries ||--o{ delivery_lines : contains
  order_version_lines ||--o{ delivery_lines : supplies_part
  deliveries ||--o{ acceptance_documents : inspected_by
  acceptance_documents ||--o{ acceptance_lines : contains
  delivery_lines ||--o{ acceptance_lines : inspected
  purchase_orders ||--o{ supplier_returns : return_for
  supplier_returns ||--o{ supplier_return_lines : contains
  acceptance_lines ||--o{ supplier_return_lines : returns_from
  fact_corrections ||--o{ deliveries : replaces
  fact_corrections ||--o{ acceptance_documents : replaces
  fact_corrections ||--o{ supplier_returns : replaces
```

Принцип: после согласования заказа `order_version_lines` содержит **по одной строке на часть обязательства с собственным сроком**; она связана с постоянной логической строкой `order_lines`. Сегмент не нуждается в отдельной таблице.

## В. Взаимодействия, претензии, оценки, рейтинг и аудит

```mermaid
erDiagram
  suppliers ||--o{ interactions : communicates
  suppliers ||--o{ claims : claims
  claims o|--o{ interactions : related
  claims o|--o{ delivery_lines : remedy
  claims o|--o{ supplier_return_lines : reason_for
  order_lines o|--o{ claims : concerns
  delivery_lines o|--o{ claims : concerns
  suppliers ||--o{ expert_assessments : assessed
  suppliers ||--o{ supplier_ratings : rated
  expert_assessments o|--o{ supplier_ratings : applied
  supplier_ratings ||--o{ rating_line_inputs : contains
  order_version_lines ||--o{ rating_line_inputs : explains
  users ||--o{ audit_events : records
```

`audit_events` использует осмысленную пару `entity_type/entity_id` только для **журнала**; критические связи документов задаются настоящими FK.

## D. Три проверки, требующие специальных правил

1. **Согласованные версии:** `contracts.current_registered_version_id` должен указывать на `contract_versions` того же договора; `purchase_orders.current_approved_version_id` — на `order_versions` того же заказа. Достаточно обычного FK недостаточно; используем составной FK либо проверку в транзакции и ограничение БД.
2. **Один открытый запрос:** на поставщика — только один `supplier_status_requests` в `PENDING`; на заказ — одна `order_versions` в `PENDING` (с учётом разрешённых черновиков). На PostgreSQL — частичные уникальные индексы.
3. **Приёмка и возврат:** по `delivery_lines` суммарно подтверждённая приёмка не превышает фактическую поставку; по `acceptance_lines` суммарные подтверждённые возвраты не превышают принятого количества. Это агрегатные транзакционные инварианты, а не CHECK одной строки.
