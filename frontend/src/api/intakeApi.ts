import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { Intake, UpsertIntakeInput } from '@/types/intake'

const MEMBER_KEY = ['member-intake']
const adminKey = (memberId: string) => ['admin-member-intake', memberId]

export function useMemberIntake(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId),
    queryFn: () =>
      apiFetch<Intake[]>(`/api/admin/members/${memberId}/intake`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

/* ---------- member side: the daily food and water log ---------- */

export function useMyIntake() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () => apiFetch<Intake[]>('/api/member/intake', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

/** The API upserts by date, so saving a day that exists corrects it. */
export function useSaveMyIntake() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpsertIntakeInput) =>
      apiFetch<Intake>('/api/member/intake', {
        method: 'POST',
        headers: memberHeaders(session!.token),
        body: JSON.stringify(input),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEMBER_KEY }),
  })
}

export function useDeleteMyIntake() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (intakeId: string) =>
      apiFetch<void>(`/api/member/intake/${intakeId}`, { method: 'DELETE', headers: memberHeaders(session!.token) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: MEMBER_KEY }),
  })
}
