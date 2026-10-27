import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { Intake, UpsertIntakeInput } from '@/types/intake'

const MEMBER_KEY = ['member-intake']
const adminKey = (memberId: string) => ['admin-member-intake', memberId]

export function useMyIntake() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () => apiFetch<Intake[]>('/api/member/intake', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

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
    onSuccess: (saved) => {
      queryClient.setQueryData<Intake[]>(MEMBER_KEY, (old) => {
        // The API upserts by date, so replace a same-day row rather than appending one.
        const rest = (old ?? []).filter((row) => row.id !== saved.id)
        return [saved, ...rest].sort((a, b) => b.consumedOn.localeCompare(a.consumedOn))
      })
    },
  })
}

export function useDeleteMyIntake() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (intakeId: string) =>
      apiFetch<void>(`/api/member/intake/${intakeId}`, {
        method: 'DELETE',
        headers: memberHeaders(session!.token),
      }),
    onSuccess: (_result, intakeId) => {
      queryClient.setQueryData<Intake[]>(MEMBER_KEY, (old) => old?.filter((row) => row.id !== intakeId))
    },
  })
}

export function useMemberIntake(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId),
    queryFn: () =>
      apiFetch<Intake[]>(`/api/admin/members/${memberId}/intake`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}
