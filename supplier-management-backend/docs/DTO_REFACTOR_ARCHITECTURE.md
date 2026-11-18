# Структура DTO, View и Mapper (редакция 31.10.2026)

## Цель

Устранить большие контейнеры `Requests.java` (40 вложенных record) и
`Views.java` (27 вложенных record и преобразования Entity), сохранив
**существующий HTTP/JSON-контракт** для совместимости с текущим frontend.
Рефакторинг не добавляет таблиц, эндпоинтов или бизнес-правил.

## Что такое View в данном приложении

В именах `SupplierView`, `OrderVersionView` и т. п. **View — это выходной
DTO/проекция для чтения через REST API**, а не SQL VIEW в PostgreSQL и не
JPA-сущность. Эти структуры определяют, какие данные увидит клиент.
Такой View нужен, чтобы не сериализовать Entity целиком (с ленивыми связями,
циклами в графе объектов и потенциально приватными полями) и не привязывать
JSON-контракт к физической схеме БД.

При желании в будущем `*View` можно переименовать в `*Response`, но сейчас
сохраняем понятные названия и прежние имена схем OpenAPI.

## Слои

```
src/main/java/by/bsuir/supplier/
├── controller/             # HTTP, статус-коды, @Valid, аутентификация
├── dto/
│   ├── request/            # 40 самостоятельных *Request.java record
│   ├── response/           # 34 самостоятельных *View/response.java record
│   └── PageResponse.java  # общий формат пагинации
├── mapper/                 # 9 модульных *ViewMapper.java
├── entity/<module>/        # 30 JPA Entity; только сохранение данных
├── repository/<module>/    # Spring Data JPA
├── service/<module>/       # транзакции и бизнес-логика
│   └── query/              # сервисы чтения, формируют response DTO
└── security/               # Spring Security/JWT/сессии
```

**Направление преобразования:**
`HTTP JSON -> Request record -> Controller -> Service -> Repository -> Entity`
и обратно:
`Entity -> module ViewMapper -> Response record -> HTTP JSON`.

Пример: `SupplierRequest` (входная валидация), `Supplier` (JPA Entity),
`SupplierView` (разрешённые клиенту поля), `SupplierViewMapper.toView(entity)`.

В `ContractCreateRequest` и `ContractRevisionRequest` вложенное поле `terms`
имеет тип **`ContractTermsRequest`**, а не неявную внутреннюю запись другого класса.
Все самостоятельные контрактные типы находятся в отдельных файлах.

## Сохранение совместимости

- Сохранены **97 HTTP-операций**, пути, параметры и формат успешных ответов.
- Сохранены имена полей `record`, то есть имена JSON-свойств для Jackson.
- Сохранены имена внешних OpenAPI-схем; изменения суффикса `Request` относятся
  к Java-классам, а не к публичной спецификации.
- В `docs/OPENAPI_CONTRACT_BASELINE_STAGE4.json` сохранён снимок исходного
  контракта. `tools/verify_dto_refactor.py` проверяет полную идентичность.
- Frontend использует тот же JSON API, поэтому его исходники менять не нужно.

## Проверки и границы результата

```
python3 tools/generate_openapi.py
bash tools/run-offline-checks.sh
mvn clean verify
```

Офлайн-проверки включают компиляцию Java со **заглушками** Spring/Hibernate,
16 арифметических проверок, проверку Entity/DDL, XLSX и OpenAPI.
`mvn clean verify` с реальными зависимостями и PostgreSQL/Testcontainers
обязателен для реальной технической приёмки и **в этом окружении не выполнен**.

## Развёртывание

Архив содержит **одну корневую папку** `supplier-management-backend/`.
Открывайте эту папку (там находится `pom.xml`) как Maven-проект, а не
создавайте Java-файлы вручную по текстовому содержимому архива.
Все Java-файлы кодированы в UTF-8, используют LF и один публичный тип
на файл; стандарты базового форматирования закреплены в `.editorconfig`.
