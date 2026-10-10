import { useQueryClient } from '@tanstack/react-query'
import { useCallback, useEffect } from 'react'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { ApiError } from '@/lib/api'

const ADMIN_QUERY_KEYS = ['admin-members', 'admin-member', 'admin-member-notes', 'admin-dashboard']

/** Clears the management key and every cached management response. */
export function useAdminSignOut() {
  const { signOut } = useAdminAuth()
  const queryClient = useQueryClient()

  return useCallback(() => {
    signOut()
    // The next key may belong to someone else, so nothing cached may survive.
    ADMIN_QUERY_KEYS.forEach((key) => queryClient.removeQueries({ queryKey: [key] }))
  }, [signOut, queryClient])
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
