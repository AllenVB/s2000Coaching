import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useMemberAuth } from '@/context/MemberAuthContext'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { Member, MemberSession, UpdateMyProfileBody } from '@/types/member'

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

/**
 * The member editing their own details. The API deliberately takes fewer fields than the
 * coach's form: see `UpdateOwnProfileRequest` on the backend for what is withheld and why.
 */
export function useUpdateMyProfile() {
  const { session, updateMember } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (body: UpdateMyProfileBody) =>
      apiFetch<Member>('/api/member/me', {
        method: 'PATCH',
        headers: memberHeaders(session!.token),
        body: JSON.stringify(body),
      }),
    onSuccess: (member) => {
      // Two readers of the profile: the stored session and the `member-profile` query, which
      // `useMemberProfileSync` prefers. Both have to move, or the page keeps the stale copy.
      updateMember(member)
      queryClient.setQueryData<Member>(['member-profile'], member)
    },
  })
}

export function useSetReminders() {
  const { session, updateMember } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (enabled: boolean) =>
      apiFetch<Member>('/api/member/reminders', {
        method: 'PUT',
        headers: memberHeaders(session!.token),
        body: JSON.stringify({ enabled }),
      }),
    onSuccess: (member) => {
      updateMember(member)
      queryClient.setQueryData<Member>(['member-profile'], member)
    },
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
