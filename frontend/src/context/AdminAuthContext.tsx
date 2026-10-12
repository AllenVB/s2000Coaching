import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

import { clearCoachSession, readCoachSession, storeCoachSession } from '@/lib/adminSession'
import type { CoachSession } from '@/types/coach'

interface AdminAuthContextValue {
  session: CoachSession | null
  /** The bearer token, or null when signed out — what the API hooks read. */
  token: string | null
  signIn: (session: CoachSession) => void
  signOut: () => void
}

const AdminAuthContext = createContext<AdminAuthContextValue | null>(null)

export function AdminAuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<CoachSession | null>(() => readCoachSession())

  const signIn = useCallback((next: CoachSession) => {
    storeCoachSession(next)
    setSession(next)
  }, [])

  const signOut = useCallback(() => {
    clearCoachSession()
    setSession(null)
  }, [])

  const value = useMemo<AdminAuthContextValue>(
    () => ({ session, token: session?.token ?? null, signIn, signOut }),
    [session, signIn, signOut],
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
