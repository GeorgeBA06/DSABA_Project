import type { ApiMethod, Page, Tokens } from './types';

const BASE = (import.meta.env?.VITE_API_BASE_URL || '/api/v1').replace(/\/$/, '');
const REFRESH_KEY = 'supplierhub_refresh';
export class ApiError extends Error {
  readonly status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}
function readRefresh(): string | null {
  return sessionStorage.getItem(REFRESH_KEY);
}
function setRefresh(value: string | null): void {
  if (value) sessionStorage.setItem(REFRESH_KEY, value);
  else sessionStorage.removeItem(REFRESH_KEY);
}
async function parseResponse<T>(res: Response): Promise<T> {
  if (res.status === 204) return undefined as T;
  const type = res.headers.get('content-type') || '';
  const text = await res.text();
  let body: unknown = text;
  if (type.includes('json') && text) {
    try {
      body = JSON.parse(text);
    } catch {
      /* invalid JSON is handled below */
    }
  }
  if (!res.ok) {
    const obj = body && typeof body === 'object' ? (body as Record<string, unknown>) : {};
    const reason =
      typeof obj.message === 'string'
        ? obj.message
        : typeof obj.detail === 'string'
          ? obj.detail
          : '';
    throw new ApiError(
      res.status,
      reason ||
        (
          {
            400: 'Неверные данные',
            401: 'Сеанс истёк',
            403: 'Недостаточно прав',
            404: 'Запись не найдена',
            409: 'Конфликт изменений',
            500: 'Ошибка сервера',
          } as Record<number, string>
        )[res.status] ||
        `Ошибка HTTP ${res.status}`,
    );
  }
  return body as T;
}
export class ApiClient {
  private accessToken: string | null = null;
  private pendingRefresh: Promise<boolean> | null = null;
  private expireHandler: (() => void) | null = null;
  onExpire(callback: () => void) {
    this.expireHandler = callback;
  }
  hasRefresh() {
    return Boolean(readRefresh());
  }
  clear() {
    this.accessToken = null;
    setRefresh(null);
  }
  install(tokens: Tokens) {
    this.accessToken = tokens.accessToken;
    setRefresh(tokens.refreshToken);
  }
  async login(login: string, password: string) {
    const response = await this.send('POST', '/auth/login', { login, password });
    const tokens = await parseResponse<Tokens>(response);
    this.install(tokens);
    return tokens;
  }
  private async send(
    method: ApiMethod,
    path: string,
    body?: unknown,
    bearer?: string | null,
  ): Promise<Response> {
    try {
      return await fetch(`${BASE}${path}`, {
        method,
        credentials: 'omit',
        headers: {
          ...(body === undefined ? {} : { 'Content-Type': 'application/json' }),
          ...(bearer ? { Authorization: `Bearer ${bearer}` } : {}),
        },
        body: body === undefined ? undefined : JSON.stringify(body),
      });
    } catch {
      throw new ApiError(0, 'Сервер недоступен. Проверьте соединение и адрес API.');
    }
  }
  async refresh(): Promise<boolean> {
    if (this.pendingRefresh) return this.pendingRefresh;
    const token = readRefresh();
    if (!token) return false;
    this.pendingRefresh = (async () => {
      try {
        const response = await this.send('POST', '/auth/refresh', { refreshToken: token });
        this.install(await parseResponse<Tokens>(response));
        return true;
      } catch {
        this.expireSession();
        return false;
      }
    })();
    try {
      return await this.pendingRefresh;
    } finally {
      this.pendingRefresh = null;
    }
  }
  private expireSession() {
    if (!this.accessToken && !this.hasRefresh()) return;
    this.clear();
    this.expireHandler?.();
  }

  private async authorizedResponse(
    method: ApiMethod,
    path: string,
    body?: unknown,
  ): Promise<Response> {
    if (!this.accessToken && this.hasRefresh() && !(await this.refresh())) {
      throw new ApiError(401, 'Сеанс истёк. Войдите повторно.');
    }

    const bearer = this.accessToken;
    let response = await this.send(method, path, body, bearer);
    if (response.status !== 401) return response;

    if (this.hasRefresh()) {
      // A concurrent request may already have refreshed the access token.
      if (bearer === this.accessToken && !(await this.refresh())) {
        throw new ApiError(401, 'Сеанс истёк. Войдите повторно.');
      }
      response = await this.send(method, path, body, this.accessToken);
    }

    if (response.status === 401) this.expireSession();
    return response;
  }

  async request<T>(method: ApiMethod, path: string, body?: unknown): Promise<T> {
    return parseResponse<T>(await this.authorizedResponse(method, path, body));
  }
  get<T>(path: string) {
    return this.request<T>('GET', path);
  }
  post<T>(path: string, body?: unknown) {
    return this.request<T>('POST', path, body);
  }
  put<T>(path: string, body?: unknown) {
    return this.request<T>('PUT', path, body);
  }
  patch<T>(path: string, body?: unknown) {
    return this.request<T>('PATCH', path, body);
  }
  delete<T>(path: string) {
    return this.request<T>('DELETE', path);
  }
  page<T>(path: string, page = 0, size = 20) {
    const separator = path.includes('?') ? '&' : '?';
    return this.get<Page<T>>(`${path}${separator}page=${page}&size=${size}`);
  }
  async download(path: string, filename: string) {
    const res = await this.authorizedResponse('GET', path);
    if (!res.ok) return parseResponse<never>(res);
    const blob = await res.blob();
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = filename;
    document.body.appendChild(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
  }
  async logout() {
    try {
      if (this.accessToken) await this.request<void>('POST', '/auth/logout');
    } finally {
      this.clear();
    }
  }
}
export const api = new ApiClient();
