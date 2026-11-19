import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../core/api';
import { allRows, todayMinsk, canDecide } from '../core/data';
import { buildOperationRequest } from '../core/operationsApi';
import { useAuth } from '../core/auth';
import { label } from '../core/navigation';
import type { JsonRecord } from '../core/types';
import {
  ErrorBanner,
  Field,
  Loading,
  PageHeading,
  PrimaryButton,
  SecondaryButton,
  SelectInput,
  TextInput,
} from '../ui/Shared';

export const operationTitles: Record<string, string> = {
  'password-change': 'Смена пароля',
  'user-reset': 'Сброс пароля сотрудника',
  'user-add-role': 'Назначение роли',
  'contract-revision': 'Новая редакция договора',
  'order-draft': 'Редактирование черновика заказа',
  'order-revise': 'Переработка отклонённого заказа',
  'order-increase': 'Увеличение заказа',
  'order-reschedule': 'Перенос срока по инициативе поставщика',
  'return-create': 'Оформление возврата',
  'delivery-correction': 'Исправление поступления',
  'acceptance-correction': 'Исправление приёмки',
  'claim-create': 'Создание претензии',
  'assessment-create': 'Новая экспертная оценка',
  'rating-calculate': 'Расчёт рейтинга',
  'claim-send': 'Отправка претензии',
  'claim-resolve': 'Урегулирование претензии',
};
interface FieldSpec {
  key: string;
  name: string;
  kind?: 'text' | 'date' | 'password' | 'number' | 'select' | 'textarea';
  required?: boolean;
  options?: { value: string; label: string }[];
}
const opt = (xs: JsonRecord[], name = 'name') =>
  xs.map((x) => ({ value: String(x.id), label: String(x[name] || x.number || x.id) }));
const emptyOrder = () => ({ productId: '', quantity: '1', unitPrice: '0', dueOn: todayMinsk() });
const chan = [
  ['EMAIL', 'Электронная почта'],
  ['PHONE', 'Телефон'],
  ['MEETING', 'Встреча'],
  ['MESSENGER', 'Мессенджер'],
  ['OTHER', 'Другое'],
];
const options = (pairs: string[][]) => pairs.map(([value, name]) => ({ value, label: name }));

export function OperationPage({
  kind,
  id,
  go,
}: {
  kind: string;
  id?: string;
  go: (url: string) => void;
}) {
  const { can, logout, current } = useAuth();
  const [value, setValue] = useState<JsonRecord>({});
  const [lines, setLines] = useState<JsonRecord[]>([]);
  const [suppliers, setSuppliers] = useState<JsonRecord[]>([]);
  const [categories, setCategories] = useState<JsonRecord[]>([]);
  const [orders, setOrders] = useState<JsonRecord[]>([]);
  const [offers, setOffers] = useState<JsonRecord[]>([]);
  const [orderVersions, setOrderVersions] = useState<JsonRecord[]>([]);
  const [sourceLines, setSourceLines] = useState<JsonRecord[]>([]);
  const [deliveryLines, setDeliveryLines] = useState<JsonRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const update = (key: string, v: string) => setValue((o) => ({ ...o, [key]: v }));
  const changeLine = (i: number, key: string, v: string) =>
    setLines((rows) => rows.map((r, j) => (j === i ? { ...r, [key]: v } : r)));
  const manager = can('PROCUREMENT_MANAGER');
  const admin = can('ADMINISTRATOR');
  const missingId =
    !id &&
    [
      'user-reset',
      'user-add-role',
      'contract-revision',
      'order-draft',
      'order-revise',
      'order-increase',
      'order-reschedule',
      'return-create',
      'delivery-correction',
      'acceptance-correction',
      'claim-send',
      'claim-resolve',
    ].includes(kind);
  const allowed =
    kind === 'password-change' ||
    (['user-reset', 'user-add-role'].includes(kind)
      ? admin
      : kind === 'rating-calculate'
        ? manager
        : can('PROCUREMENT_SPECIALIST', 'PROCUREMENT_MANAGER'));
  const origin = kind.startsWith('order-')
    ? `/orders/${id}`
    : kind.startsWith('contract-')
      ? `/contracts/${id}`
      : kind === 'return-create'
        ? `/orders/${id}`
        : kind === 'delivery-correction'
          ? `/deliveries/${id}`
          : kind === 'acceptance-correction'
            ? `/acceptances/${id}`
            : kind.startsWith('user-')
              ? `/users/${id}`
              : kind.startsWith('claim-')
                ? id
                  ? `/claims/${id}`
                  : '/claims'
                : kind === 'assessment-create'
                  ? '/assessments'
                  : kind === 'rating-calculate'
                    ? '/ratings'
                    : '/dashboard';
  useEffect(() => {
    if (!allowed || missingId || !operationTitles[kind]) return;
    let alive = true;
    setLoading(true);
    setError(null);
    (async () => {
      try {
        const defaultValues: JsonRecord = {
          returnedOn: todayMinsk(),
          arrivedOn: todayMinsk(),
          inspectedOn: todayMinsk(),
          periodEnd: todayMinsk(),
          periodStart: todayMinsk().slice(0, 8) + '01',
          applicableFrom: todayMinsk(),
          applicableTo: todayMinsk(),
          scope: 'OVERALL',
          score: '75',
          channel: 'EMAIL',
          type: 'QUALITY',
          result: 'FULL',
          remedy: 'REPLACEMENT',
        };
        let source: JsonRecord[] = [];
        let vRows: JsonRecord[] = [];
        let offs: JsonRecord[] = [];
        const supplierList = ['claim-create', 'assessment-create', 'rating-calculate'].includes(
          kind,
        )
          ? await allRows('/suppliers')
          : [];
        const cats = kind === 'rating-calculate' ? await allRows('/catalog/categories') : [];
        const orderList = kind === 'claim-create' ? await allRows('/orders') : [];
        if (kind === 'claim-resolve' && id) {
          const claim = await api.get<JsonRecord>(`/relationships/claims/${id}`);
          defaultValues.claimAuthorId = claim.createdById;
        }
        if (kind === 'contract-revision' && id) {
          const versions = await api.get<JsonRecord[]>(`/contracts/${id}/versions`);
          const last = versions.sort((a, b) => Number(b.versionNo) - Number(a.versionNo))[0];
          if (last)
            Object.assign(defaultValues, {
              number: last.number,
              signedOn: last.signedOn || '',
              validFrom: last.validFrom,
              validTo: last.validTo,
              terms: last.terms,
            });
        }
        if (
          ['order-draft', 'order-revise', 'order-increase', 'order-reschedule'].includes(kind) &&
          id
        ) {
          const [order, versions] = await Promise.all([
            api.get<JsonRecord>(`/orders/${id}`),
            api.get<JsonRecord[]>(`/orders/${id}/versions`),
          ]);
          const sorted = [...versions].sort((a, b) => Number(b.versionNo) - Number(a.versionNo));
          const chosen =
            kind === 'order-draft'
              ? sorted.find((v) => v.kind === 'INITIAL' && v.state === 'DRAFT')
              : kind === 'order-revise'
                ? sorted[0]
                : sorted.find((v) => v.state === 'APPROVED');
          if (!chosen) throw new Error('Для операции отсутствует подходящая версия заказа.');
          source = (await api.get<JsonRecord[]>(`/orders/versions/${chosen.id}/lines`)).sort(
            (a, b) => Number(a.lineNo ?? 0) - Number(b.lineNo ?? 0),
          );
          offs = await api.get<JsonRecord[]>(`/suppliers/${order.supplierId}/products`);
          if (['order-draft', 'order-revise'].includes(kind)) {
            const byName = (name: unknown) =>
              offs.filter((o) => o.productName === name && o.active === true);
            vRows = source.map((s) => ({
              productId:
                s.productId ||
                (byName(s.productNameSnapshot).length === 1
                  ? byName(s.productNameSnapshot)[0].productId
                  : ''),
              quantity: String(s.quantity),
              unitPrice: String(s.unitPrice),
              dueOn: String(s.executionDueOn || ''),
              name: s.productNameSnapshot,
              orderLineId: s.orderLineId,
            }));
          }
          if (kind === 'order-reschedule')
            Object.assign(defaultValues, {
              segmentKey: source[0]?.segmentKey || '',
              newDueOn: source[0]?.executionDueOn || '',
            });
          if (kind === 'order-increase')
            Object.assign(defaultValues, {
              orderLineId: source[0]?.orderLineId || '',
              unitPrice: String(source[0]?.unitPrice ?? '0'),
              dueOn: todayMinsk(),
            });
        }
        if (kind === 'return-create' && id) {
          const deliveries = (await allRows('/fulfillment/deliveries')).filter(
            (x) => x.orderId === id && x.state === 'CONFIRMED',
          );
          const ids = new Set(deliveries.map((x) => x.id));
          const acceptances = (await allRows('/fulfillment/acceptances')).filter(
            (x) => ids.has(x.deliveryId) && x.state === 'CONFIRMED',
          );
          const superseded = new Set(
            acceptances.map((a) => String(a.supersedesId || '')).filter(Boolean),
          );
          const current = acceptances.filter((a) => !superseded.has(String(a.id)));
          const parts = (
            await Promise.all(
              current.map((x) => api.get<JsonRecord[]>(`/fulfillment/acceptances/${x.id}/lines`)),
            )
          ).flat();
          const past = (await allRows('/fulfillment/returns')).filter(
            (r) => r.orderId === id && ['CONFIRMED', 'PENDING'].includes(String(r.state)),
          );
          const returned = (
            await Promise.all(
              past.map((r) => api.get<JsonRecord[]>(`/fulfillment/returns/${r.id}/lines`)),
            )
          ).flat();
          const used = new Map<string, number>();
          for (const r of returned)
            used.set(
              String(r.acceptanceLineId),
              (used.get(String(r.acceptanceLineId)) || 0) + Number(r.quantity || 0),
            );
          vRows = parts
            .filter((x) => Number(x.acceptedQty) > (used.get(String(x.id)) || 0))
            .map((x) => ({
              acceptanceLineId: x.id,
              acceptedQty: x.acceptedQty,
              availableQty: Math.max(0, Number(x.acceptedQty) - (used.get(String(x.id)) || 0)),
              quantity: '0',
              defectDescription: '',
              deliveryLineId: x.deliveryLineId,
            }));
          if (!vRows.length)
            throw new Error('Для возврата нужны подтверждённые приёмки с принятым товаром.');
        }
        if (kind === 'delivery-correction' && id) {
          const [doc, parts] = await Promise.all([
            api.get<JsonRecord>(`/fulfillment/deliveries/${id}`),
            api.get<JsonRecord[]>(`/fulfillment/deliveries/${id}/lines`),
          ]);
          const versions = await api.get<JsonRecord[]>(`/orders/${doc.orderId}/versions`);
          const approved = [...versions]
            .filter((v) => v.state === 'APPROVED')
            .sort((a, b) => Number(b.versionNo) - Number(a.versionNo))[0];
          if (!approved)
            throw new Error(
              'Невозможно исправить поступление без действующей согласованной версии заказа.',
            );
          const approvedParts = await api.get<JsonRecord[]>(
            `/orders/versions/${approved.id}/lines`,
          );
          const byId = new Map(approvedParts.map((part) => [String(part.id), part]));
          const allVersionLines = (
            await Promise.all(
              versions.map((version) =>
                api.get<JsonRecord[]>(`/orders/versions/${version.id}/lines`),
              ),
            )
          ).flat();
          const sourceById = new Map(allVersionLines.map((part) => [String(part.id), part]));
          vRows = parts.map((p) => {
            const original = sourceById.get(String(p.orderVersionLineId));
            const current =
              byId.get(String(p.orderVersionLineId)) ||
              approvedParts.find((x) => x.segmentKey === original?.segmentKey);
            if (!current)
              throw new Error(
                'Не удалось сопоставить старую строку с актуальным обязательством; исправление требует проверки заказа.',
              );
            return { approvedSegmentId: current.id, quantity: String(p.quantity) };
          });
          defaultValues.arrivedOn = doc.arrivedOn;
        }
        if (kind === 'acceptance-correction' && id) {
          const [doc, parts] = await Promise.all([
            api.get<JsonRecord>(`/fulfillment/acceptances/${id}`),
            api.get<JsonRecord[]>(`/fulfillment/acceptances/${id}/lines`),
          ]);
          defaultValues.inspectedOn = doc.inspectedOn;
          vRows = parts.map((p) => ({
            deliveryLineId: p.deliveryLineId,
            accepted: String(p.acceptedQty),
            qualityRejected: String(p.qualityRejectedQty),
            otherRejected: String(p.otherRejectedQty),
            reason: p.otherReason || '',
          }));
        }
        if (alive) {
          setValue(defaultValues);
          setLines(vRows);
          setSuppliers(supplierList);
          setCategories(cats);
          setOrders(orderList);
          setSourceLines(source);
          setOffers(offs);
        }
      } catch (e) {
        if (alive) setError(e);
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [kind, id, allowed, missingId]);
  // When creating a claim, resolve its target by actual order/version data.
  useEffect(() => {
    if (kind !== 'claim-create' || !value.orderId) return;
    let live = true;
    (async () => {
      try {
        const vers = await api.get<JsonRecord[]>(`/orders/${value.orderId}/versions`);
        const latest = [...vers]
          .filter((v) => v.state === 'APPROVED')
          .sort((a, b) => Number(b.versionNo) - Number(a.versionNo))[0];
        const part = latest
          ? await api.get<JsonRecord[]>(`/orders/versions/${latest.id}/lines`)
          : [];
        if (live) {
          setSourceLines(part);
          setValue((v) => ({ ...v, orderLineId: '', deliveryLineId: '' }));
        }
      } catch (e) {
        if (live) setError(e);
      }
    })();
    return () => {
      live = false;
    };
  }, [kind, value.orderId]);
  // Optional delivery-line reference for a claim; only lines belonging to its order line are offered.
  useEffect(() => {
    if (kind !== 'claim-create' || !value.orderLineId || !value.orderId) return;
    let live = true;
    (async () => {
      try {
        const docs = (await allRows('/fulfillment/deliveries')).filter(
          (d) => d.orderId === value.orderId,
        );
        const lines = (
          await Promise.all(
            docs.map((d) => api.get<JsonRecord[]>(`/fulfillment/deliveries/${d.id}/lines`)),
          )
        ).flat();
        const keys = sourceLines
          .filter((s) => s.orderLineId === value.orderLineId)
          .map((s) => s.id);
        if (live) setDeliveryLines(lines.filter((l) => keys.includes(l.orderVersionLineId)));
      } catch (e) {
        if (live) setError(e);
      }
    })();
    return () => {
      live = false;
    };
  }, [kind, value.orderLineId, value.orderId, sourceLines]);
  const rows: FieldSpec[] =
    kind === 'password-change'
      ? [
          { key: 'currentPassword', name: 'Текущий пароль', kind: 'password', required: true },
          { key: 'newPassword', name: 'Новый пароль', kind: 'password', required: true },
        ]
      : kind === 'user-reset'
        ? [{ key: 'newPassword', name: 'Новый пароль', kind: 'password', required: true }]
        : kind === 'user-add-role'
          ? [
              {
                key: 'role',
                name: 'Новая роль',
                required: true,
                kind: 'select',
                options: options([
                  ['PROCUREMENT_SPECIALIST', 'Специалист'],
                  ['PROCUREMENT_MANAGER', 'Руководитель'],
                  ['ADMINISTRATOR', 'Администратор'],
                ]),
              },
            ]
          : kind === 'contract-revision'
            ? [
                { key: 'number', name: 'Номер договора', required: true },
                { key: 'signedOn', name: 'Дата подписания', kind: 'date' },
                { key: 'validFrom', name: 'Действует с', kind: 'date', required: true },
                { key: 'validTo', name: 'Действует по', kind: 'date', required: true },
                { key: 'terms', name: 'Условия', kind: 'textarea', required: true },
                { key: 'reason', name: 'Основание изменения', kind: 'textarea', required: true },
              ]
            : ['order-draft', 'order-revise'].includes(kind)
              ? kind === 'order-revise'
                ? [{ key: 'reason', name: 'Причина переработки', required: true, kind: 'textarea' }]
                : []
              : kind === 'order-increase'
                ? [
                    {
                      key: 'orderLineId',
                      name: 'Строка заказа',
                      kind: 'select',
                      required: true,
                      options: options(
                        sourceLines
                          .filter(
                            (r, i, a) => a.findIndex((x) => x.orderLineId === r.orderLineId) === i,
                          )
                          .map((s) => [
                            String(s.orderLineId),
                            `${s.productNameSnapshot} · ${s.orderLineId}`,
                          ]),
                      ),
                    },
                    {
                      key: 'additional',
                      name: 'Дополнительное количество',
                      kind: 'number',
                      required: true,
                    },
                    {
                      key: 'unitPrice',
                      name: 'Цена за единицу, BYN',
                      kind: 'number',
                      required: true,
                    },
                    { key: 'dueOn', name: 'Срок поставки', kind: 'date', required: true },
                    {
                      key: 'reason',
                      name: 'Основание увеличения',
                      kind: 'textarea',
                      required: true,
                    },
                  ]
                : kind === 'order-reschedule'
                  ? [
                      {
                        key: 'segmentKey',
                        name: 'Часть обязательства',
                        kind: 'select',
                        required: true,
                        options: options(
                          sourceLines.map((s) => [
                            String(s.segmentKey),
                            `${s.productNameSnapshot} · ${s.quantity} · ${s.executionDueOn}`,
                          ]),
                        ),
                      },
                      { key: 'newDueOn', name: 'Новый срок', kind: 'date', required: true },
                      {
                        key: 'reason',
                        name: 'Основание переноса',
                        kind: 'textarea',
                        required: true,
                      },
                    ]
                  : kind === 'return-create'
                    ? [
                        { key: 'returnedOn', name: 'Дата возврата', kind: 'date', required: true },
                        {
                          key: 'reason',
                          name: 'Причина возврата',
                          kind: 'textarea',
                          required: true,
                        },
                      ]
                    : kind === 'delivery-correction'
                      ? [
                          {
                            key: 'arrivedOn',
                            name: 'Дата фактического поступления',
                            kind: 'date',
                            required: true,
                          },
                          {
                            key: 'reason',
                            name: 'Причина исправления',
                            kind: 'textarea',
                            required: true,
                          },
                        ]
                      : kind === 'acceptance-correction'
                        ? [
                            {
                              key: 'inspectedOn',
                              name: 'Дата проверки',
                              kind: 'date',
                              required: true,
                            },
                            {
                              key: 'reason',
                              name: 'Причина исправления',
                              kind: 'textarea',
                              required: true,
                            },
                          ]
                        : kind === 'claim-send'
                          ? [
                              {
                                key: 'channel',
                                name: 'Канал отправки',
                                kind: 'select',
                                required: true,
                                options: options(chan),
                              },
                            ]
                          : kind === 'claim-resolve'
                            ? [
                                {
                                  key: 'result',
                                  name: 'Результат',
                                  kind: 'select',
                                  required: true,
                                  options: options(
                                    canDecide(value.claimAuthorId, current?.user.id, manager)
                                      ? [
                                          ['FULL', 'Удовлетворена полностью'],
                                          ['PARTIAL', 'Частично'],
                                          ['NONE', 'Не удовлетворена'],
                                        ]
                                      : [['FULL', 'Удовлетворена полностью']],
                                  ),
                                },
                                {
                                  key: 'remedy',
                                  name: 'Способ урегулирования',
                                  kind: 'select',
                                  required: true,
                                  options: options([
                                    ['REPLACEMENT', 'Замена'],
                                    ['ADDITIONAL_DELIVERY', 'Допоставка'],
                                    ['REFUND', 'Возмещение'],
                                    ['OTHER', 'Другое'],
                                  ]),
                                },
                                {
                                  key: 'description',
                                  name: 'Описание результата',
                                  kind: 'textarea',
                                  required: true,
                                },
                              ]
                            : kind === 'claim-create'
                              ? [
                                  { key: 'number', name: 'Номер претензии', required: true },
                                  {
                                    key: 'supplierId',
                                    name: 'Поставщик',
                                    kind: 'select',
                                    required: true,
                                    options: opt(suppliers),
                                  },
                                  {
                                    key: 'orderId',
                                    name: 'Заказ поставщика',
                                    kind: 'select',
                                    required: true,
                                    options: options(
                                      orders
                                        .filter((o) => o.supplierId === value.supplierId)
                                        .map((o) => [String(o.id), String(o.number)]),
                                    ),
                                  },
                                  {
                                    key: 'orderLineId',
                                    name: 'Строка заказа',
                                    kind: 'select',
                                    required: true,
                                    options: options(
                                      sourceLines
                                        .filter(
                                          (x, i, a) =>
                                            a.findIndex((v) => v.orderLineId === x.orderLineId) ===
                                            i,
                                        )
                                        .map((x) => [
                                          String(x.orderLineId),
                                          `${x.productNameSnapshot} · ${x.orderLineId}`,
                                        ]),
                                    ),
                                  },
                                  {
                                    key: 'deliveryLineId',
                                    name: 'Строка поставки (при наличии)',
                                    kind: 'select',
                                    options: options(
                                      deliveryLines.map((l) => [
                                        String(l.id),
                                        `${l.id} · ${l.quantity}`,
                                      ]),
                                    ),
                                  },
                                  {
                                    key: 'type',
                                    name: 'Нарушение',
                                    kind: 'select',
                                    required: true,
                                    options: options([
                                      ['LATE', 'Просрочка'],
                                      ['SHORTAGE', 'Недопоставка'],
                                      ['QUALITY', 'Качество'],
                                      ['OTHER', 'Иное'],
                                    ]),
                                  },
                                  {
                                    key: 'description',
                                    name: 'Описание нарушения',
                                    kind: 'textarea',
                                    required: true,
                                  },
                                  {
                                    key: 'requirements',
                                    name: 'Требования к поставщику',
                                    kind: 'textarea',
                                    required: true,
                                  },
                                  { key: 'responseDueOn', name: 'Срок ответа', kind: 'date' },
                                ]
                              : kind === 'assessment-create'
                                ? [
                                    {
                                      key: 'supplierId',
                                      name: 'Поставщик',
                                      kind: 'select',
                                      required: true,
                                      options: opt(suppliers),
                                    },
                                    {
                                      key: 'periodStart',
                                      name: 'Период с',
                                      kind: 'date',
                                      required: true,
                                    },
                                    {
                                      key: 'periodEnd',
                                      name: 'Период по',
                                      kind: 'date',
                                      required: true,
                                    },
                                    {
                                      key: 'applicableFrom',
                                      name: 'Применять с',
                                      kind: 'date',
                                      required: true,
                                    },
                                    {
                                      key: 'applicableTo',
                                      name: 'Применять по',
                                      kind: 'date',
                                      required: true,
                                    },
                                    {
                                      key: 'score',
                                      name: 'Экспертный балл',
                                      kind: 'select',
                                      required: true,
                                      options: options(
                                        ['0', '25', '50', '75', '100'].map((v) => [v, v]),
                                      ),
                                    },
                                    {
                                      key: 'justification',
                                      name: 'Обоснование',
                                      kind: 'textarea',
                                      required: true,
                                    },
                                  ]
                                : kind === 'rating-calculate'
                                  ? [
                                      {
                                        key: 'supplierId',
                                        name: 'Поставщик',
                                        kind: 'select',
                                        required: true,
                                        options: opt(suppliers),
                                      },
                                      {
                                        key: 'scope',
                                        name: 'Охват',
                                        kind: 'select',
                                        required: true,
                                        options: options([
                                          ['OVERALL', 'Общий'],
                                          ['CATEGORY', 'По категории товаров'],
                                        ]),
                                      },
                                      ...(value.scope === 'CATEGORY'
                                        ? [
                                            {
                                              key: 'categoryId',
                                              name: 'Категория товаров',
                                              kind: 'select' as const,
                                              required: true,
                                              options: opt(categories),
                                            },
                                          ]
                                        : []),
                                    ]
                                  : [];
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      if (
        kind === 'claim-resolve' &&
        value.result !== 'FULL' &&
        !canDecide(value.claimAuthorId, current?.user.id, manager)
      )
        throw new Error('Закрытие с неполным удовлетворением требует независимого руководителя.');
      if (
        kind === 'return-create' &&
        lines.some((l) => Number(l.quantity) < 0 || Number(l.quantity) > Number(l.availableQty))
      )
        throw new Error(
          'Количество возврата должно быть в пределах доступного принятого количества.',
        );
      if (
        kind === 'acceptance-correction' &&
        lines.some((l) => Number(l.otherRejected) > 0 && !String(l.reason || '').trim())
      )
        throw new Error('Для прочих отклонений необходимо указать причину.');
      const { method, endpoint, payload } = buildOperationRequest(kind, id, value, lines);
      const output = await api.request<JsonRecord | undefined>(method, endpoint, payload);
      if (kind === 'password-change') {
        await logout().catch(() => undefined);
        go('/login');
        return;
      }
      if (kind === 'claim-create') go(output?.id ? `/claims/${output.id}` : '/claims');
      else if (kind === 'assessment-create')
        go(output?.id ? `/assessments/${output.id}` : '/assessments');
      else if (kind === 'rating-calculate') go(output?.id ? `/ratings/${output.id}` : '/ratings');
      else if (kind === 'return-create') go(output?.id ? `/returns/${output.id}` : '/returns');
      else if (['delivery-correction', 'acceptance-correction'].includes(kind))
        go(output?.id ? `/corrections/${output.id}` : '/corrections');
      else go(origin);
    } catch (e) {
      setError(e);
    } finally {
      setBusy(false);
    }
  };
  if (!allowed) return <ErrorBanner error="Недостаточно прав для выполнения операции." />;
  if (missingId)
    return <ErrorBanner error="Не указан документ, над которым необходимо выполнить операцию." />;
  if (!operationTitles[kind]) return <ErrorBanner error="Операция не найдена." />;
  return (
    <>
      <button className="text-back" onClick={() => go(origin)}>
        ← Назад
      </button>
      <PageHeading
        title={operationTitles[kind]}
        subtitle="Данные проверяются сервером; изменения фиксируются в истории документов"
      />
      {loading ? (
        <Loading />
      ) : (
        <section className="card form-card">
          <form className="form-stack" onSubmit={submit}>
            <div className="form-grid">
              {rows.map((f) => (
                <Field key={f.key} label={f.name}>
                  {f.kind === 'textarea' ? (
                    <textarea
                      className="input"
                      rows={3}
                      required={f.required}
                      value={String(value[f.key] ?? '')}
                      onChange={(e) => update(f.key, e.target.value)}
                    />
                  ) : f.kind === 'select' ? (
                    <SelectInput
                      name={f.key}
                      required={f.required}
                      items={f.options || []}
                      value={String(value[f.key] ?? '')}
                      onChange={(s) => {
                        update(f.key, s);
                        if (f.key === 'supplierId')
                          setValue((o) => ({
                            ...o,
                            supplierId: s,
                            orderId: '',
                            orderLineId: '',
                            deliveryLineId: '',
                          }));
                      }}
                    />
                  ) : (
                    <TextInput
                      name={f.key}
                      required={f.required}
                      type={
                        f.kind === 'password'
                          ? 'password'
                          : f.kind === 'date'
                            ? 'date'
                            : f.kind === 'number'
                              ? 'number'
                              : 'text'
                      }
                      value={String(value[f.key] ?? '')}
                      onChange={(s) => update(f.key, s)}
                    />
                  )}
                </Field>
              ))}
            </div>
            {['order-draft', 'order-revise'].includes(kind) && (
              <>
                <h2>Строки заказа</h2>
                <p className="muted note">
                  В случае переработки отклонённой версии порядок строк и номенклатура должны
                  оставаться прежними.
                </p>
                {lines.map((r, i) => (
                  <div key={i} className="order-line">
                    <strong>
                      Строка {i + 1}: {String(r.name || '')}
                    </strong>
                    <div className="form-grid">
                      <Field label="Товар (ассортимент поставщика)">
                        {kind === 'order-revise' ? (
                          <TextInput
                            name={'product' + i}
                            value={String(r.name || r.productId || '')}
                            disabled
                          />
                        ) : (
                          <SelectInput
                            name={'product' + i}
                            required
                            items={offers
                              .filter((x) => x.active === true)
                              .map((x) => ({
                                value: String(x.productId),
                                label: `${String(x.productName)} · ${String(x.productId).slice(0, 8)}`,
                              }))}
                            value={String(r.productId || '')}
                            onChange={(s) => changeLine(i, 'productId', s)}
                          />
                        )}
                      </Field>
                      {(['quantity', 'unitPrice', 'dueOn'] as const).map((k) => (
                        <Field
                          key={k}
                          label={
                            { quantity: 'Количество', unitPrice: 'Цена, BYN', dueOn: 'Срок' }[k]
                          }
                        >
                          <TextInput
                            name={`${k}${i}`}
                            required
                            type={k === 'dueOn' ? 'date' : 'number'}
                            step={k === 'dueOn' ? undefined : 'any'}
                            value={String(r[k] ?? '')}
                            onChange={(s) => changeLine(i, k, s)}
                          />
                        </Field>
                      ))}
                    </div>
                  </div>
                ))}
                {kind === 'order-draft' && (
                  <SecondaryButton onClick={() => setLines((rs) => [...rs, emptyOrder()])}>
                    Добавить строку
                  </SecondaryButton>
                )}
                {kind === 'order-draft' && lines.length > 1 && (
                  <SecondaryButton onClick={() => setLines((rs) => rs.slice(0, -1))}>
                    Удалить последнюю строку
                  </SecondaryButton>
                )}
              </>
            )}
            {['return-create', 'delivery-correction', 'acceptance-correction'].includes(kind) && (
              <>
                <h2>Строки документа</h2>
                {lines.map((r, i) => (
                  <div key={i} className="order-line">
                    <strong>
                      {kind === 'return-create'
                        ? `Строка приёмки · принято ${r.acceptedQty} · доступно ${r.availableQty}`
                        : kind === 'delivery-correction'
                          ? `Часть обязательства ${r.approvedSegmentId}`
                          : `Строка поступления ${r.deliveryLineId}`}
                    </strong>
                    <div className="form-grid">
                      {(kind === 'return-create'
                        ? (['quantity', 'defectDescription'] as const)
                        : kind === 'delivery-correction'
                          ? (['quantity'] as const)
                          : (['accepted', 'qualityRejected', 'otherRejected', 'reason'] as const)
                      ).map((k) => (
                        <Field
                          key={k}
                          label={
                            {
                              quantity: 'Количество',
                              defectDescription: 'Описание дефекта',
                              accepted: 'Принято',
                              qualityRejected: 'Брак',
                              otherRejected: 'Прочие отклонения',
                              reason: 'Причина прочих отклонений',
                            }[k]
                          }
                        >
                          <TextInput
                            name={`${k}${i}`}
                            type={k === 'defectDescription' || k === 'reason' ? 'text' : 'number'}
                            step={k === 'defectDescription' || k === 'reason' ? undefined : 'any'}
                            value={String(r[k] ?? '')}
                            onChange={(s) => changeLine(i, k, s)}
                          />
                        </Field>
                      ))}
                    </div>
                  </div>
                ))}
              </>
            )}
            <ErrorBanner error={error} />
            <div className="form-footer">
              <SecondaryButton onClick={() => go(origin)}>Отмена</SecondaryButton>
              <PrimaryButton type="submit" busy={busy}>
                Сохранить
              </PrimaryButton>
            </div>
          </form>
        </section>
      )}
    </>
  );
}
