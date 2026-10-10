import { useMutation, useQuery } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { Member, MemberSession } from '@/types/member'

export function useRequestMemberCode() {
  return useMutation({
    mutationFn: ({ email }: { email: string }) =>
      apiFetch<void>('/api/member/auth/request-code', {
        method: 'POST',
        body: JSON.stringify({ email }),
      }),
  })
}

export function useVerifyMemberCode() {
  return useMutation({
    mutationFn: ({ email, code }: { email: string; code: string }) =>
      apiFetch<MemberSession>('/api/member/auth/verify-code', {
        method: 'POST',
        body: JSON.stringify({ email, code }),
      }),
  })
}

/** Refetches the signed-in member so a profile edited by the coach shows up on reload. */
export function useMemberProfile(token: string | null) {
  return useQuery({
    queryKey: ['member-profile'],
    queryFn: () => apiFetch<Member>('/api/member/me', { headers: memberHeaders(token!) }),
    enabled: Boolean(token),
    retry: false,
  })
}

export function useMemberLogout() {
  return useMutation({
    mutationFn: ({ token }: { token: string }) =>
      apiFetch<void>('/api/member/auth/logout', {
        method: 'POST',
        headers: memberHeaders(token),
      }),
  })
}
