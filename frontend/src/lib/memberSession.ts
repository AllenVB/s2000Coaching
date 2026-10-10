import { readStoredValue, removeStoredValue, writeStoredValue } from '@/lib/storage'
import type { MemberSession } from '@/types/member'

const STORAGE_KEY = 's2000.memberSession'

/** Returns the stored session, or null when there is none or it has already expired. */
export function readMemberSession(): MemberSession | null {
  const raw = readStoredValue(localStorage, STORAGE_KEY)
  if (!raw) {
    return null
  }

  try {
    const parsed = JSON.parse(raw) as MemberSession
    if (!parsed.token || Date.parse(parsed.expiresAt) <= Date.now()) {
      removeStoredValue(localStorage, STORAGE_KEY)
      return null
    }
    return parsed
  }
  catch {
    removeStoredValue(localStorage, STORAGE_KEY)
    return null
  }
}

export function storeMemberSession(session: MemberSession) {
  writeStoredValue(localStorage, STORAGE_KEY, JSON.stringify(session))
}

export function clearMemberSession() {
  removeStoredValue(localStorage, STORAGE_KEY)
}

export function memberHeaders(token: string): Record<string, string> {
  return { Authorization: `Bearer ${token}` }
}
