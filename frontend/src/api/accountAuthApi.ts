import { useMutation } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api'
import type { CoachSession } from '@/types/coach'
import type { MemberSession } from '@/types/member'

/** The single sign-in answers with who signed in and that side's own session. */
export type AccountSession =
  | { role: 'COACH'; coach: CoachSession; member: null }
  | { role: 'MEMBER'; coach: null; member: MemberSession }

/** One form for everybody: the server decides whether the address is the coach's or a member's. */
export function useAccountLogin() {
  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      apiFetch<AccountSession>('/api/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),
  })
}
