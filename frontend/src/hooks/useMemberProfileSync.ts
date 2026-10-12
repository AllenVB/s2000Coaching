import { useEffect } from 'react'

import { useMemberProfile } from '@/api/memberAuthApi'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { ApiError } from '@/lib/api'
import type { Member } from '@/types/member'

/**
 * Refetches the signed-in member and keeps the stored session in sync. A deleted member or
 * an expired session answers 401, which signs out locally so the form comes back instead of
 * an endless error.
 */
export function useMemberProfileSync(): { member: Member | null; error: Error | null } {
  const { session, signOut, updateMember } = useMemberAuth()
  const profile = useMemberProfile(session?.token ?? null)

  const fetched = profile.data
  useEffect(() => {
    if (fetched) {
      updateMember(fetched)
    }
  }, [fetched, updateMember])

  const error = profile.error
  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      signOut()
    }
  }, [error, signOut])

  return {
    member: fetched ?? session?.member ?? null,
    error: error instanceof ApiError && error.status === 401 ? null : error,
  }
}
