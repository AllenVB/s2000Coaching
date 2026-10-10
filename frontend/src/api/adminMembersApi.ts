import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import type { CreateMemberInput, Member } from '@/types/member'

const QUERY_KEY = ['admin-members']

/** Probe used by the key prompt: a 200 means the key is accepted, a 401 that it is not. */
export function fetchMembersWithToken(token: string) {
  return apiFetch<Member[]>('/api/admin/members', { headers: adminHeaders(token) })
}

export function useMembers() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: () => fetchMembersWithToken(token!),
    enabled: Boolean(token),
  })
}

export function useCreateMember() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: CreateMemberInput) =>
      apiFetch<Member>('/api/admin/members', {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: (created) => {
      queryClient.setQueryData<Member[]>(QUERY_KEY, (old) => [created, ...(old ?? [])])
    },
  })
}

export function useResendVerificationCode() {
  const { token } = useAdminAuth()

  return useMutation({
    mutationFn: ({ memberId }: { memberId: string }) =>
      apiFetch<void>(`/api/admin/members/${memberId}/verification-code`, {
        method: 'POST',
        headers: adminHeaders(token!),
      }),
  })
}

export function useCompleteRegistration() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ memberId, code }: { memberId: string; code: string }) =>
      apiFetch<Member>(`/api/admin/members/${memberId}/verification`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify({ code }),
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData<Member[]>(QUERY_KEY, (old) =>
        old?.map((member) => (member.id === updated.id ? updated : member)),
      )
    },
  })
}

export function useDeleteMember() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ memberId }: { memberId: string }) =>
      apiFetch<void>(`/api/admin/members/${memberId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: (_result, { memberId }) => {
      queryClient.setQueryData<Member[]>(QUERY_KEY, (old) =>
        old?.filter((member) => member.id !== memberId),
      )
    },
  })
}
