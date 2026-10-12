import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'

import { clearMemberSession, readMemberSession, storeMemberSession } from '@/lib/memberSession'
import type { Member, MemberSession } from '@/types/member'

interface MemberAuthContextValue {
  session: MemberSession | null
  signIn: (session: MemberSession) => void
  signOut: () => void
  /** Keeps the cached profile in sync after a refetch, without touching the token. */
  updateMember: (member: Member) => void
}

const MemberAuthContext = createContext<MemberAuthContextValue | null>(null)

export function MemberAuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<MemberSession | null>(() => readMemberSession())

  const signIn = useCallback((next: MemberSession) => {
    storeMemberSession(next)
    setSession(next)
  }, [])

  const signOut = useCallback(() => {
    clearMemberSession()
    setSession(null)
  }, [])

  const updateMember = useCallback((member: Member) => {
    setSession((current) => {
      // Same object means the refetch returned unchanged data (react-query keeps the
      // identity stable then); returning `current` lets React skip the re-render, which
      // is what stops the profile effect from re-running forever.
      if (!current || current.member === member) {
        return current
      }
      const next = { ...current, member }
      storeMemberSession(next)
      return next
    })
  }, [])

  const value = useMemo<MemberAuthContextValue>(
    () => ({ session, signIn, signOut, updateMember }),
    [session, signIn, signOut, updateMember],
  )

  return <MemberAuthContext.Provider value={value}>{children}</MemberAuthContext.Provider>
}

export function useMemberAuth(): MemberAuthContextValue {
  const context = useContext(MemberAuthContext)
  if (!context) {
    throw new Error('useMemberAuth must be used within a MemberAuthProvider')
  }
  return context
}
