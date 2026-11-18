# Stage 4 — совместимость с frontend Stage 2

Базовый backend Stage 4 сохранён без изменения бизнес-логики и HTTP-маршрутов.

Изменены только:
- `src/main/java/by/bsuir/supplier/dto/Views.java`: к `OrderVersionLineView` добавлены `productId` и `lineNo`, вычисляемые из `OrderLine`.
- `docs/openapi.json`: схема ответа обновлена в соответствии с DTO.

Причина: UI редактирования черновика и отвергнутой версии должен знать исходную позицию заказа и товар, а не восстанавливать идентификатор по неоднозначному названию товара.

Проверка: `bash tools/run-offline-checks.sh` проходит; Maven, Hibernate и PostgreSQL в этом окружении недоступны, поэтому реальная интеграционная приёмка не проведена.
