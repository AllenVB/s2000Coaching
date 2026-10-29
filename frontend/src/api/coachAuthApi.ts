import { useMutation } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import type { CoachSession } from '@/types/coach'
import type { MailTestResponse } from '@/types/member'

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

/**
 * The server ends every session of the account, this tab's included, so the caller signs out
 * locally on success rather than keeping a token the server has already forgotten.
 */
export function useChangeCoachPassword() {
  return useMutation({
    mutationFn: ({
      token,
      currentPassword,
      newPassword,
    }: {
      token: string
      currentPassword: string
      newPassword: string
    }) =>
      apiFetch<void>('/api/admin/auth/password', {
        method: 'POST',
        headers: adminHeaders(token),
        body: JSON.stringify({ currentPassword, newPassword }),
      }),
  })
}

export function useCoachLogout() {
  return useMutation({
    mutationFn: ({ token }: { token: string }) =>
      apiFetch<void>('/api/admin/auth/logout', { method: 'POST', headers: adminHeaders(token) }),
  })
}

/** Mails the signed-in coach a test message and reports what the mail server said. */
export function useSendTestMail() {
  const { token } = useAdminAuth()

  return useMutation({
    mutationFn: () =>
      apiFetch<MailTestResponse>('/api/admin/mail/test', {
        method: 'POST',
        headers: adminHeaders(token!),
      }),
  })
}
