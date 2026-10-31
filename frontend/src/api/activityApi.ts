import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { Activity, UpsertActivityInput } from '@/types/activity'

const MEMBER_KEY = ['member-activity']

export function useMyActivity() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () => apiFetch<Activity[]>('/api/member/activity', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

/** The API upserts by date, so saving a day that exists corrects it. */
export function useSaveMyActivity() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpsertActivityInput) =>
      apiFetch<Activity>('/api/member/activity', {
        method: 'POST',
        headers: memberHeaders(session!.token),
        body: JSON.stringify(input),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEMBER_KEY }),
  })
}

export function useDeleteMyActivity() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/api/member/activity/${id}`, { method: 'DELETE', headers: memberHeaders(session!.token) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEMBER_KEY }),
  })
}

export function useMemberActivity(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-member-activity', memberId],
    queryFn: () =>
      apiFetch<Activity[]>(`/api/admin/members/${memberId}/activity`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}
