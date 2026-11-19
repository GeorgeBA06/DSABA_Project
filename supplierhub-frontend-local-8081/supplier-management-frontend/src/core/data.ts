import { api } from './api.ts';
import type { JsonRecord } from './types';
/** The backend provides offset pagination, not a server-side lookup endpoint for every selector.
 * Fetch all pages of the small course-project reference directories rather than silently
 * hiding records after the first 100. Failure is explicit if a directory is unreasonably large. */
export async function allRows(path: string, maxRows = 10000): Promise<JsonRecord[]> {
  const results: JsonRecord[] = [];
  for (let page = 0; page < Math.ceil(maxRows / 100); page++) {
    const chunk = await api.page<JsonRecord>(path, page, 100);
    results.push(...chunk.items);
    if (results.length > maxRows)
      throw new Error('Справочник слишком большой. Уточните выборку на сервере.');
    if (page + 1 >= chunk.totalPages || chunk.items.length === 0) return results;
  }
  throw new Error('Справочник превышает допустимый размер загрузки.');
}
export function requireNumber(value: unknown, caption: string, allowZero = false): number {
  const str = String(value ?? '').trim();
  const n = Number(str);
  if (!str || !Number.isFinite(n) || (allowZero ? n < 0 : n <= 0))
    throw new Error(`Укажите корректное значение: ${caption}`);
  return n;
}
export const todayMinsk = () =>
  new Intl.DateTimeFormat('en-CA', {
    timeZone: 'Europe/Minsk',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).format(new Date());
export function supplierTransitions(status: unknown): string[] {
  switch (status) {
    case 'CANDIDATE':
      return ['ACTIVE', 'TERMINATED'];
    case 'ACTIVE':
      return ['SUSPENDED', 'TERMINATED'];
    case 'SUSPENDED':
      return ['ACTIVE', 'TERMINATED'];
    default:
      return [];
  }
}
export function canDecide(createdById: unknown, currentId: unknown, isManager: boolean): boolean {
  return isManager && typeof createdById === 'string' && createdById !== currentId;
}
export function toOrderItems(rows: JsonRecord[]) {
  if (!rows.length) throw new Error('Добавьте хотя бы один товар.');
  return rows.map((r, i) => {
    if (!r.productId || !r.dueOn) throw new Error(`Проверьте товар и срок в строке ${i + 1}.`);
    return {
      productId: r.productId,
      quantity: requireNumber(r.quantity, `количество строки ${i + 1}`),
      unitPrice: requireNumber(r.unitPrice, `цена строки ${i + 1}`, true),
      dueOn: r.dueOn,
    };
  });
}
