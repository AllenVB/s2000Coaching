import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { CreateFormCheckInput, FormCheck } from '@/types/formCheck'

const MEMBER_KEY = ['member-form-checks']
const adminKey = (memberId: string) => ['admin-member-form-checks', memberId]

export function useMyFormChecks() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () => apiFetch<FormCheck[]>('/api/member/form-checks', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export function useCreateFormCheck() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: CreateFormCheckInput) =>
      apiFetch<FormCheck>('/api/member/form-checks', {
        method: 'POST',
        headers: memberHeaders(session!.token),
        body: JSON.stringify(input),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEMBER_KEY }),
  })
}

export function useDeleteFormCheck() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/api/member/form-checks/${id}`, {
        method: 'DELETE',
        headers: memberHeaders(session!.token),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEMBER_KEY }),
  })
}

export function useMemberFormChecks(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId),
    queryFn: () =>
      apiFetch<FormCheck[]>(`/api/admin/members/${memberId}/form-checks`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useReplyToFormCheck(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, reply }: { id: string; reply: string }) =>
      apiFetch<FormCheck>(`/api/admin/form-checks/${id}/reply`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify({ reply }),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: adminKey(memberId) })
      queryClient.invalidateQueries({ queryKey: ['admin-attention'] })
    },
  })
}
