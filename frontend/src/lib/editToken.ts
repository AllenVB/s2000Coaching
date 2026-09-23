const STORAGE_KEY = 's2000_edit_token'

export function getStoredEditToken(): string | null {
  try {
    return window.localStorage.getItem(STORAGE_KEY)
  } catch {
    return null
  }
}

export function setStoredEditToken(token: string): void {
  try {
    window.localStorage.setItem(STORAGE_KEY, token)
  } catch {
    // localStorage unavailable (private browsing, etc.) — edits just won't persist the token.
  }
}

export function clearStoredEditToken(): void {
  try {
    window.localStorage.removeItem(STORAGE_KEY)
  } catch {
    // ignore
  }
}
