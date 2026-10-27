import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { CheckIn, UpsertCheckInInput } from '@/types/checkIn'

const MEMBER_KEY = ['member-check-ins']
const adminKey = (memberId: string) => ['admin-member-check-ins', memberId]

export function useMyCheckIns() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () => apiFetch<CheckIn[]>('/api/member/check-ins', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export function useSaveMyCheckIn() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpsertCheckInInput) =>
      apiFetch<CheckIn>('/api/member/check-ins', {
        method: 'POST',
        headers: memberHeaders(session!.token),
        body: JSON.stringify(input),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEMBER_KEY }),
  })
}

export function useMemberCheckIns(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId),
    queryFn: () =>
      apiFetch<CheckIn[]>(`/api/admin/members/${memberId}/check-ins`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useReplyToCheckIn(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ checkInId, reply }: { checkInId: string; reply: string }) =>
      apiFetch<CheckIn>(`/api/admin/check-ins/${checkInId}/reply`, {
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
