import { readStoredValue, removeStoredValue, writeStoredValue } from '@/lib/storage'
import type { CoachSession } from '@/types/coach'

const STORAGE_KEY = 's2000.coachSession'

/**
 * The coach session lives in sessionStorage, not localStorage: management access ends when
 * the tab closes, which is the right default for a page that can delete members.
 */
export function readCoachSession(): CoachSession | null {
  const raw = readStoredValue(sessionStorage, STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as CoachSession
    if (!parsed.token || Date.parse(parsed.expiresAt) <= Date.now()) {
      removeStoredValue(sessionStorage, STORAGE_KEY)
      return null
    }
    return parsed
  }
  catch {
    removeStoredValue(sessionStorage, STORAGE_KEY)
    return null
  }
}

export function storeCoachSession(session: CoachSession) {
  writeStoredValue(sessionStorage, STORAGE_KEY, JSON.stringify(session))
}

export function clearCoachSession() {
  removeStoredValue(sessionStorage, STORAGE_KEY)
}

export function adminHeaders(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` }
}
