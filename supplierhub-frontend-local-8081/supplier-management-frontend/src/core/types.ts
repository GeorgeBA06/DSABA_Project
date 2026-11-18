export type UUID = string;
export type JsonRecord = Record<string, unknown>;
export type Role = 'PROCUREMENT_SPECIALIST' | 'PROCUREMENT_MANAGER' | 'ADMINISTRATOR';
export type UserStatus = 'ACTIVE' | 'BLOCKED';
export interface User { id: UUID; login: string; displayName: string; status: UserStatus; createdAt?: string; }
export interface SessionInfo { user: User; roles: Role[]; }
export interface Tokens { accessToken: string; refreshToken: string; tokenType: string; expiresInSeconds: number; }
export interface Page<T> { items: T[]; totalElements: number; totalPages: number; page: number; size: number; }
export interface Supplier { id: UUID; name: string; contactName?: string; email?: string; phone?: string; address?: string; registrationNumber?: string; status: string; createdAt?: string; updatedAt?: string; }
export interface Product { id: UUID; name: string; sku: string; categoryId: UUID; unitId: UUID; active: boolean; }
export interface Contract { id: UUID; supplierId: UUID; state: string; currentRegisteredVersionId?: UUID; createdAt?: string; }
export interface Order { id: UUID; number: string; supplierId: UUID; contractId?: UUID; state: string; currentApprovedVersionId?: UUID; createdAt?: string; }
export interface Version { id: UUID; versionNo: number; state: string; number?: string; createdById?: UUID; submittedAt?: string; reason?: string; }
export interface Rating { id: UUID; supplierId: UUID; totalScore?: number | null; timelinessScore?: number | null; completenessScore?: number | null; qualityScore?: number | null; interactionScore?: number | null; state: string; band?: string; periodStart: string; periodEnd: string; }
export type ApiMethod = 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE';
