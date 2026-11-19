import { useEffect, useState } from 'react';
import type { ReactNode, FormEvent } from 'react';
import { api } from '../core/api';
import { formatDate, formatDecimal, label, shortId } from '../core/navigation';
import type { Column } from '../core/resources';
import type { JsonRecord } from '../core/types';

export function Icon({ name, size = 20 }: { name: string; size?: number }) {
  const paths: Record<string, ReactNode> = {
    grid: (
      <>
        <rect x="3" y="3" width="7" height="7" rx="1" />
        <rect x="14" y="3" width="7" height="7" rx="1" />
        <rect x="3" y="14" width="7" height="7" rx="1" />
        <rect x="14" y="14" width="7" height="7" rx="1" />
      </>
    ),
    building: (
      <>
        <rect x="4" y="3" width="16" height="18" rx="2" />
        <path d="M9 21v-4h6v4M8 7h2m4 0h2M8 11h2m4 0h2" />
      </>
    ),
    package: (
      <>
        <path d="m12 2 9 5v10l-9 5-9-5V7l9-5ZM3 7l9 5 9-5M12 12v10" />
      </>
    ),
    file: (
      <>
        <path d="M5 3h10l4 4v14H5V3Zm10 0v5h4M8 12h8M8 16h8" />
      </>
    ),
    basket: (
      <>
        <path d="M3 9h18l-2 12H5L3 9Zm4 0 5-6 5 6M9 13v4m6-4v4" />
      </>
    ),
    truck: (
      <>
        <path d="M2 5h12v12H2zM14 9h4l4 4v4h-8M5 17a2 2 0 1 0 4 0 2 2 0 0 0-4 0m11 0a2 2 0 1 0 4 0 2 2 0 0 0-4 0" />
      </>
    ),
    check: (
      <>
        <path d="m5 12 4 4L19 6" />
      </>
    ),
    repeat: (
      <>
        <path d="M17 2l4 4-4 4M3 11V8a2 2 0 0 1 2-2h16M7 22l-4-4 4-4m14-1v3a2 2 0 0 1-2 2H3" />
      </>
    ),
    message: (
      <>
        <path d="M4 4h16v12H9l-5 4V4Z" />
      </>
    ),
    alert: (
      <>
        <path d="m12 3 10 18H2L12 3Zm0 6v5m0 3v1" />
      </>
    ),
    chart: (
      <>
        <path d="M3 21V3m0 18h18M7 17v-5m5 5V7m5 10V4" />
      </>
    ),
    users: (
      <>
        <circle cx="9" cy="8" r="3" />
        <path d="M3 20v-2a6 6 0 0 1 12 0v2M17 5a3 3 0 0 1 0 6m1 3a5 5 0 0 1 3 5" />
      </>
    ),
    shield: (
      <>
        <path d="M12 2 20 5v7c0 5-3 8-8 10-5-2-8-5-8-10V5l8-3Z" />
        <path d="m9 12 2 2 4-4" />
      </>
    ),
    tag: (
      <>
        <path d="M3 3h10l8 9-9 9-9-8V3Zm5 5h.01" />
      </>
    ),
    ruler: (
      <>
        <path d="M3 17 17 3l4 4L7 21l-4-4Zm7-7 3 3m0-6 3 3" />
      </>
    ),
    search: (
      <>
        <circle cx="11" cy="11" r="7" />
        <path d="m16 16 5 5" />
      </>
    ),
    arrow: (
      <>
        <path d="M5 12h14m-7-7 7 7-7 7" />
      </>
    ),
    logout: (
      <>
        <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4M16 17l5-5-5-5m5 5H9" />
      </>
    ),
    plus: (
      <>
        <path d="M12 5v14M5 12h14" />
      </>
    ),
    refresh: (
      <>
        <path d="M20 7v6h-6M4 17v-6h6M6 9a7 7 0 0 1 13-2M18 15a7 7 0 0 1-13 2" />
      </>
    ),
    menu: (
      <>
        <path d="M3 6h18M3 12h18M3 18h18" />
      </>
    ),
    close: (
      <>
        <path d="M5 5l14 14M19 5 5 19" />
      </>
    ),
    star: (
      <>
        <path d="m12 2 3 7 7 .5-5.5 5 2 7.5-6.5-4-6.5 4 2-7.5L2 9.5 9 9l3-7Z" />
      </>
    ),
    edit: (
      <>
        <path d="m5 17 11-11 3 3-11 11H5v-3ZM14 8l3 3" />
      </>
    ),
    download: (
      <>
        <path d="M12 3v13m-5-5 5 5 5-5M4 19h16" />
      </>
    ),
  };
  return (
    <svg
      width={size}
      height={size}
      viewBox="0 0 24 24"
      fill="none"
      stroke="currentColor"
      strokeWidth="1.7"
      strokeLinecap="round"
      strokeLinejoin="round"
      aria-hidden="true"
    >
      {paths[name] || paths.grid}
    </svg>
  );
}
export function Badge({ value }: { value: unknown }) {
  const normalized = String(value || '');
  const tone = [
    'ACTIVE',
    'APPROVED',
    'PUBLISHED',
    'REGISTERED',
    'COMPLETED',
    'CONFIRMED',
    'HIGH',
  ].includes(normalized)
    ? 'positive'
    : ['REJECTED', 'TERMINATED', 'BLOCKED', 'CANCELLED', 'LOW'].includes(normalized)
      ? 'negative'
      : [
            'PENDING',
            'PENDING_APPROVAL',
            'SUSPENDED',
            'ATTENTION',
            'PRELIMINARY',
            'IN_PROGRESS',
          ].includes(normalized)
        ? 'warning'
        : 'neutral';
  return <span className={`badge badge-${tone}`}>{label(value)}</span>;
}
export function PrimaryButton({
  children,
  type = 'button',
  busy = false,
  disabled = false,
  onClick,
}: {
  children: ReactNode;
  type?: 'button' | 'submit';
  busy?: boolean;
  disabled?: boolean;
  onClick?: () => void;
}) {
  return (
    <button className="btn btn-primary" type={type} disabled={busy || disabled} onClick={onClick}>
      {busy ? 'Сохранение...' : children}
    </button>
  );
}
export function SecondaryButton({
  children,
  onClick,
  disabled = false,
}: {
  children: ReactNode;
  onClick?: () => void;
  disabled?: boolean;
}) {
  return (
    <button className="btn btn-secondary" onClick={onClick} type="button" disabled={disabled}>
      {children}
    </button>
  );
}
export function PageHeading({
  title,
  subtitle,
  action,
}: {
  title: string;
  subtitle?: string;
  action?: ReactNode;
}) {
  return (
    <div className="page-heading">
      <div>
        <h1>{title}</h1>
        {subtitle && <p className="muted">{subtitle}</p>}
      </div>
      {action && <div className="page-actions">{action}</div>}
    </div>
  );
}
export function Empty({
  title = 'Нет данных',
  description = 'Записей пока нет.',
}: {
  title?: string;
  description?: string;
}) {
  return (
    <div className="empty">
      <div className="empty-icon">
        <Icon name="grid" size={25} />
      </div>
      <strong>{title}</strong>
      <p className="muted">{description}</p>
    </div>
  );
}
export function ErrorBanner({ error, onRetry }: { error: unknown; onRetry?: () => void }) {
  if (!error) return null;
  return (
    <div className="error-banner" role="alert">
      <strong>Не удалось выполнить запрос.</strong>
      <span>{error instanceof Error ? error.message : String(error)}</span>
      {onRetry && <SecondaryButton onClick={onRetry}>Повторить</SecondaryButton>}
    </div>
  );
}
export function Field({
  label: fieldLabel,
  children,
  hint,
}: {
  label: string;
  children: ReactNode;
  hint?: string;
}) {
  return (
    <label className="field">
      <span>{fieldLabel}</span>
      {children}
      {hint && <small className="muted">{hint}</small>}
    </label>
  );
}
export function TextInput({
  name,
  value,
  onChange,
  placeholder = '',
  required = false,
  type = 'text',
  min,
  step,
  disabled = false,
}: {
  name: string;
  value: string | number;
  onChange?: (value: string) => void;
  placeholder?: string;
  required?: boolean;
  type?: string;
  min?: number;
  step?: string;
  disabled?: boolean;
}) {
  return (
    <input
      className="input"
      name={name}
      value={value}
      onChange={(event) => onChange?.(event.target.value)}
      placeholder={placeholder}
      type={type}
      required={required}
      min={min}
      step={step}
      disabled={disabled}
    />
  );
}
export function SelectInput({
  name,
  value,
  onChange,
  items,
  placeholder = 'Выберите значение',
  required = false,
}: {
  name: string;
  value: string;
  onChange: (value: string) => void;
  items: { value: string; label: string }[];
  placeholder?: string;
  required?: boolean;
}) {
  return (
    <select
      className="input"
      name={name}
      value={value}
      onChange={(e) => onChange(e.target.value)}
      required={required}
    >
      <option value="">{placeholder}</option>
      {items.map((option) => (
        <option key={option.value} value={option.value}>
          {option.label}
        </option>
      ))}
    </select>
  );
}
export function DataTable({
  columns,
  rows,
  onRow,
  emptyText = 'Записей нет',
}: {
  columns: Column[];
  rows: JsonRecord[];
  onRow?: (row: JsonRecord) => void;
  emptyText?: string;
}) {
  if (!rows.length) return <Empty description={emptyText} />;
  return (
    <div className="table-scroll">
      <table>
        <thead>
          <tr>
            {columns.map((c) => (
              <th key={c.key}>{c.title}</th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr
              key={String(row.id ?? i)}
              tabIndex={onRow ? 0 : undefined}
              className={onRow ? 'clickable' : ''}
              onClick={() => onRow?.(row)}
              onKeyDown={(e) => {
                if (e.key === 'Enter') onRow?.(row);
              }}
            >
              {columns.map((col) => (
                <td key={col.key}>
                  {col.format === 'status' ? (
                    <Badge value={row[col.key]} />
                  ) : col.format === 'date' ? (
                    formatDate(row[col.key])
                  ) : col.format === 'decimal' ? (
                    formatDecimal(row[col.key])
                  ) : col.format === 'id' ? (
                    <code className="short-id" title={String(row[col.key] ?? '')}>
                      {shortId(row[col.key])}
                    </code>
                  ) : (
                    label(row[col.key])
                  )}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}
export function Pager({
  page,
  totalPages,
  totalElements,
  onChange,
}: {
  page: number;
  totalPages: number;
  totalElements: number;
  onChange: (page: number) => void;
}) {
  return (
    <div className="pager">
      <span className="muted">Всего записей: {totalElements}</span>
      <div className="row">
        <SecondaryButton disabled={page <= 0} onClick={() => onChange(page - 1)}>
          Назад
        </SecondaryButton>
        <span>
          {page + 1} / {Math.max(1, totalPages)}
        </span>
        <SecondaryButton disabled={page + 1 >= totalPages} onClick={() => onChange(page + 1)}>
          Вперёд
        </SecondaryButton>
      </div>
    </div>
  );
}
export function usePageData<T>(path: string, page: number, version: number = 0) {
  const [data, setData] = useState<{
    items: T[];
    totalElements: number;
    totalPages: number;
  } | null>(null);
  const [busy, setBusy] = useState(true);
  const [error, setError] = useState<unknown>(null);
  useEffect(() => {
    let active = true;
    setBusy(true);
    setError(null);
    api
      .page<T>(path, page, 20)
      .then((res) => {
        if (active) setData(res);
      })
      .catch((err) => {
        if (active) setError(err);
      })
      .finally(() => {
        if (active) setBusy(false);
      });
    return () => {
      active = false;
    };
  }, [path, page, version]);
  return { data, busy, error };
}
export function Loading() {
  return (
    <div className="loading" role="status" aria-label="Загрузка данных">
      <div className="spinner" />
      Загрузка данных...
    </div>
  );
}
export function ConfirmForm({
  title,
  description,
  onClose,
  onSubmit,
  commentLabel = 'Комментарий',
  requireComment = false,
}: {
  title: string;
  description?: string;
  onClose: () => void;
  onSubmit: (comment: string) => Promise<void>;
  commentLabel?: string;
  requireComment?: boolean;
}) {
  const [value, setValue] = useState('');
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState<unknown>(null);
  const handle = async (e: FormEvent) => {
    e.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await onSubmit(value);
      onClose();
    } catch (err) {
      setError(err);
    } finally {
      setBusy(false);
    }
  };
  return (
    <div
      className="modal-backdrop"
      role="presentation"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <section className="modal" role="dialog" aria-modal="true" aria-label={title}>
        <header className="modal-title">
          <h2>{title}</h2>
          <button className="icon-button" onClick={onClose} aria-label="Закрыть">
            <Icon name="close" />
          </button>
        </header>
        {description && <p className="muted">{description}</p>}
        <form onSubmit={handle}>
          <Field label={commentLabel}>
            <textarea
              className="input"
              rows={3}
              value={value}
              onChange={(e) => setValue(e.target.value)}
              required={requireComment}
            />
          </Field>
          <ErrorBanner error={error} />
          <div className="modal-footer">
            <SecondaryButton onClick={onClose}>Отмена</SecondaryButton>
            <PrimaryButton type="submit" busy={busy}>
              Подтвердить
            </PrimaryButton>
          </div>
        </form>
      </section>
    </div>
  );
}
