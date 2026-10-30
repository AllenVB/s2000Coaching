import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useMemberAuth } from '@/context/MemberAuthContext'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type {
  Member,
  MemberSession,
  RegisterInput,
  RegistrationResponse,
  UpdateMyProfileBody,
} from '@/types/member'

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

export function useMemberLogin() {
  return useMutation({
    mutationFn: ({ email, password }: { email: string; password: string }) =>
      apiFetch<MemberSession>('/api/member/auth/login', {
        method: 'POST',
        body: JSON.stringify({ email, password }),
      }),
  })
}

export function useRegisterMember() {
  return useMutation({
    mutationFn: (input: RegisterInput) =>
      apiFetch<RegistrationResponse>('/api/member/auth/register', {
        method: 'POST',
        body: JSON.stringify(input),
      }),
  })
}

/** Chooses a new password with the mailed code; answers with a session, so the member is in. */
export function useResetMemberPassword() {
  return useMutation({
    mutationFn: ({ email, code, newPassword }: { email: string; code: string; newPassword: string }) =>
      apiFetch<MemberSession>('/api/member/auth/reset-password', {
        method: 'POST',
        body: JSON.stringify({ email, code, newPassword }),
      }),
  })
}

/** Sets or changes the signed-in member's password; the current one is required when one exists. */
export function useChangeMemberPassword() {
  const { session, updateMember } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ currentPassword, newPassword }: { currentPassword?: string; newPassword: string }) =>
      apiFetch<void>('/api/member/password', {
        method: 'PUT',
        headers: memberHeaders(session!.token),
        body: JSON.stringify({ currentPassword: currentPassword || null, newPassword }),
      }),
    onSuccess: () => {
      // A member who had no password has one now, which changes what the profile offers.
      if (session) {
        const updated = { ...session.member, hasPassword: true }
        updateMember(updated)
        queryClient.setQueryData<Member>(['member-profile'], updated)
      }
    },
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
