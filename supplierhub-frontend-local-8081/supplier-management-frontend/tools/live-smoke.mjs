/** Real HTTP acceptance smoke: MUTATES DATA. Run against a disposable PostgreSQL database.
 * Requires Java backend Stage 4 to be fully running, ADMIN_LOGIN and ADMIN_PASSWORD.
 * This file is deliberately not executed in offline CI: it requires the live server/DB. */
import assert from 'node:assert/strict';
import { randomUUID } from 'node:crypto';
const base = (process.env.SMOKE_API_BASE || 'http://localhost:8081/api/v1').replace(/\/$/, '');
const loginName = process.env.SMOKE_ADMIN_LOGIN || '';
const password = process.env.SMOKE_ADMIN_PASSWORD || '';
if (!loginName || !password) {
  console.error(
    'Укажите SMOKE_ADMIN_LOGIN и SMOKE_ADMIN_PASSWORD. Используйте только тестовую БД.',
  );
  process.exit(2);
}
const stamp = randomUUID().slice(0, 8);
const secret = `S3cure-${randomUUID()}!`;
const today = new Intl.DateTimeFormat('en-CA', {
  timeZone: 'Europe/Minsk',
  year: 'numeric',
  month: '2-digit',
  day: '2-digit',
}).format(new Date());
const plus = (days) => {
  const d = new Date(today + 'T12:00:00Z');
  d.setUTCDate(d.getUTCDate() + days);
  return d.toISOString().slice(0, 10);
};
const ensure = (value, msg) => {
  assert.ok(value, msg);
  return value;
};
const assertStatus = (value, expected, ctx) => assert.equal(value, expected, ctx);
const account = {};
let checks = 0;
async function http(token, method, path, body) {
  const response = await fetch(base + path, {
    method,
    headers: {
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
    },
    body: body === undefined ? undefined : JSON.stringify(body),
  });
  const contentType = response.headers.get('content-type') || '';
  const data = contentType.includes('json')
    ? await response.json()
    : contentType.includes('spreadsheet')
      ? await response.arrayBuffer()
      : await response.text();
  if (!response.ok)
    throw new Error(
      `${method} ${path}: HTTP ${response.status}: ${JSON.stringify(data).slice(0, 450)}`,
    );
  checks++;
  return data;
}
const call = (who, method, path, body) => http(account[who], method, path, body);
const log = (phase) => console.log(`[OK] ${phase}`);
try {
  const admin = await http(null, 'POST', '/auth/login', { login: loginName, password });
  account.admin = ensure(admin.accessToken, 'Admin token');
  log('Вход администратора');
  const specialist = await call('admin', 'POST', '/admin/users', {
    login: `spec_${stamp}`,
    displayName: `Тест специалист ${stamp}`,
    password: secret,
    roles: ['PROCUREMENT_SPECIALIST'],
  });
  const manager = await call('admin', 'POST', '/admin/users', {
    login: `mgr_${stamp}`,
    displayName: `Тест руководитель ${stamp}`,
    password: secret,
    roles: ['PROCUREMENT_MANAGER'],
  });
  account.specialist = (
    await http(null, 'POST', '/auth/login', { login: specialist.login, password: secret })
  ).accessToken;
  account.manager = (
    await http(null, 'POST', '/auth/login', { login: manager.login, password: secret })
  ).accessToken;
  assert.ok(account.specialist && account.manager);
  checks++;
  log('Создание сотрудников и ролевые входы');
  const category = await call('manager', 'POST', '/catalog/categories', {
    code: `CT${stamp}`,
    name: `Тестовая категория ${stamp}`,
  });
  const unit = await call('manager', 'POST', '/catalog/units', {
    code: `U${stamp}`,
    name: `Тестовая единица ${stamp}`,
  });
  const product = await call('specialist', 'POST', '/catalog/products', {
    sku: `PRD${stamp}`,
    name: `Тестовый товар ${stamp}`,
    categoryId: category.id,
    unitId: unit.id,
  });
  const supplier = await call('specialist', 'POST', '/suppliers', {
    name: `Тестовый поставщик ${stamp}`,
    contactName: 'Тестовый контакт',
    email: `supplier-${stamp}@example.org`,
    phone: '',
    address: '',
    registrationNumber: '',
  });
  await call('specialist', 'POST', `/suppliers/${supplier.id}/products`, {
    productId: product.id,
    supplierItemCode: 'TEST',
  });
  const sr = await call('specialist', 'POST', `/suppliers/${supplier.id}/status-requests`, {
    status: 'ACTIVE',
    reason: 'Тест запуска',
  });
  await call('manager', 'POST', `/suppliers/status-requests/${sr.id}/decision`, {
    approve: true,
    comment: 'Разрешено для проверки',
  });
  assertStatus(
    (await call('specialist', 'GET', `/suppliers/${supplier.id}`)).status,
    'ACTIVE',
    'Статус после утверждения',
  );
  checks++;
  log('Справочники → поставщик → ассортимент → независимая активация');
  const contract = await call('specialist', 'POST', '/contracts', {
    supplierId: supplier.id,
    terms: {
      number: `CN-${stamp}`,
      signedOn: today,
      validFrom: plus(-1),
      validTo: plus(90),
      terms: 'Условия тестового договора',
    },
  });
  const cv = (await call('specialist', 'GET', `/contracts/${contract.id}/versions`))[0];
  await call('specialist', 'POST', `/contracts/versions/${cv.id}/submit`);
  await call('manager', 'POST', `/contracts/versions/${cv.id}/decision`, {
    approve: true,
    comment: '',
  });
  assertStatus(
    (await call('specialist', 'GET', `/contracts/${contract.id}`)).state,
    'REGISTERED',
    'Статус договора',
  );
  checks++;
  log('Договор → отправка → независимое утверждение');
  const order = await call('specialist', 'POST', '/orders', {
    number: `ORD-${stamp}`,
    supplierId: supplier.id,
    contractId: contract.id,
    items: [{ productId: product.id, quantity: 5, unitPrice: 10.25, dueOn: plus(15) }],
  });
  const versions = await call('specialist', 'GET', `/orders/${order.id}/versions`);
  const first = ensure(
    versions.find((x) => x.kind === 'INITIAL'),
    'Исходная редакция',
  );
  await call('specialist', 'POST', `/orders/versions/${first.id}/submit`);
  await call('manager', 'POST', `/orders/versions/${first.id}/decision`, {
    approve: true,
    comment: '',
  });
  const approved = ensure(
    (await call('specialist', 'GET', `/orders/${order.id}/versions`)).find(
      (v) => v.state === 'APPROVED',
    ),
    'Нет утверждённой версии',
  );
  const line = ensure(
    (await call('specialist', 'GET', `/orders/versions/${approved.id}/lines`))[0],
    'Нет строки заказа',
  );
  assert.equal(line.productId, product.id);
  checks++;
  log('Заказ → согласование → идентификаторы товаров в DTO');
  const delivery = await call('specialist', 'POST', `/fulfillment/orders/${order.id}/deliveries`, {
    arrivedOn: today,
    externalNumber: `DN-${stamp}`,
    items: [{ segmentId: line.id, quantity: 3 }],
  });
  await call('specialist', 'POST', `/fulfillment/deliveries/${delivery.id}/confirm`);
  const received = ensure(
    (await call('specialist', 'GET', `/fulfillment/deliveries/${delivery.id}/lines`))[0],
    'Нет строки поступления',
  );
  const inspected = await call(
    'specialist',
    'POST',
    `/fulfillment/deliveries/${delivery.id}/acceptances`,
    {
      inspectedOn: today,
      items: [
        {
          deliveryLineId: received.id,
          accepted: 2,
          qualityRejected: 1,
          otherRejected: 0,
          reason: null,
        },
      ],
    },
  );
  await call('specialist', 'POST', `/fulfillment/acceptances/${inspected.id}/confirm`);
  const accepted = ensure(
    (await call('specialist', 'GET', `/fulfillment/acceptances/${inspected.id}/lines`))[0],
    'Нет строки приёмки',
  );
  log('Поставка → подтверждение → частичная приёмка');
  const claim = await call('specialist', 'POST', '/relationships/claims', {
    number: `CL-${stamp}`,
    supplierId: supplier.id,
    orderLineId: line.orderLineId,
    deliveryLineId: received.id,
    type: 'QUALITY',
    description: 'Проверочный дефект',
    requirements: 'Устранить нарушение',
    responseDueOn: plus(10),
  });
  await call('specialist', 'POST', `/relationships/claims/${claim.id}/send`, { channel: 'EMAIL' });
  await call('specialist', 'POST', `/relationships/claims/${claim.id}/begin-resolution`);
  await call('specialist', 'POST', `/relationships/claims/${claim.id}/resolve`, {
    result: 'FULL',
    remedy: 'REPLACEMENT',
    description: 'Тестовое урегулирование',
  });
  assertStatus(
    (await call('specialist', 'GET', `/relationships/claims/${claim.id}`)).state,
    'CLOSED',
    'Завершение претензии',
  );
  checks++;
  log('Претензия → отправка → урегулирование');
  const ret = await call('specialist', 'POST', `/fulfillment/orders/${order.id}/returns`, {
    returnedOn: today,
    reason: 'Дефект',
    items: [
      { acceptanceLineId: accepted.id, quantity: 1, defectDescription: 'Проверочный возврат' },
    ],
  });
  await call('specialist', 'POST', `/fulfillment/returns/${ret.id}/submit`);
  await call('manager', 'POST', `/fulfillment/returns/${ret.id}/decision`, {
    approve: true,
    comment: '',
  });
  log('Возврат → независимое согласование');
  const assessment = await call('specialist', 'POST', '/ratings/expert-assessments', {
    supplierId: supplier.id,
    periodStart: plus(-29),
    periodEnd: today,
    applicableFrom: today,
    applicableTo: plus(29),
    score: 75,
    justification: 'Тестовая экспертная оценка',
  });
  await call('specialist', 'POST', `/ratings/expert-assessments/${assessment.id}/submit`);
  await call('manager', 'POST', `/ratings/expert-assessments/${assessment.id}/decision`, {
    approve: true,
    comment: '',
  });
  const rating = await call('manager', 'POST', '/ratings/calculate', {
    supplierId: supplier.id,
    scope: 'OVERALL',
    categoryId: null,
  });
  assert.ok(['PRELIMINARY', 'PUBLISHED', 'INSUFFICIENT_DATA'].includes(rating.state));
  checks++;
  log('Экспертная оценка → согласование → рейтинг');
  const summary = await call('specialist', 'GET', '/dashboard');
  assert.ok(typeof summary.suppliers === 'number');
  checks++;
  const report = await call('specialist', 'GET', '/reports/suppliers');
  assert.ok(Array.isArray(report));
  checks++;
  const xlsx = await fetch(base + '/reports/suppliers.xlsx', {
    headers: { Authorization: `Bearer ${account.specialist}` },
  });
  assert.equal(xlsx.status, 200);
  const bytes = new Uint8Array(await xlsx.arrayBuffer());
  assert.equal(String.fromCharCode(...bytes.slice(0, 2)), 'PK');
  checks += 2;
  log('Панель и аналитический XLSX');
  const roles = await call('admin', 'GET', `/admin/users/${specialist.id}/roles`);
  assert.ok(roles.includes('PROCUREMENT_SPECIALIST'));
  checks++;
  await call('admin', 'POST', `/admin/users/${specialist.id}/roles`, {
    role: 'PROCUREMENT_MANAGER',
  });
  await call('admin', 'DELETE', `/admin/users/${specialist.id}/roles/PROCUREMENT_MANAGER`);
  await call('admin', 'POST', `/admin/users/${specialist.id}/block`, {
    reason: 'Завершение smoke-теста',
  });
  log('Управление ролями и блокировка');
  console.log(
    `PASS: ${checks} реальных HTTP-операций и утверждений; последовательность на PostgreSQL завершена. Тестовые записи помечены ${stamp}.`,
  );
} catch (error) {
  console.error(`FAIL after ${checks} успешных операций/проверок:`, error.stack || error);
  process.exitCode = 1;
}
