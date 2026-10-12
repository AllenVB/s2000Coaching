/**
 * Thin wrappers around web storage. Reads and writes throw in some privacy modes, so every
 * access is guarded and treated as "nothing stored" on failure.
 */
export function readStoredValue(storage: Storage, key: string): string | null {
  try {
    return storage.getItem(key)
  }
  catch {
    return null
  }
}

export function writeStoredValue(storage: Storage, key: string, value: string) {
  try {
    storage.setItem(key, value)
  }
  catch {
    // Non-fatal: the session simply does not survive a reload.
  }
}

export function removeStoredValue(storage: Storage, key: string) {
  try {
    storage.removeItem(key)
  }
  catch {
    // Non-fatal.
  }
}
