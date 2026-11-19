import { readFileSync } from 'node:fs';
import assert from 'node:assert/strict';
import { api } from '../src/core/api.ts';
import { allRows, supplierTransitions, canDecide, toOrderItems } from '../src/core/data.ts';
import { buildOperationRequest } from '../src/core/operationsApi.ts';

let assertions = 0;
const id = '11111111-1111-4111-8111-111111111111';
const date = '2026-10-30';
const item = { productId: id, quantity: '2', unitPrice: '12.50', dueOn: date };
const use = (kind, value, lines = []) => ({
  kind,
  req: buildOperationRequest(kind, id, value, lines),
});
const samples = [
  use('password-change', { currentPassword: 'OldPass12!', newPassword: 'NewPass12!' }),
  use('user-reset', { newPassword: 'NewPass12!' }),
  use('user-add-role', { role: 'PROCUREMENT_MANAGER' }),
  use('contract-revision', {
    number: 'DS-1',
    validFrom: date,
    validTo: '2026-12-30',
    terms: 'Поставка товаров',
    reason: 'Продление',
  }),
  use('order-draft', {}, [item]),
  use('order-revise', { reason: 'Исправлены условия' }, [item]),
  use('order-increase', {
    orderLineId: id,
    additional: '1.25',
    unitPrice: '10.5',
    dueOn: date,
    reason: 'Дополнительная потребность',
  }),
  use('order-reschedule', {
    segmentKey: 'segment-1',
    newDueOn: '2026-12-30',
    reason: 'Запрос поставщика',
  }),
  use('return-create', { returnedOn: date, reason: 'Брак' }, [
    { acceptanceLineId: id, quantity: '1.25', defectDescription: 'Повреждена упаковка' },
  ]),
  use('delivery-correction', { arrivedOn: date, reason: 'Исправление даты' }, [
    { approvedSegmentId: id, quantity: '2.5' },
  ]),
  use('acceptance-correction', { inspectedOn: date, reason: 'Ошибка акта' }, [
    {
      deliveryLineId: id,
      accepted: '1',
      qualityRejected: '0',
      otherRejected: '1',
      reason: 'Иное отклонение',
    },
  ]),
  use('claim-send', { channel: 'EMAIL' }),
  use('claim-resolve', { result: 'FULL', remedy: 'REPLACEMENT', description: 'Товар заменён' }),
  use('claim-create', {
    number: 'CL-1',
    supplierId: id,
    orderLineId: id,
    type: 'QUALITY',
    description: 'Брак',
    requirements: 'Замена',
  }),
  use('assessment-create', {
    supplierId: id,
    periodStart: date,
    periodEnd: date,
    applicableFrom: date,
    applicableTo: '2026-11-15',
    score: '75',
    justification: 'Взаимодействие удовлетворительно',
  }),
  use('rating-calculate', { supplierId: id, scope: 'OVERALL' }),
];
const openapi = JSON.parse(
  readFileSync(new URL('../docs/backend-openapi-stage4.json', import.meta.url), 'utf8'),
);
const schemas = openapi.components.schemas;
const resolveSchema = (s) => (s?.$ref ? schemas[s.$ref.split('/').at(-1)] : s);
function validate(schema, data, path = 'root') {
  schema = resolveSchema(schema);
  if (!schema) return;
  if (data == null) {
    assert.ok(!schema.required, `${path} не может быть пустым`);
    return;
  }
  if (schema.type === 'object' || schema.properties) {
    assert.equal(typeof data, 'object', path);
    for (const key of schema.required || []) {
      assert.ok(Object.hasOwn(data, key), `${path}.${key} обязателен`);
      assert.notEqual(data[key], undefined, `${path}.${key} отсутствует`);
    }
    for (const [key, entry] of Object.entries(schema.properties || {})) {
      if (data[key] !== undefined && data[key] !== null)
        validate(entry, data[key], `${path}.${key}`);
    }
  } else if (schema.type === 'array') {
    assert.ok(Array.isArray(data), `${path}: array`);
    data.forEach((v, i) => validate(schema.items, v, `${path}[${i}]`));
  } else if (schema.type === 'string') {
    assert.equal(typeof data, 'string', `${path} string`);
    if (schema.format === 'uuid')
      assert.match(data, /^[0-9a-f]{8}-[0-9a-f-]{27,}$/i, `${path}: uuid`);
    if (schema.format === 'date') assert.match(data, /^\d{4}-\d{2}-\d{2}$/, `${path}: date`);
    if (schema.enum) assert.ok(schema.enum.includes(data), `${path}: enum`);
  } else if (['integer', 'number'].includes(schema.type)) {
    assert.equal(typeof data, 'number', `${path}: number`);
    assert.ok(Number.isFinite(data), `${path}: finite`);
  }
  assertions++;
}
for (const { kind, req } of samples) {
  const route =
    '/api/v1' + req.endpoint.replace(/\/11111111-1111-4111-8111-111111111111/g, '/{id}');
  // /fulfillment/orders/{orderId} has a distinct path parameter name.
  const matched = Object.entries(openapi.paths).find(([path, methods]) => {
    const regexp = new RegExp(
      '^' +
        path.replace(/[.*+?^${}()|[\]\\]/g, '\\$&').replace(/\\\{[a-zA-Z]+\\\}/g, '[^/]+') +
        '$',
    );
    return regexp.test('/api/v1' + req.endpoint) && methods[req.method.toLowerCase()];
  });
  assert.ok(matched, `${kind}: маршрут ${req.method} ${route} не найден`);
  const schema =
    matched[1][req.method.toLowerCase()].requestBody?.content?.['application/json']?.schema;
  assert.ok(schema, `${kind}: отсутствует request schema`);
  validate(schema, req.payload, kind);
  assertions += 2;
}
assert.deepEqual(supplierTransitions('CANDIDATE'), ['ACTIVE', 'TERMINATED']);
assertions++;
assert.deepEqual(supplierTransitions('ACTIVE'), ['SUSPENDED', 'TERMINATED']);
assertions++;
assert.deepEqual(supplierTransitions('SUSPENDED'), ['ACTIVE', 'TERMINATED']);
assertions++;
assert.deepEqual(supplierTransitions('TERMINATED'), []);
assertions++;
assert.equal(canDecide(id, id, true), false);
assert.equal(canDecide('another', id, true), true);
assert.equal(canDecide('another', id, false), false);
assertions += 3;
for (const [kind, value, lines] of [
  [
    'order-increase',
    { orderLineId: id, additional: '0', unitPrice: '1', dueOn: date, reason: 'test' },
    [],
  ],
  ['order-draft', {}, [{ ...item, quantity: '-1' }]],
  ['return-create', { returnedOn: date, reason: 'test' }, []],
  ['claim-create', { supplierId: id }, []],
]) {
  assert.throws(() => buildOperationRequest(kind, id, value, lines), Error);
  assertions++;
}
assert.deepEqual(toOrderItems([item]), [
  { productId: id, quantity: 2, unitPrice: 12.5, dueOn: date },
]);
assertions++;
let requests = 0;
api.page = async (_p, page) => {
  requests++;
  return { items: [{ id: String(page) }], totalPages: 4, totalElements: 4, page, size: 100 };
};
assert.equal((await allRows('/catalog/products')).length, 4);
assert.equal(requests, 4);
assertions += 2;
console.log(
  `PASS: ${samples.length} контрактов новых сценариев, ${assertions} утверждений (валидация запросов по Stage 4 OpenAPI + роль/состояния/пагинация)`,
);
