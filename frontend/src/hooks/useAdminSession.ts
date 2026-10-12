import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect } from 'react'

import { useCoachLogout } from '@/api/coachAuthApi'
import { useAdminAuth } from '@/context/AdminAuthContext'
import { ApiError } from '@/lib/api'

const ADMIN_QUERY_KEYS = [
  'admin-members',
  'admin-member',
  'admin-member-notes',
  'admin-member-measurements',
  'admin-member-messages',
  'admin-member-plans',
  'admin-member-nutrition-plans',
  'admin-member-workout-logs',
  'admin-member-orders',
  'admin-plan-templates',
  'admin-nutrition-templates',
  'admin-exercises',
  'admin-orders',
  'admin-order',
  'admin-dashboard',
]

/** Revokes the session server-side, then clears every cached management response. */
export function useAdminSignOut() {
  const { token, signOut } = useAdminAuth()
  const logout = useCoachLogout()
  const queryClient = useQueryClient()

  return useCallback(() => {
    // Sign out locally regardless of the response: the stored token is useless to us either way.
    if (token) {
      logout.mutate({ token }, { onSettled: () => signOut() })
    }
    else {
      signOut()
    }
    // Another coach may sign in next, so nothing cached may survive.
    ADMIN_QUERY_KEYS.forEach((key) => queryClient.removeQueries({ queryKey: [key] }))
  }, [token, logout, signOut, queryClient])
}

/**
 * Returns to the key prompt when the API rejects the stored key (rotated or revoked
 * server-side), instead of leaving the page stuck on an error.
 */
export function useSignOutOn401(error: unknown) {
  const signOut = useAdminSignOut()

  useEffect(() => {
    if (error instanceof ApiError && error.status === 401) {
      signOut()
    }
  }, [error, signOut])
}
