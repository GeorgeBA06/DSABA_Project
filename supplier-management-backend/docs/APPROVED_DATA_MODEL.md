# Supplier Management — окончательная оптимизированная модель данных (проект на утверждение)

Версия: 2.0, 31.10.2026  
Статус: **рекомендованная целевая модель для пересборки backend**, а не готовая миграция/код.  
Основа: ТЗ на курсовой проект, логическая схема v1, согласованные уточнения по Java/Spring Data JPA/Беларуси/ролям/рейтингу, физическая JPA-модель Stage 1 из 43 таблиц.

## 0. Результат

**30 таблиц / 30 JPA Entity вместо 43 / 43** (минус 13, сокращение на 30,2%). Оптимизация делается за счёт объединения семантически совпадающих объектов и удаления инфраструктуры, не требуемой первой версией. Нет искусственной цели «сделать 20»: такое сокращение изменило бы уже согласованные процессы или создало бы JSON-таблицы без реляционной целостности.

| Модуль | Таблиц | Таблицы |
|---|---:|---|
| Доступ | 3 | `users`, `user_role_assignments`, `auth_sessions` |
| Каталог | 3 | `product_categories`, `units`, `products` |
| Поставщики | 3 | `suppliers`, `supplier_products`, `supplier_status_requests` |
| Договоры | 2 | `contracts`, `contract_versions` |
| Заказы | 6 | `purchase_orders`, `order_lines`, `order_versions`, `order_version_lines`, `order_closures`, `order_closure_lines` |
| Исполнение | 7 | `deliveries`, `delivery_lines`, `acceptance_documents`, `acceptance_lines`, `supplier_returns`, `supplier_return_lines`, `fact_corrections` |
| Претензии и контакты | 2 | `interactions`, `claims` |
| Надёжность | 3 | `expert_assessments`, `supplier_ratings`, `rating_line_inputs` |
| Аудит | 1 | `audit_events` |
| **Всего** | **30** | |

### Принятый минимальный функциональный объём

- Одна компания-покупатель; товары, не услуги; валюта BYN; НДС не рассчитывается.
- Внутренние пользователи; роли: `PROCUREMENT_SPECIALIST`, `PROCUREMENT_MANAGER`, `ADMINISTRATOR`, включая совмещение. Администратор **не получает автоматически** прав закупок. Нет публичной регистрации.
- Каждый специалист вправе работать с **любым** поставщиком. Нет таблицы обязательных назначений поставщика сотруднику.
- Поставщик с основными контактами, предложением товаров, статусом и согласованием смены статуса руководителем.
- Договоры и заказы имеют согласуемые версии; автор версии не вправе согласовать её сам.
- Частичные поступления, несколько приёмок поступления, брак, возвраты и пакетное исправление ошибочного подтверждённого факта.
- Претензия относится к **одному первичному нарушению/строке заказа**; сопутствующая коммуникация и подтверждённое решение сохраняются. Если требуется претензия сразу к множеству разных нарушений, позже возвращается `claim_targets`.
- Ежедневные рейтинги на 90-дневном окне; экспертная оценка утверждается руководителем за месяц и действует до 30 дней согласно согласованным правилам. История рейтингов, ограничений и входных агрегатов сохраняется.
- Отчёты и главная панель являются запросами/проекциями существующих данных, а не самостоятельными таблицами.

## 1. Таблицы и ключевые поля

В таблицах ниже `id` означает UUID PK, если явно не сказано иное. `*_id` — FK, `created_by`/`decided_by` и т. п. — FK в `users`; поля времени и аудита указаны отдельно в правилах. Это **логическая спецификация физической модели**, а не синтаксически полный SQL DDL.

### 1.1. Авторизация и доступ — 3

**`users` / `User`**: `id`, `login` UQ, `password_hash`, `display_name`, `status {ACTIVE,BLOCKED}`, `failed_login_count`, `locked_until?`, `created_at`, `updated_at`, `lock_version`. Сотрудников создаёт администратор; удаление пользователей с историей запрещено.

**`user_role_assignments` / `UserRoleAssignment`**: `id`, `user_id`, `role_code {PROCUREMENT_SPECIALIST,PROCUREMENT_MANAGER,ADMINISTRATOR}`, `assigned_by_id`, `assigned_at`, `revoked_at?`. Единственное активное назначение каждой роли одному пользователю; частичный уникальный индекс `(user_id,role_code) WHERE revoked_at IS NULL`. Роль является Java Enum, поэтому таблица `roles` не нужна; исторические назначения сохраняются.

**`auth_sessions` / `AuthSession`**: `id`, `user_id`, `refresh_token_hash` (хеш, не сам токен), `token_generation`, `created_at`, `expires_at`, `revoked_at?`, `rotated_at?`, `lock_version`. Для JWT access token — подпись и срок ~15 минут, для refresh — непрозрачный токен ~7 дней. Ротация хеша **атомарна** (CAS/блокировка), повтор старого поколения отклоняется; отзыв блокирует сессию. Если позже потребуется полный журнал всех поколений refresh-токенов для криминалистики, добавится отдельная таблица — сейчас это не функциональное требование.

### 1.2. Каталог — 3

**`product_categories` / `ProductCategory`**: `id`, `code` UQ, `name`, `active`, `lock_version`.

**`units` / `Unit`**: `id`, `code` UQ, `name`, `active`. Например, шт., кг, м; числовые количества могут быть дробными.

**`products` / `Product`**: `id`, `sku` UQ, `name`, `category_id`, `unit_id`, `active`, `created_at`, `updated_at`, `lock_version`. Применяются soft-disable, а не каскадное удаление истории.

### 1.3. Поставщики — 3

**`suppliers` / `Supplier`**: `id`, `name`, `contact_name?`, `email?`, `phone?`, `address?`, `registration_number?` (необязателен и не валидируется по государственному реестру), `status {CANDIDATE,ACTIVE,SUSPENDED,TERMINATED}`, `created_by_id`, `created_at`, `updated_at`, `lock_version`. Несколько адресных контактов в первой версии не нужны.

**`supplier_products` / `SupplierProduct`**: `supplier_id`, `product_id`, `supplier_item_code?`, `active`; PK `(supplier_id,product_id)`. Это не лишняя таблица: требуется связь M:N с собственными атрибутами.

**`supplier_status_requests` / `SupplierStatusRequest`**: `id`, `supplier_id`, `from_status`, `to_status`, `reason`, `requested_by_id`, `requested_at`, `state {PENDING,APPROVED,REJECTED}`, `decided_by_id?`, `decided_at?`, `decision_comment?`. Одна открытая заявка на поставщика; автор и согласующий разные; одобрение и смена `suppliers.status` выполняются атомарно. Таблицу сознательно оставляем ради независимого согласования, а не из-за UI-экрана.

### 1.4. Договоры — 2

**`contracts` / `Contract`**: `id`, `supplier_id`, `state {DRAFT,REGISTERED,TERMINATED}`, `current_registered_version_id?`, `created_by_id`, `created_at`, `lock_version`.

**`contract_versions` / `ContractVersion`**: `id`, `contract_id`, `version_no`, `number`, `signed_on?`, `valid_from`, `valid_to`, `terms` (текст минимальных условий), `change_reason?`, `state {DRAFT,PENDING,APPROVED,REJECTED}`, `created_by_id`, `created_at`, `decided_by_id?`, `decided_at?`, `decision_comment?`. UQ `(contract_id,version_no)`; `valid_from <= valid_to`. После утверждения редакция неизменяема; новая редакция — новая строка. `current_registered_version_id` обязан ссылаться на версию **того же договора**.

### 1.5. Заказы — 6

**`purchase_orders` / `PurchaseOrder`**: `id`, `number` UQ, `supplier_id`, `contract_id?` (только пока черновик без договора), `state {DRAFT,PENDING_APPROVAL,APPROVED,IN_PROGRESS,COMPLETED,CANCELLED}`, `current_approved_version_id?`, `created_by_id`, `created_at`, `updated_at`, `lock_version`. Сумма заказа вычисляется как сумма `quantity * unit_price` актуальной версии; отдельный денежный баланс/НДС не хранится.

**`order_lines` / `OrderLine`**: `id`, `order_id`, `line_no`, `product_id` (фиксированная идентичность товара строки); UQ `(order_id,line_no)`. Один товар допускается в нескольких строках одного заказа с разными сроками.

**`order_versions` / `OrderVersion`**: `id`, `order_id`, `version_no`, `base_version_id?`, `contract_version_id?`, `kind {INITIAL,CHANGE}`, `initiator {BUYER,SUPPLIER,CORRECTION}?`, `reason?`, `state {DRAFT,PENDING,APPROVED,REJECTED}`, `created_by_id`, `created_at`, `submitted_at?`, `decided_by_id?`, `decided_at?`, `decision_comment?`. UQ `(order_id,version_no)`; только одно открытое изменение (`kind=CHANGE`, `state IN (DRAFT,PENDING)`) на заказ. Новая версия **и есть запрос на изменение**; отдельная таблица `order_change_requests` не нужна.

**`order_version_lines` / `OrderVersionLine`** — ключевая оптимизация: `id`, `order_version_id`, `order_line_id`, `segment_key` (стабильный логический идентификатор части обязательства), `source_version_line_id?` (происхождение из предшествующей версии), `quantity`, `unit_price`, `execution_due_on`, `rating_due_on`, `origin_reason {INITIAL,BUYER_CHANGE,SUPPLIER_RESCHEDULE,CORRECTION}`, `product_name_snapshot`, `category_code_snapshot`, `category_name_snapshot`, `unit_code_snapshot`. UQ `(order_version_id,segment_key)`; `quantity > 0`, `unit_price >= 0`. **В одной версии может быть несколько записей для одной `order_line_id`**, если её количество состоит из частей с разными сроками/основаниями. Именно поэтому отдельная `order_obligation_segments` исключается. UI показывает общую товарную строку, агрегируя её части; сервер считает сроки и рейтинг **по частям**.

**Пример сегментов (без дополнительной таблицы).** Первоначально заказано 100 шт. со сроком 10 ноября: в первой версии одна запись `segment_key=A, quantity=100, rating_due_on=10.11`. Покупатель добавляет 50 шт. со сроком 20 ноября: во второй версии существуют части `A=100, рейтинг=10.11` и `B=50, рейтинг=20.11`. Поставщик переносит исполнение части A на 18 ноября: в третьей версии `execution_due_on` для этой части меняется, `rating_due_on` остаётся 10 ноября. При разделении уже исполняемого сегмента сервис обязан распределять ранее полученные количества без повторного учёта; тест этого инварианта обязателен.

**`order_closures` / `OrderClosure`**: `id`, `order_id`, `action {CLOSE,REOPEN}`, `result {FULFILLED,CLOSED_WITH_REMAINDER}?`, `reason?`, `requested_by_id?`, `decided_by_id?`, `occurred_at`. Каждое закрытие и повторное открытие — отдельная неизменяемая запись; прежние не переписываются. При остатке обязательны основание и независимое решение руководителя.

**`order_closure_lines` / `OrderClosureLine`**: составной PK `(closure_id,order_line_id)`, `ordered_qty`, `fulfilled_qty`, `unfulfilled_qty` при действии `CLOSE`; UQ `(closure_id,order_line_id)` и CHECK `ordered_qty=fulfilled_qty+unfulfilled_qty`. Это небольшой, но полезный **неизменяемый снимок** фактического результата на момент закрытия. Хранение в отдельных строках лучше JSON-массива: проверки, 3НФ и выборки проще.

### 1.6. Поступления, приёмка, возвраты, исправления — 7

**`deliveries` / `Delivery`**: `id`, `order_id`, `arrived_on`, `registered_at`, `registered_by_id`, `state {DRAFT,CONFIRMED,CANCELLED}`, `external_number?`, `supersedes_id?`, `correction_id?`. Дата прибытия не равна времени ввода.

**`delivery_lines` / `DeliveryLine`**: `id`, `delivery_id`, `order_version_line_id` (ссылка на конкретную утверждённую часть обязательства), `quantity > 0`, `replacement_for_line_id?`, `claim_id?` для связи допоставки/замены с одной претензией. Отдельные дублирующие ссылки на заказ/товар не нужны — они однозначно следуют по FK.

**`acceptance_documents` / `AcceptanceDocument`**: `id`, `delivery_id`, `inspected_on`, `registered_by_id`, `registered_at`, `state {DRAFT,CONFIRMED,CANCELLED}`, `supersedes_id?`, `correction_id?`. Нужна отдельно от поставки: один приход можно проверять частями и в другое время.

**`acceptance_lines` / `AcceptanceLine`**: `id`, `acceptance_document_id`, `delivery_line_id`, `accepted_qty`, `quality_rejected_qty`, `other_rejected_qty`, `other_reason?`. Все величины >=0, сумма >0; сумма всех подтверждённых распределений для одной физической строки не превосходит поступившее количество.

**`supplier_returns` / `SupplierReturn`**: `id`, `order_id`, `returned_on`, `state {DRAFT,PENDING,CONFIRMED,REJECTED}`, `reason`, `created_by_id`, `approved_by_id?`, `approved_at?`, `supersedes_id?`, `correction_id?`. Возврат только ранее принятого дефектного товара; подтверждает руководитель, не автор.

**`supplier_return_lines` / `SupplierReturnLine`**: `id`, `return_id`, `acceptance_line_id`, `quantity > 0`, `defect_description`, `claim_id?`. Совокупный подтверждённый возврат по приёмке не может превышать ранее принятое количество.

**`fact_corrections` / `FactCorrection`**: `id`, `reason`, `state {DRAFT,PENDING,APPROVED,REJECTED}`, `effective_on`, `created_by_id`, `created_at`, `decided_by_id?`, `decided_at?`, `decision_comment?`. Одно согласование может охватывать несколько заменяющих документов. Исправления факта — это **не** физические возвраты; исходная подтверждённая запись не изменяется и не удаляется.

### 1.7. Контакты и претензии — 2

**`interactions` / `Interaction`**: `id`, `supplier_id`, `employee_id`, `occurred_at`, `channel {EMAIL,PHONE,MEETING,MESSENGER,OTHER}`, `subject`, `content`, `outcome?`, `claim_id?`. Хронология переписки и контактов; автоматическая отправка писем не предусмотрена.

**`claims` / `Claim`**: `id`, `supplier_id`, `number` UQ, `order_line_id` (обязателен), `delivery_line_id?`, `violation_type {LATE,SHORTAGE,QUALITY,OTHER}`, `state {DRAFT,SENT,IN_RESOLUTION,CLOSED,CANCELLED}`, `description`, `requirements`, `response_due_on?`, `sent_at?`, `send_channel?`, `created_by_id`, `created_at`, `result?`, `remedy_type?`, `resolution_description?`, `resolved_at?`, `approved_by_id?`, `lock_version`. Одна претензия — одно первичное нарушение, связанное с поставщиком и при наличии с конкретной строкой заказа/поставки. Допоставку можно связать через `delivery_lines.claim_id`; сообщения — через `interactions.claim_id`. Изменения состояния/решения фиксируются в `audit_events`; это позволяет не заводить отдельные `claim_targets`, `claim_events`, `claim_delivery_links` в первой версии. Частичное/нулевое удовлетворение требует утверждения руководителем.

### 1.8. Экспертная оценка и рейтинг — 3

**`expert_assessments` / `ExpertAssessment`**: `id`, `supplier_id`, `period_start`, `period_end`, `applicable_from`, `applicable_to`, `score IN (0,25,50,75,100)`, `justification`, `state {DRAFT,PENDING,APPROVED,REJECTED}`, `created_by_id`, `created_at`, `approved_by_id?`, `approved_at?`, `supersedes_id?`. Независимое утверждение; утверждённая оценка не редактируется.

**`supplier_ratings` / `SupplierRating`**: `id`, `supplier_id`, `scope {OVERALL,CATEGORY}`, `category_id?`, `period_start`, `period_end`, `as_of_at`, `method_version`, `rules_snapshot` (неизменяемый JSONB с четырьмя весами и порогами), `expert_assessment_id?`, `timeliness_score?`, `completeness_score?`, `quality_score?`, `interaction_score?`, `total_score?`, `band?`, `state {INSUFFICIENT_DATA,PRELIMINARY,PUBLISHED}`, `missing_reasons?` (структурированный снимок), `order_count`, `line_count`, `created_at`, `supersedes_id?`. Четыре критерия **фиксированы в требованиях**, поэтому четыре колонки лучше дополнительной EAV-таблицы. При предварительном результате `total_score IS NULL`; `0` не означает «нет данных». Новый расчёт — новая строка; прежний рейтинг неизменяем.

**`rating_line_inputs` / `RatingLineInput`**: `id`, `rating_id`, `order_version_line_id`, `selection_reason {DUE_IN_PERIOD,OPEN_AT_PERIOD_START}`, `ordered_qty`, `delivered_on_time_qty`, `fulfilled_qty`, `quality_checked_qty`, `quality_good_qty`, `timeliness_score`, `completeness_score`, `quality_score?`, `source_cutoff_at`, `source_hash?`. Одна строка на **учтённую часть обязательства**, а не на товар целиком; UQ `(rating_id,order_version_line_id)`. Эти снимки дают воспроизводимость арифметики и возможность показать, из каких обязательств получен результат без четырёх дополнительных таблиц входных фактов. Подробные факты остаются в неизменяемых версиях поставок/приёмок/возвратов и выбираются по `source_cutoff_at`.

**Формула и публикация.** Итог, если все критерии определены: `0.35*T + 0.25*C + 0.30*Q + 0.10*I`, округление до 1 знака по зафиксированному алгоритму. Диапазоны: высокая надёжность 85–100; приемлемая 70–<85; требует внимания 50–<70; низкая <50. Публикация только при минимум 3 согласованных заказах в выборке, проверенном качестве и утверждённой экспертной оценке. Отсутствующий критерий не принимается за ноль и не перераспределяет вес. Категорийный результат рассчитывается по собственным заказам и полноте данных. Период 90 дней, включая старые открытые обязательства по зафиксированному правилу.

### 1.9. Аудит — 1

**`audit_events` / `AuditEvent`**: `id`, `actor_id?`, `occurred_at`, `category {BUSINESS,SECURITY}`, `action`, `entity_type`, `entity_id`, `before_data?`, `after_data?`, `reason?`, `correlation_id`. Append-only. Здесь `entity_type/entity_id` допустимы, потому что это журнал, а не основная связь бизнес-объектов. Секреты и токены не логируются. Значимые решения также имеют явные поля `created_by`/`decided_by` в своих бизнес-объектах; аудит **не подменяет** историю версий.

## 2. Что изменяем относительно 43 таблиц

**Удаляем/объединяем ровно 13 таблиц:**

| Старая таблица | Решение в v2 |
|---|---|
| `roles` | Java Enum и CHECK на `user_role_assignments.role_code`; история назначений остаётся |
| `auth_refresh_tokens` | Один актуальный refresh-хеш и поколение в `auth_sessions` |
| `auth_login_attempts` | Учёт неудачных попыток/периода блокировки в `users`; ограничения по источнику — в приложении/на reverse proxy |
| `order_obligation_segments` | Части обязательств включены в `order_version_lines` с `segment_key` |
| `claim_targets` | Один первичный объект нарушения в `claims`; ограничение первой версии |
| `claim_events` | Статус/итог в `claims`, сообщения в `interactions`, переходы и решения в `audit_events` |
| `claim_delivery_links` | Опциональная ссылка `delivery_lines.claim_id` |
| `expert_assessment_evidence` | Обоснование внутри `expert_assessments`; связанные сообщения/претензии доступны по поставщику |
| `rating_runs` | Период, срез, методика и запуск отражены в каждом `supplier_ratings` |
| `rating_criterion_results` | Четыре явных столбца критериев в `supplier_ratings` |
| `rating_delivery_inputs` | Неизменяемые факты + `rating_line_inputs` |
| `rating_acceptance_inputs` | Неизменяемые факты + `rating_line_inputs` |
| `rating_return_inputs` | Неизменяемые факты + `rating_line_inputs` |

**Сохраняем сознательно** `supplier_status_requests`, `order_closures` + `order_closure_lines`, `fact_corrections`, `rating_line_inputs`. Это не технические «микросущности»: они обеспечивают независимое согласование смены статуса, историческую фиксацию остатка, контролируемые исправления и обоснованный рейтинг соответственно.

## 3. Основные связи и правила целостности

- `User` 1:N `UserRoleAssignment`, 1:N `AuthSession`.
- `ProductCategory` 1:N `Product`; `Unit` 1:N `Product`.
- `Supplier` M:N `Product` **через** `SupplierProduct`.
- `Supplier` 1:N `SupplierStatusRequest`, `Contract`, `PurchaseOrder`, `Interaction`, `Claim`, `ExpertAssessment`, `SupplierRating`.
- `Contract` 1:N `ContractVersion`; `PurchaseOrder` N:1 `Contract` и `Supplier`; `OrderVersion` N:1 `ContractVersion`.
- `PurchaseOrder` 1:N `OrderLine`, `OrderVersion`, `OrderClosure`, `Delivery`; `OrderVersion` 1:N `OrderVersionLine`; `OrderLine` 1:N `OrderVersionLine`.
- `OrderClosure` 1:N `OrderClosureLine` на каждое закрытие; `OrderClosureLine` N:1 `OrderLine`.
- `Delivery` 1:N `DeliveryLine`, N:1 `PurchaseOrder`; `DeliveryLine` N:1 `OrderVersionLine`. `AcceptanceDocument` N:1 `Delivery`; `AcceptanceLine` N:1 `AcceptanceDocument` и N:1 `DeliveryLine`.
- `SupplierReturn` 1:N `SupplierReturnLine`; `SupplierReturnLine` N:1 `AcceptanceLine`.
- `FactCorrection` 1:N исправленные `Delivery`/`AcceptanceDocument`/`SupplierReturn` посредством `correction_id`.
- `Interaction` N:1 `Supplier`, optional N:1 `Claim`. `Claim` N:1 `Supplier`, optional N:1 `OrderLine`/`DeliveryLine`; целостность поставщика и заказа проверяется на сервере и, где возможно, составными FK.
- `ExpertAssessment` N:1 `Supplier`; `SupplierRating` N:1 `Supplier` и optional N:1 `ExpertAssessment`; `RatingLineInput` N:1 `SupplierRating` и `OrderVersionLine`.

**Ограничения:**

1. В текущих связях FK, UQ, CHECK, NOT NULL, индексы по фильтрам и архивным датам. Не создавать технические таблицы для каждой смены статуса.
2. Не допускать ссылку на версию чужого заказа/договора, на чужую товарную строку, на приёмку другой поставки или претензию другого поставщика. Для сложных междокументных инвариантов нужны транзакции с блокировкой и при необходимости составные FK/DB-триггеры.
3. Открытые заявки/предложенные версии уникальны **частичными индексами**; индекс только по UUID не обеспечивает этот бизнес-инвариант.
4. После `APPROVED/CONFIRMED` бизнес-факты неизменяемы. Исправления — новые строки с `supersedes_id` и `correction_id`; исходная запись остаётся видимой. Для рейтингов новые строки `supersedes_id`.
5. Числа: `numeric(18,3)` для количества, `numeric(19,2)` для BYN, `numeric(5,1)` для баллов. Денежные итоги считаются по строкам, не хранятся в трёх местах.
6. Время: `date` для плановых/фактических календарных дат, `timestamptz`/`Instant` для регистрации и решений. Часовой пояс бизнеса — `Europe/Minsk` из конфигурации, UTC в базе для моментов времени.
7. Для конкурентных приёмок/возвратов, изменений количества и закрытий: `@Transactional` + блокировка родительского заказа/документа, затем пересчёт остатков и проверка. `@Version` у изменяемых корней; отдельная бизнес-версия в `*_versions`.
8. Нельзя удалять пользователей, товары, договоры и подтверждённые документы, на которые ссылается история; используется деактивация/архивный статус.
9. Поля решений `decided_by` + `decided_at` должны быть согласованы с состоянием; автор и утверждающий не могут совпадать.
10. Реализация ограничений, которые SQL CHECK одной строки не способен выразить (сумма приёмок, возвратов, цепочка исправлений, объём по сегментам), принадлежит сервису/транзакции и интеграционным тестам; часть можно дополнительно закрепить триггерами БД.

## 4. Почему не 20–25 таблиц

Свести до 20–25 возможно **только через изменение утверждённых сценариев**, например: запрет частичных приёмок, отказ от версии договора/заказа, отсутствие независимого согласования статусов, отсутствие возвратов, отказ от исправления подтверждённых событий, отсутствие исторических снимков расчёта/закрытия или сведение всего к полиморфным JSON-таблицам. Это ухудшит целостность, отчётность либо аудит. **30 — предпочтительный баланс функционала и технического качества.**

Если в дальнейшем функциональные требования будут формально сокращены, условно можно отказаться от `supplier_status_requests` (статус меняет только руководитель напрямую) и `order_closure_lines` (отсутствует требование воспроизводить результат закрытия по строкам), но **в эту целевую модель они входят**: они защищают уже согласованные процессы.

## 5. Соответствие UI и данным

| Минимальный экран/функция | Источник данных |
|---|---|
| Логин, профиль, управление пользователями | `users`, `user_role_assignments`, `auth_sessions` |
| Главная (единый каркас с ролевыми блоками) | агрегаты по заказам, поставкам, претензиям, рейтингам и пользователям |
| Реестр/карточка поставщика | `suppliers`, `supplier_products`, связи с договорами/заказами/рейтингами |
| Каталог | `products`, `product_categories`, `units` |
| Договоры, версии, согласования | `contracts`, `contract_versions` |
| Заказы, изменения, согласования, закрытие | `purchase_orders`, `order_lines`, `order_versions`, `order_version_lines`, `order_closures`, `order_closure_lines` |
| Поступления, приёмка, возврат, исправление | семь таблиц исполнения |
| Взаимодействия, претензии и их история | `interactions`, `claims`, `audit_events` |
| Рейтинги, критерии, динамика, отчётность | `expert_assessments`, `supplier_ratings`, `rating_line_inputs` |
| Аудит | `audit_events` (с разграничением бизнес/security событий) |

Не нужны отдельные таблицы `notifications`, `dashboard_metrics`, `reports`, `search_index`, `supplier_cards`, `rating_method_editor`. Списки и отчёты формируются серверными запросами с пагинацией и фильтрами.

## 6. Правила реализации с Spring Data JPA

- 30 `@Entity`, сопоставленные миграции PostgreSQL. `JpaRepository<T, UUID>` (либо `@EmbeddedId` для составного ключа), типизированные запросы; `@ManyToOne(fetch=LAZY)` и только нужные двусторонние коллекции. Лишние eager-связи запрещены.
- `Spring Data JPA` — основной механизм работы с БД; native SQL — только специализированные чтения/агрегаты внутри репозиториев. Никакого универсального `Db`, `Map<String,Object>` как API-ответа.
- `service` содержит транзакционные бизнес-правила; `controller` HTTP; `dto.request`/`dto.response` строгие контракты; `mapper` преобразования; `entity` — только сохранённые состояния. Пакеты группируются по слоям и подпакетам (согласованный подход).
- `Flyway` — единственный создатель схемы; Hibernate `ddl-auto=validate`.
- API не выдаёт сущности напрямую и не отдаёт полные графы зависимостей; списки — `Page`/проекции, отдельные DTO карточек и команд.
- Паттерны: `Strategy` для критериев рейтинга, явные допустимые переходы состояний через `State` или чистую state-machine policy (без 50 классов состояний).
- Сначала unit-тесты расчёта рейтинга и допусков переходов; затем интеграционные с PostgreSQL Testcontainers: миграции, JPA-валидация, FK/unique, конкурентные приёмки/возвраты, версии договоров/заказов, закупочный цикл и доступы.
- Требуемая приёмка: успешная сборка `mvn clean verify`, Flyway на пустой БД, JPA `validate`, интеграционные тесты, OpenAPI и запуск без IDE. **Этот файл является проектной моделью; перечисленные проверки будущего кода ещё не выполнялись.**

## 7. Контрольные сценарии до начала пересборки

1. Специалист создаёт поставщика, товары и связку поставщик–товары; руководитель одобряет активацию; специалист любого пользователя может оформлять заказ.
2. Новая версия договора регистрируется без изменения прежней; чужой договор нельзя назначить заказу.
3. Заказ 100 шт. со сроком D; покупатель увеличивает на 50 шт. со сроком D2: новый набор сегментов в `order_version_lines`, старые 100 сохраняют свой рейтинговый срок; новая часть 50 имеет свой.
4. Поставщик просит перенести 100 шт. с D на D2: срок исполнения изменён, рейтинговый срок прежний.
5. Приход 100, частичная приёмка 60+30, брак 10; повторная приёмка не может превысить фактическое поступление.
6. Возврат 5 из ранее принятых 90: остаток исполненного уменьшается, качество меняется без двойного счёта проверенных штук.
7. Параллельные возвраты/приёмки не проходят сверх доступного остатка.
8. Пакетное исправление ошибочного прихода утверждено отдельно; старый рейтинг остаётся, создаётся пересчёт с новой исторической версией.
9. Претензия по одной строке/поставке; история ответов в `interactions`, история решений в `audit_events`.
10. Рейтинг при отсутствии качества/эксперта остаётся `PRELIMINARY` с NULL итогом; при достаточной выборке публикуются балл, категория и четыре критерия.
11. Закрытие с остатком фиксирует по строкам снимок неисполненного количества; поздний возврат и новое решение не переписывают старое закрытие.
12. Самосогласование запрещено при любом сочетании ролей, администратор без закупочной роли не может принимать бизнес-решения.

## 8. Что необходимо сделать затем

1. **Зафиксировать эту v2 как новую модель**, отказавшись от Stage 1 43-table как базы кода.
2. Написать с нуля согласованную `V1__schema.sql` для **новой пустой БД**: 30 таблиц, ключи/ограничения/индексы/уникальность открытых объектов, правила неизменяемости. Предыдущая V1 несовместима: её нельзя поверх применить как V2 без миграции данных.
3. Создать 30 JPA Entity, составные ключи где нужно, ассоциации, Enum, `@Version`; репозитории и маппинг проверить вместе с PostgreSQL.
4. Реализовать сервисы и DTO **вертикальными функциональными срезами**: доступ → каталог/поставщики → договоры/заказы → поставки/возвраты → претензии/эксперт → рейтинг/отчёты.
5. Только после `mvn clean verify` и интеграционной приёмки — окончательная интеграция frontend. Утверждённые стили и навигацию не пересматривать без нужды.

### Случаи, требующие осознанного решения при изменении требований

- Если одна претензия должна покрывать несколько товарных строк/поставок, надо вернуть таблицу `claim_targets` (+1). Здесь сознательно выбран *один первичный инцидент*.
- Если refresh-ротация обязана сохранять отдельную запись каждого поколения токена, вернётся `auth_refresh_tokens` (+1). Для MVP текущая сессия с хешем/поколением достаточна при корректной атомарной ротации и отзыве.
- Если нужно прослеживание рейтинга до **каждого** конкретного входного документа без реконструкции из неизменяемых событий и `as_of`, понадобятся таблицы связей доказательств. Для курсовой первой версии достаточно неизменяемых агрегатов `rating_line_inputs`.
- Если реальные файлы/электронные письма/много контактных лиц станут обязательными, добавятся специализированные таблицы **после** появления соответствующих требований.
