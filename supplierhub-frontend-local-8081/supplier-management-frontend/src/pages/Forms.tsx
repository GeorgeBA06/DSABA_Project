import { useEffect, useState } from 'react';
import type { FormEvent } from 'react';
import { api } from '../core/api';
import { findResource } from '../core/resources';
import { useAuth } from '../core/auth';
import type { JsonRecord } from '../core/types';
import { allRows, toOrderItems } from '../core/data';
import {
  ErrorBanner,
  Field,
  Icon,
  Loading,
  PageHeading,
  PrimaryButton,
  SecondaryButton,
  SelectInput,
  TextInput,
} from '../ui/Shared';

interface Option {
  value: string;
  label: string;
}
interface FieldSpec {
  key: string;
  label: string;
  required?: boolean;
  type?: string;
  options?: Option[];
  placeholder?: string;
}
const supplierFields: FieldSpec[] = [
  { key: 'name', label: 'Наименование', required: true },
  { key: 'contactName', label: 'Контактное лицо' },
  { key: 'email', label: 'Электронная почта', type: 'email' },
  { key: 'phone', label: 'Телефон' },
  { key: 'address', label: 'Адрес' },
  { key: 'registrationNumber', label: 'Регистрационный номер' },
];
function FormFields({
  fields,
  values,
  update,
}: {
  fields: FieldSpec[];
  values: JsonRecord;
  update: (key: string, value: string) => void;
}) {
  return (
    <div className="form-grid">
      {fields.map((spec) => (
        <Field key={spec.key} label={spec.label}>
          {spec.options ? (
            <SelectInput
              name={spec.key}
              value={String(values[spec.key] || '')}
              onChange={(value) => update(spec.key, value)}
              required={spec.required}
              items={spec.options}
            />
          ) : (
            <TextInput
              name={spec.key}
              value={String(values[spec.key] ?? '')}
              onChange={(value) => update(spec.key, value)}
              type={spec.type || 'text'}
              required={spec.required}
              placeholder={spec.placeholder}
            />
          )}
        </Field>
      ))}
    </div>
  );
}
const loadList = allRows;
export function CreateForm({
  id,
  editId,
  go,
}: {
  id: string;
  editId?: string;
  go: (path: string) => void;
}) {
  const resource = findResource(id);
  const { can } = useAuth();
  const [values, setValues] = useState<JsonRecord>({});
  const [items, setItems] = useState<JsonRecord[]>([
    { productId: '', quantity: '1', unitPrice: '0', dueOn: '' },
  ]);
  const [categories, setCategories] = useState<JsonRecord[]>([]);
  const [units, setUnits] = useState<JsonRecord[]>([]);
  const [products, setProducts] = useState<JsonRecord[]>([]);
  const [suppliers, setSuppliers] = useState<JsonRecord[]>([]);
  const [contracts, setContracts] = useState<JsonRecord[]>([]);
  const [offers, setOffers] = useState<JsonRecord[]>([]);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => {
    let alive = true;
    setLoading(true);
    setValues({});
    (async () => {
      try {
        const needsCatalog = id === 'products' || id === 'orders';
        const needsSuppliers = ['contracts', 'orders', 'interactions'].includes(id);
        const [cats, unis, prods, sups, cons, edited] = await Promise.all([
          needsCatalog ? loadList('/catalog/categories') : Promise.resolve([]),
          needsCatalog ? loadList('/catalog/units') : Promise.resolve([]),
          Promise.resolve([]),
          needsSuppliers ? loadList('/suppliers') : Promise.resolve([]),
          id === 'orders' ? loadList('/contracts') : Promise.resolve([]),
          editId && id === 'suppliers'
            ? api.get<JsonRecord>(`/suppliers/${editId}`)
            : Promise.resolve({}),
        ]);
        if (alive) {
          setCategories(cats);
          setUnits(unis);
          setProducts(prods);
          setSuppliers(sups);
          setContracts(cons);
          setValues(edited);
        }
      } catch (err) {
        if (alive) setError(err);
      } finally {
        if (alive) setLoading(false);
      }
    })();
    return () => {
      alive = false;
    };
  }, [id, editId]);
  const change = (key: string, value: string) => {
    setValues((previous) => ({
      ...previous,
      [key]: value,
      ...(id === 'orders' && key === 'supplierId' ? { contractId: '' } : {}),
    }));
    if (id === 'orders' && key === 'supplierId') {
      setItems([{ productId: '', quantity: '1', unitPrice: '0', dueOn: '' }]);
    }
  };
  useEffect(() => {
    if (id !== 'orders' || !values.supplierId) {
      setOffers([]);
      return;
    }
    let active = true;
    api
      .get<JsonRecord[]>(`/suppliers/${values.supplierId}/products`)
      .then((res) => {
        if (active) setOffers(res);
      })
      .catch((e) => {
        if (active) {
          setOffers([]);
          setError(e);
        }
      });
    return () => {
      active = false;
    };
  }, [id, values.supplierId]);
  const supplierOptions = suppliers.map((s) => ({ value: String(s.id), label: String(s.name) }));
  const selectedSupplier = String(values.supplierId || '');
  const contractOptions = contracts
    .filter((c) => String(c.supplierId) === selectedSupplier && c.state === 'REGISTERED')
    .map((c) => ({ value: String(c.id), label: `Договор ${String(c.id).slice(0, 8)}` }));
  const productOptions = offers
    .filter((p) => p.active === true)
    .map((p) => ({ value: String(p.productId), label: String(p.productName) }));
  const simpleFields: FieldSpec[] =
    id === 'suppliers'
      ? supplierFields
      : id === 'categories' || id === 'units'
        ? [
            { key: 'code', label: 'Код', required: true },
            { key: 'name', label: 'Наименование', required: true },
          ]
        : id === 'products'
          ? [
              { key: 'sku', label: 'Артикул', required: true },
              { key: 'name', label: 'Наименование', required: true },
              {
                key: 'categoryId',
                label: 'Категория',
                required: true,
                options: categories.map((c) => ({ value: String(c.id), label: String(c.name) })),
              },
              {
                key: 'unitId',
                label: 'Единица измерения',
                required: true,
                options: units.map((u) => ({ value: String(u.id), label: String(u.name) })),
              },
            ]
          : id === 'contracts'
            ? [
                { key: 'supplierId', label: 'Поставщик', required: true, options: supplierOptions },
                { key: 'number', label: 'Номер договора', required: true },
                { key: 'signedOn', label: 'Дата подписания', type: 'date' },
                { key: 'validFrom', label: 'Дата начала', type: 'date', required: true },
                { key: 'validTo', label: 'Дата окончания', type: 'date', required: true },
              ]
            : id === 'orders'
              ? [
                  { key: 'number', label: 'Номер заказа', required: true },
                  {
                    key: 'supplierId',
                    label: 'Поставщик',
                    required: true,
                    options: supplierOptions,
                  },
                  {
                    key: 'contractId',
                    label: 'Зарегистрированный договор',
                    required: true,
                    options: contractOptions,
                  },
                ]
              : id === 'interactions'
                ? [
                    {
                      key: 'supplierId',
                      label: 'Поставщик',
                      required: true,
                      options: supplierOptions,
                    },
                    {
                      key: 'channel',
                      label: 'Канал',
                      required: true,
                      options: [
                        { value: 'EMAIL', label: 'Электронная почта' },
                        { value: 'PHONE', label: 'Телефон' },
                        { value: 'MEETING', label: 'Встреча' },
                        { value: 'MESSENGER', label: 'Мессенджер' },
                        { value: 'OTHER', label: 'Другое' },
                      ],
                    },
                    { key: 'subject', label: 'Тема', required: true },
                    { key: 'outcome', label: 'Результат' },
                  ]
                : id === 'users'
                  ? [
                      { key: 'login', label: 'Логин', required: true },
                      { key: 'displayName', label: 'Имя сотрудника', required: true },
                      {
                        key: 'password',
                        label: 'Первоначальный пароль',
                        required: true,
                        type: 'password',
                      },
                    ]
                  : [];
  const submit = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      let result: JsonRecord | undefined;
      if (id === 'suppliers') {
        const payload = Object.fromEntries(supplierFields.map((f) => [f.key, values[f.key] || '']));
        result = editId
          ? await api.put<JsonRecord>(`/suppliers/${editId}`, payload)
          : await api.post<JsonRecord>('/suppliers', payload);
      } else if (id === 'categories' || id === 'units')
        result = await api.post<JsonRecord>(`/catalog/${id}`, {
          code: values.code,
          name: values.name,
        });
      else if (id === 'products')
        result = await api.post<JsonRecord>('/catalog/products', {
          sku: values.sku,
          name: values.name,
          categoryId: values.categoryId,
          unitId: values.unitId,
        });
      else if (id === 'contracts')
        result = await api.post<JsonRecord>('/contracts', {
          supplierId: values.supplierId,
          terms: {
            number: values.number,
            signedOn: values.signedOn || null,
            validFrom: values.validFrom,
            validTo: values.validTo,
            terms: values.terms,
          },
        });
      else if (id === 'orders') {
        if (items.some((item) => !productOptions.some((p) => p.value === item.productId)))
          throw new Error('В заказе можно выбирать только предлагаемые данным поставщиком товары.');
        result = await api.post<JsonRecord>('/orders', {
          number: values.number,
          supplierId: values.supplierId,
          contractId: values.contractId,
          items: toOrderItems(items),
        });
      } else if (id === 'interactions')
        result = await api.post<JsonRecord>('/relationships/interactions', {
          supplierId: values.supplierId,
          channel: values.channel,
          subject: values.subject,
          content: values.content,
          outcome: values.outcome || null,
          occurredAt: new Date().toISOString(),
        });
      else if (id === 'users')
        result = await api.post<JsonRecord>('/admin/users', {
          login: values.login,
          displayName: values.displayName,
          password: values.password,
          roles: [values.role],
        });
      else throw new Error('Для этого раздела создание записей пока не предусмотрено.');
      go(result?.id && resource?.detail ? `/${id}/${result.id}` : `/${id}`);
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };
  const updateItem = (index: number, key: string, value: string) =>
    setItems((prev) => prev.map((item, i) => (i === index ? { ...item, [key]: value } : item)));
  if (
    !resource ||
    !resource.canCreate ||
    !can(...resource.access) ||
    (['categories', 'units'].includes(id) && !can('PROCUREMENT_MANAGER'))
  )
    return <ErrorBanner error="Создание недоступно" />;
  return (
    <>
      <button className="text-back" onClick={() => go(`/${id}`)}>
        ← Вернуться в реестр
      </button>
      <PageHeading title={editId ? 'Редактирование' : 'Новая запись'} subtitle={resource.title} />
      {loading ? (
        <Loading />
      ) : (
        <section className="card form-card">
          <form onSubmit={submit} className="form-stack">
            <h2>Основные сведения</h2>
            <FormFields fields={simpleFields} values={values} update={change} />
            {id === 'contracts' && (
              <Field label="Условия договора">
                <textarea
                  className="input"
                  rows={5}
                  required
                  value={String(values.terms || '')}
                  onChange={(e) => change('terms', e.target.value)}
                  placeholder="Основные условия сотрудничества"
                />
              </Field>
            )}
            {id === 'interactions' && (
              <Field label="Содержание взаимодействия">
                <textarea
                  className="input"
                  rows={5}
                  required
                  value={String(values.content || '')}
                  onChange={(e) => change('content', e.target.value)}
                  placeholder="Краткое описание общения с поставщиком"
                />
              </Field>
            )}
            {id === 'users' && (
              <Field label="Роль">
                <SelectInput
                  name="role"
                  required
                  value={String(values.role || '')}
                  onChange={(v) => change('role', v)}
                  items={[
                    { value: 'PROCUREMENT_SPECIALIST', label: 'Специалист по закупкам' },
                    { value: 'PROCUREMENT_MANAGER', label: 'Руководитель закупок' },
                    { value: 'ADMINISTRATOR', label: 'Администратор' },
                  ]}
                />
              </Field>
            )}
            {id === 'orders' && (
              <div className="order-lines">
                <div className="row space-between">
                  <h2>Товары в заказе</h2>
                  <SecondaryButton
                    onClick={() =>
                      setItems((prev) => [
                        ...prev,
                        { productId: '', quantity: '1', unitPrice: '0', dueOn: '' },
                      ])
                    }
                  >
                    <Icon name="plus" size={16} /> Добавить строку
                  </SecondaryButton>
                </div>
                {items.map((item, index) => (
                  <div className="order-line" key={index}>
                    <div className="row space-between">
                      <strong>Строка {index + 1}</strong>
                      {items.length > 1 && (
                        <button
                          type="button"
                          className="text-danger"
                          onClick={() => setItems((prev) => prev.filter((_, i) => i !== index))}
                        >
                          Удалить
                        </button>
                      )}
                    </div>
                    <div className="form-grid">
                      <Field label="Товар">
                        <SelectInput
                          required
                          name="productId"
                          value={String(item.productId || '')}
                          onChange={(v) => updateItem(index, 'productId', v)}
                          items={productOptions}
                        />
                      </Field>
                      <Field label="Количество">
                        <TextInput
                          name="quantity"
                          type="number"
                          min={0.0001}
                          step="any"
                          required
                          value={String(item.quantity)}
                          onChange={(v) => updateItem(index, 'quantity', v)}
                        />
                      </Field>
                      <Field label="Цена за единицу, BYN">
                        <TextInput
                          name="unitPrice"
                          type="number"
                          min={0}
                          step="0.01"
                          required
                          value={String(item.unitPrice)}
                          onChange={(v) => updateItem(index, 'unitPrice', v)}
                        />
                      </Field>
                      <Field label="Срок поставки">
                        <TextInput
                          name="dueOn"
                          type="date"
                          required
                          value={String(item.dueOn || '')}
                          onChange={(v) => updateItem(index, 'dueOn', v)}
                        />
                      </Field>
                    </div>
                  </div>
                ))}
              </div>
            )}
            <ErrorBanner error={error} />
            <div className="form-footer">
              <SecondaryButton onClick={() => go(`/${id}`)}>Отмена</SecondaryButton>
              <PrimaryButton type="submit" busy={busy}>
                {editId ? 'Сохранить изменения' : 'Создать'}
              </PrimaryButton>
            </div>
          </form>
        </section>
      )}
    </>
  );
}
