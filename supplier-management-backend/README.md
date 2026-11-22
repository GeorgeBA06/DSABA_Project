# Supplier Management — Backend Stage 4 (REST + Security), DTO refactor

**Дата:** 31 октября 2026 года. **База:** согласованная минимальная модель с 30 таблицами и 30 JPA Entity. Это **не результат подтверждённого production-развёртывания**: полноценная сборка с настоящими библиотеками и PostgreSQL здесь недоступна. Предыдущие архивы не требуются — данный проект включает слои Entity, Repository, Service, Security и Controller.

## Назначение и архитектура

Java 21, Spring Boot 3.5.9, Spring Data JPA, Flyway, PostgreSQL 16, Spring Security OAuth2 Resource Server, Nimbus JWT, BCrypt, REST/JSON. Вся предметная логика на backend. Пакеты разделены по слоям (`entity`, `repository`, `service`, `dto`, `controller`, `security`), внутри слоёв — по бизнес-модулям. HTML frontend в архив не входит.

### Читаемая структура Java-контрактов

- `dto/request/`: **40 отдельных `record`** для входных запросов (`SupplierRequest`, `OrderCreateRequest` и др.).
- `dto/response/`: **34 отдельных `record`** для выходных JSON-проекций (`SupplierView`, `OrderVersionView`, `MeView`, `Tokens` и др.).
- `mapper/`: **9 модульных Mapper-классов** для преобразования JPA Entity → выходные DTO.
- Бывшие большие контейнеры `Requests.java` и `Views.java` удалены.
- Java-код отформатирован, импорты организованы, используется UTF-8/LF и `.editorconfig`.

**`View` здесь — это response DTO, а не SQL-представление БД.** Архитектура и причина такого разделения описаны в
[`docs/DTO_REFACTOR_ARCHITECTURE.md`](docs/DTO_REFACTOR_ARCHITECTURE.md).
**JSON/REST-контракт остался без изменений**: его идентичность проверяется по исходному снимку OpenAPI.

Из клиентского приложения доступны поставщики, каталог, договоры и версии, заказы и изменения, поступления/приёмка, возвраты и корректировки, претензии, взаимодействия, экспертные оценки, рейтинги, информационная панель, отчёты XLSX и администрирование. Полный контракт: [`docs/openapi.json`](docs/openapi.json) (97 HTTP-операций). REST-контроллеры не сериализуют JPA Entity напрямую.

## Локальный запуск (Linux, WSL, macOS или PowerShell с OpenSSL)

Нужны **Java 21, Maven 3.9+, Docker/Compose и доступ к Maven Central при первой сборке**. PostgreSQL запускается Docker-контейнером; продакшен-окружение должно передавать реальные секреты через менеджер секретов.

```bash
# 1. Находясь в корне распакованного архива:
bash tools/generate-local-keys.sh  # однократная генерация 3072-bit RSA пары в secrets/

# 2. Запустить PostgreSQL:
docker compose up -d postgres

# 3. Установить переменные окружения (НЕ используйте этот пароль в публичном окружении):
export DB_URL='jdbc:postgresql://localhost:5432/supplier_management'
export DB_USER=supplier
export DB_PASSWORD=supplier_local
export BOOTSTRAP_ADMIN_LOGIN=admin
export BOOTSTRAP_ADMIN_PASSWORD='REPLACE_WITH_A_RANDOM_PASSWORD_AT_LEAST_12_CHARACTERS'
export JWT_PRIVATE_KEY_PATH="$(pwd)/secrets/jwt-private.pem"
export JWT_PUBLIC_KEY_PATH="$(pwd)/secrets/jwt-public.pem"

# 4. Собрать и выполнить тесты с PostgreSQL Testcontainers:
mvn clean verify

# 5. Запустить API:
mvn spring-boot:run
```

**Windows PowerShell:** `docker compose up -d postgres`, `mvn clean verify`, `mvn spring-boot:run`; переменные окружения задаются как `$env:DB_USER='supplier'`. Ключи генерируются при наличии OpenSSL: `openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out secrets/jwt-private.pem` и `openssl pkey -in secrets/jwt-private.pem -pubout -out secrets/jwt-public.pem`. Каталог `secrets` создать заранее. `.env.example` — образец конфигурации, он **не загружается автоматически** при запуске Spring Boot; переменные необходимо установить в оболочке, системе CI или конфигурации окружения.

Первый запуск с пустой БД требует `BOOTSTRAP_ADMIN_LOGIN` и `BOOTSTRAP_ADMIN_PASSWORD`; без них загрузка не завершится. Создание администратора выполняется единожды, если нет пользователей. После первого запуска bootstrap-переменные можно убрать из окружения. **Нет публичной регистрации**: остальные учётные записи создаёт администратор через REST.

Проверка работоспособности: `GET http://localhost:8080/actuator/health`. Для получения access-токена выполните:

```bash
curl -s -X POST http://localhost:8080/api/v1/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"login":"admin","password":"ВАШ_РЕАЛЬНЫЙ_ПАРОЛЬ"}'
```

Токены возвращаются в JSON (`accessToken`, `refreshToken`, `tokenType`, `expiresInSeconds`). Остальные защищённые запросы требуют `Authorization: Bearer <accessToken>`. Пример: `curl -H 'Authorization: Bearer <accessToken>' http://localhost:8080/api/v1/auth/me`. В `/auth/me` возвращаются профиль и действующие роли. OpenAPI-файл также доступен после авторизации по `/openapi.json`.

## Защита и полномочия

- Access JWT подписывается **RS256** локальным приватным ключом; срок 15 минут, проверяются подпись, издатель, аудитория, время, назначение токена и `sid`.
- Оpaque refresh-токен существует 7 дней, хранится как SHA-256 в `auth_sessions`, **ротируется на каждом обновлении**. Повторно использованный старый refresh отзывает всю сессию. Выход, блокировка пользователя, сброс/смена пароля отзывают сессии.
- Роли и состояние пользователя, а также действительность сессии проверяются **по базе на каждом API-запросе** — роль из неподтверждённого тела/клиентского JWT не принимается. Специалист не ограничен списком назначенных поставщиков.
- Административная роль сама по себе не разрешает закупочные действия. Самосогласование запрещено серверными сервисами.
- Защита CSRF выключена, поскольку аутентификация выполняется только Bearer-токеном из заголовка и refresh-токеном из JSON, а не автоматически отправляемыми cookies; CORS ограничен указанными origins. Для frontend: не размещать refresh в URL и не сохранять его в `localStorage`; предпочтительно хранение в памяти и повторный вход при перезагрузке страницы.
- Тестовая RSA-пара находится **только** в `src/test/resources`. Никогда не используйте её в реальном окружении.

## Тестирование и ограничения

Автономные проверки, доступные в среде **без Maven, сети и Docker**:

```bash
./tools/run-offline-checks.sh
```

Они проверяют соответствие Entity/DDL, компилируют Java с **локальными имитациями API сторонних библиотек**, запускают 16 автономных расчётных проверок, валидируют OpenAPI и реально создают/читают XLSX. Эти проверки **не доказывают**, что Spring Boot/Hibernate запускается в PostgreSQL.

`mvn clean verify` должен выполнить настоящий `JwtTokenTest`, `SchemaJpaIT`, `ServiceWorkflowIT` и `ApiSecurityWorkflowIT` (реальный HTTP → Security → Service → JPA → PostgreSQL через Testcontainers). До успешного завершения этих тестов и устранения всех дефектов полноценная работоспособность backend **не подтверждена**.

Сервисная основа Stage 3 содержит известные ограничения: нет произвольного исторического восстановления `as-of`; не реализованы все типы изменения утверждённых заказов (например, уменьшение количества), автоматический ежедневный запуск рейтингов и полное пакетное исправление нескольких фактов. Они перечислены в [`docs/STAGE4_REPORT.md`](docs/STAGE4_REPORT.md) и [`docs/STAGE3_SERVICE_REPORT.md`](docs/STAGE3_SERVICE_REPORT.md). Не пытайтесь подключать frontend к операциям, не реализованным на стороне сервисов.

Вся схема V1 рассчитана на **новую чистую PostgreSQL БД**. Старые миграции на 43 таблицы с ней несовместимы; не запускайте эту редакцию поверх базы от первых архивов.

## Демонстрационные данные

Для презентации закупочного цикла используйте [скрипт заполнения через API](../tools/DEMO_DATA.md). Он создаёт сотрудников, поставщиков, каталог, документы на разных этапах и рейтинги.
