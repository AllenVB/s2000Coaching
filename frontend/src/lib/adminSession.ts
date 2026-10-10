import { readStoredValue, removeStoredValue, writeStoredValue } from '@/lib/storage'

const STORAGE_KEY = 's2000.adminToken'

/**
 * The management key lives in sessionStorage (not localStorage) so closing the tab ends
 * management access.
 */
export function readAdminToken(): string | null {
  return readStoredValue(sessionStorage, STORAGE_KEY)
}

export function storeAdminToken(token: string) {
  writeStoredValue(sessionStorage, STORAGE_KEY, token)
}

export function clearAdminToken() {
  removeStoredValue(sessionStorage, STORAGE_KEY)
}

export function adminHeaders(token: string): Record<string, string> {
  return { 'X-Admin-Token': token }
}
