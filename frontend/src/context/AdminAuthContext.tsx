import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

import { clearAdminToken, readAdminToken, storeAdminToken } from '@/lib/adminSession'

interface AdminAuthContextValue {
  token: string | null
  signIn: (token: string) => void
  signOut: () => void
}

const AdminAuthContext = createContext<AdminAuthContextValue | null>(null)

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [token, setToken] = useState<string | null>(() => readAdminToken())

  const signIn = useCallback((next: string) => {
    storeAdminToken(next)
    setToken(next)
  }, [])

  const signOut = useCallback(() => {
    clearAdminToken()
    setToken(null)
  }, [])

  const value = useMemo<AdminAuthContextValue>(
    () => ({ token, signIn, signOut }),
    [token, signIn, signOut],
  )

  return <AdminAuthContext.Provider value={value}>{children}</AdminAuthContext.Provider>
}

export function useAdminAuth(): AdminAuthContextValue {
  const context = useContext(AdminAuthContext)
  if (!context) {
    throw new Error('useAdminAuth must be used within an AdminAuthProvider')
  }
  return context
}
