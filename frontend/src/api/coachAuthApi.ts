import { useMutation } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import type { CoachSession } from '@/types/coach'

export function useCoachLogin() {
  const { signIn } = useAdminAuth()

  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      apiFetch<CoachSession>('/api/admin/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),
    onSuccess: (session) => signIn(session),
  })
}

export function useCoachLogout() {
  return useMutation({
    mutationFn: ({ token }: { token: string }) =>
      apiFetch<void>('/api/admin/auth/logout', { method: 'POST', headers: adminHeaders(token) }),
  })
}
