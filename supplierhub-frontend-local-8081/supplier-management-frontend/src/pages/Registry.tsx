import { useEffect, useMemo, useState } from 'react';
import { api } from '../core/api';
import { findResource } from '../core/resources';
import { useAuth } from '../core/auth';
import type { JsonRecord } from '../core/types';
import {
  ConfirmForm,
  DataTable,
  ErrorBanner,
  Icon,
  Loading,
  PageHeading,
  Pager,
  PrimaryButton,
  SecondaryButton,
  usePageData,
} from '../ui/Shared';
import { label } from '../core/navigation';
import { canDecide } from '../core/data';

export function Registry({ id, go }: { id: string; go: (path: string) => void }) {
  const resource = findResource(id);
  const { can, current } = useAuth();
  const [page, setPage] = useState(0);
  const [version, setVersion] = useState(0);
  const [search, setSearch] = useState('');
  const [review, setReview] = useState<JsonRecord | null>(null);
  const [reviewApprove, setReviewApprove] = useState<boolean | null>(null);
  useEffect(() => {
    setPage(0);
    setSearch('');
  }, [id]);
  const supportsSearch = id === 'suppliers';
  const path = useMemo(
    () =>
      `${resource?.endpoint || ''}${supportsSearch && search.trim() ? `?search=${encodeURIComponent(search.trim())}` : ''}`,
    [resource?.endpoint, search, supportsSearch],
  );
  const { data, busy, error } = usePageData<JsonRecord>(path, page, version);
  if (!resource || !can(...resource.access))
    return <ErrorBanner error="Раздел недоступен для вашей роли." />;
  const visible = supportsSearch
    ? data?.items || []
    : (data?.items || []).filter(
        (row) =>
          search.trim() === '' ||
          Object.values(row).some(
            (value) =>
              typeof value === 'string' &&
              value.toLocaleLowerCase('ru').includes(search.toLocaleLowerCase('ru')),
          ),
      );
  return (
    <>
      <PageHeading
        title={resource.title}
        subtitle="Реестр и операции с данными"
        action={
          <div className="row wrap">
            <SecondaryButton onClick={() => setVersion((v) => v + 1)}>
              <Icon name="refresh" size={16} /> Обновить
            </SecondaryButton>
            {resource.canCreate &&
              (!['categories', 'units'].includes(id) || can('PROCUREMENT_MANAGER')) && (
                <PrimaryButton onClick={() => go(`/${id}/new`)}>
                  <Icon name="plus" size={16} /> Добавить
                </PrimaryButton>
              )}
            {id === 'claims' && (
              <PrimaryButton onClick={() => go('/operation/claim-create')}>
                Создать претензию
              </PrimaryButton>
            )}
            {id === 'assessments' && (
              <PrimaryButton onClick={() => go('/operation/assessment-create')}>
                Новая оценка
              </PrimaryButton>
            )}
            {id === 'ratings' && can('PROCUREMENT_MANAGER') && (
              <PrimaryButton onClick={() => go('/operation/rating-calculate')}>
                Рассчитать рейтинг
              </PrimaryButton>
            )}
          </div>
        }
      />
      {['suppliers', 'status-requests'].includes(id) && (
        <div className="tab-row">
          {[
            { id: 'suppliers', name: 'Поставщики' },
            { id: 'status-requests', name: 'Заявки на изменение статуса' },
          ].map((tab) => (
            <button
              key={tab.id}
              className={`tab ${tab.id === id ? 'selected' : ''}`}
              onClick={() => go(`/${tab.id}`)}
            >
              {tab.name}
            </button>
          ))}
        </div>
      )}
      {['products', 'categories', 'units'].includes(id) && (
        <div className="tab-row">
          {[
            { id: 'products', name: 'Товары' },
            { id: 'categories', name: 'Категории' },
            { id: 'units', name: 'Единицы измерения' },
          ].map((tab) => (
            <button
              key={tab.id}
              className={`tab ${tab.id === id ? 'selected' : ''}`}
              onClick={() => go(`/${tab.id}`)}
            >
              {tab.name}
            </button>
          ))}
        </div>
      )}
      {['deliveries', 'acceptances', 'returns', 'corrections'].includes(id) && (
        <div className="tab-row">
          {[
            { id: 'deliveries', name: 'Поступления' },
            { id: 'acceptances', name: 'Приёмки' },
            { id: 'returns', name: 'Возвраты' },
            { id: 'corrections', name: 'Исправления' },
          ].map((tab) => (
            <button
              key={tab.id}
              className={`tab ${tab.id === id ? 'selected' : ''}`}
              onClick={() => go(`/${tab.id}`)}
            >
              {tab.name}
            </button>
          ))}
        </div>
      )}
      <section className="card registry-card">
        <div className="toolbar">
          <div className="search-field">
            <Icon name="search" size={18} />
            <input
              aria-label="Поиск"
              value={search}
              onChange={(e) => {
                setSearch(e.target.value);
                setPage(0);
              }}
              placeholder={supportsSearch ? 'Поиск поставщика' : 'Поиск на текущей странице'}
            />
          </div>
          <span className="muted toolbar-hint">
            {supportsSearch ? 'Поиск на сервере' : 'Локальный поиск только по загруженной странице'}
          </span>
        </div>
        <ErrorBanner error={error} onRetry={() => setVersion((v) => v + 1)} />{' '}
        {busy ? (
          <Loading />
        ) : (
          !error && (
            <>
              <DataTable
                columns={resource.columns}
                rows={visible}
                onRow={
                  resource.detail
                    ? (row) => go(`/${id}/${row.id}`)
                    : id === 'status-requests' && can('PROCUREMENT_MANAGER')
                      ? (row) => {
                          if (
                            row.state === 'PENDING' &&
                            canDecide(
                              row.requestedById,
                              current?.user.id,
                              can('PROCUREMENT_MANAGER'),
                            )
                          )
                            setReview(row);
                        }
                      : undefined
                }
                emptyText={
                  search
                    ? 'По вашему запросу ничего не найдено'
                    : 'Создайте первую запись или обновите страницу.'
                }
              />
              <Pager
                page={page}
                totalPages={data?.totalPages || 0}
                totalElements={data?.totalElements || 0}
                onChange={setPage}
              />
            </>
          )
        )}
      </section>
      {['ratings', 'assessments'].includes(id) && (
        <div className="tab-row">
          {[
            { id: 'ratings', name: 'Рейтинги' },
            { id: 'assessments', name: 'Экспертные оценки' },
          ].map((tab) => (
            <button
              key={tab.id}
              className={`tab ${tab.id === id ? 'selected' : ''}`}
              onClick={() => go(`/${tab.id}`)}
            >
              {tab.name}
            </button>
          ))}
        </div>
      )}
      {id === 'ratings' && (
        <p className="muted note">
          Предварительные рейтинги и записи с недостаточными данными не считаются опубликованными
          оценками.
        </p>
      )}
      {id === 'status-requests' && can('PROCUREMENT_MANAGER') && (
        <p className="muted note">
          Для принятия решения откройте ожидающую заявку другого сотрудника.
        </p>
      )}
      {review && reviewApprove === null && (
        <div
          className="modal-backdrop"
          role="presentation"
          onMouseDown={(e) => {
            if (e.target === e.currentTarget) setReview(null);
          }}
        >
          <section className="modal" role="dialog" aria-modal="true" aria-label="Решение по заявке">
            <h2>Рассмотрение заявки</h2>
            <p className="muted">
              Запрошенный статус: {label(review.toStatus)}. Основание:{' '}
              {String(review.reason || '—')}
            </p>
            <div className="row wrap">
              <SecondaryButton onClick={() => setReview(null)}>Отмена</SecondaryButton>
              <SecondaryButton onClick={() => setReviewApprove(false)}>Отклонить</SecondaryButton>
              <PrimaryButton onClick={() => setReviewApprove(true)}>Согласовать</PrimaryButton>
            </div>
          </section>
        </div>
      )}
      {review && reviewApprove !== null && (
        <ConfirmForm
          title={reviewApprove ? 'Согласовать смену статуса' : 'Отклонить заявку'}
          requireComment={!reviewApprove}
          onClose={() => {
            setReview(null);
            setReviewApprove(null);
          }}
          onSubmit={async (comment) => {
            await api.post(`/suppliers/status-requests/${review.id}/decision`, {
              approve: reviewApprove,
              comment,
            });
            setVersion((v) => v + 1);
          }}
        />
      )}

      {id === 'interactions' && (
        <p className="muted note">
          Все даты и статусы поступают с сервера; демонстрационные записи не используются.
        </p>
      )}
    </>
  );
}
