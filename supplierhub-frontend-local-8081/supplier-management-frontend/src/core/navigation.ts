import { useEffect, useState } from 'react';
export function usePathname() {
  const [path, setPath] = useState(window.location.pathname);
  useEffect(() => {
    const handler = () => setPath(window.location.pathname);
    window.addEventListener('popstate', handler);
    return () => window.removeEventListener('popstate', handler);
  }, []);
  const go = (url: string) => {
    if (window.location.pathname !== url) window.history.pushState({}, '', url);
    setPath(url);
    window.scrollTo({ top: 0, behavior: 'instant' });
  };
  return { path, go };
}
export function shortId(value: unknown): string {
  return typeof value === 'string' ? value.slice(0, 8) : '—';
}
export const formatDate = (value: unknown) => {
  if (typeof value !== 'string' || !value) return '—';
  const date = new Date(value.length === 10 ? `${value}T12:00:00` : value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleDateString('ru-BY');
};
export const formatDecimal = (value: unknown, digits = 1) => {
  const n = Number(value);
  return value === undefined || value === null || value === '' || !Number.isFinite(n)
    ? '—'
    : n.toLocaleString('ru-BY', { maximumFractionDigits: digits });
};
const messages: Record<string, string> = {
  CANDIDATE: 'Кандидат',
  ACTIVE: 'Активный',
  SUSPENDED: 'Приостановлен',
  TERMINATED: 'Прекращён',
  DRAFT: 'Черновик',
  REGISTERED: 'Зарегистрирован',
  PENDING: 'На согласовании',
  PENDING_APPROVAL: 'На согласовании',
  APPROVED: 'Согласован',
  REJECTED: 'Отклонён',
  IN_PROGRESS: 'В работе',
  COMPLETED: 'Завершён',
  CANCELLED: 'Отменён',
  CONFIRMED: 'Подтверждён',
  BLOCKED: 'Заблокирован',
  SENT: 'Отправлена',
  IN_RESOLUTION: 'Урегулирование',
  CLOSED: 'Закрыта',
  PRELIMINARY: 'Предварительный',
  PUBLISHED: 'Опубликован',
  INSUFFICIENT_DATA: 'Недостаточно данных',
  HIGH: 'Высокая',
  ACCEPTABLE: 'Приемлемая',
  ATTENTION: 'Требует внимания',
  LOW: 'Низкая',
  OVERALL: 'Общий',
  CATEGORY: 'По категории',
  FULL: 'Полностью',
  PARTIAL: 'Частично',
  NONE: 'Не удовлетворена',
  PROCUREMENT_SPECIALIST: 'Специалист по закупкам',
  PROCUREMENT_MANAGER: 'Руководитель закупок',
  ADMINISTRATOR: 'Администратор',
  EMAIL: 'Электронная почта',
  PHONE: 'Телефон',
  MEETING: 'Встреча',
  MESSENGER: 'Мессенджер',
  OTHER: 'Другое',
  LATE: 'Нарушение срока',
  SHORTAGE: 'Недопоставка',
  QUALITY: 'Качество',
  INITIAL: 'Исходная',
  CHANGE: 'Изменение',
  BUYER: 'Покупатель',
  SUPPLIER: 'Поставщик',
  CORRECTION: 'Исправление',
};
export function label(value: unknown): string {
  if (typeof value === 'boolean') return value ? 'Да' : 'Нет';
  return value === undefined || value === null || value === ''
    ? '—'
    : messages[String(value)] || String(value);
}
