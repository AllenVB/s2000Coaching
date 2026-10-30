import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { MonthlyRecord, MonthlyRecordInput } from '@/types/monthlyRecord'

/** What the table component needs, whoever is using it: the member or the coach for a member. */
export interface RecordSource {
  records: MonthlyRecord[] | undefined
  isLoading: boolean
  error: Error | null
  create: (input: MonthlyRecordInput) => Promise<MonthlyRecord>
  update: (id: string, input: MonthlyRecordInput) => Promise<MonthlyRecord>
  remove: (id: string) => Promise<void>
}

/** The member's own months. */
export function useMemberRecordSource(): RecordSource {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()
  const key = ['member-monthly-records']
  const headers = () => ({ ...memberHeaders(session!.token), 'Content-Type': 'application/json' })
  const refresh = () => queryClient.invalidateQueries({ queryKey: key })

  const list = useQuery({
    queryKey: key,
    queryFn: () => apiFetch<MonthlyRecord[]>('/api/member/monthly-records', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
  const create = useMutation({
    mutationFn: (input: MonthlyRecordInput) =>
      apiFetch<MonthlyRecord>('/api/member/monthly-records', { method: 'POST', headers: headers(), body: JSON.stringify(input) }),
    onSuccess: refresh,
  })
  const update = useMutation({
    mutationFn: ({ id, input }: { id: string; input: MonthlyRecordInput }) =>
      apiFetch<MonthlyRecord>(`/api/member/monthly-records/${id}`, { method: 'PUT', headers: headers(), body: JSON.stringify(input) }),
    onSuccess: refresh,
  })
  const remove = useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/api/member/monthly-records/${id}`, { method: 'DELETE', headers: memberHeaders(session!.token) }),
    onSuccess: refresh,
  })

  return {
    records: list.data,
    isLoading: list.isLoading,
    error: list.error,
    create: create.mutateAsync,
    update: (id, input) => update.mutateAsync({ id, input }),
    remove: remove.mutateAsync,
  }
}

/** A member's months as the coach sees and edits them. */
export function useCoachRecordSource(memberId: string): RecordSource {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()
  const key = ['admin-monthly-records', memberId]
  const headers = () => ({ ...adminHeaders(token!), 'Content-Type': 'application/json' })
  const refresh = () => queryClient.invalidateQueries({ queryKey: key })

  const list = useQuery({
    queryKey: key,
    queryFn: () => apiFetch<MonthlyRecord[]>(`/api/admin/members/${memberId}/monthly-records`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
  const create = useMutation({
    mutationFn: (input: MonthlyRecordInput) =>
      apiFetch<MonthlyRecord>(`/api/admin/members/${memberId}/monthly-records`, {
        method: 'POST',
        headers: headers(),
        body: JSON.stringify(input),
      }),
    onSuccess: refresh,
  })
  const update = useMutation({
    mutationFn: ({ id, input }: { id: string; input: MonthlyRecordInput }) =>
      apiFetch<MonthlyRecord>(`/api/admin/monthly-records/${id}`, { method: 'PUT', headers: headers(), body: JSON.stringify(input) }),
    onSuccess: refresh,
  })
  const remove = useMutation({
    mutationFn: (id: string) =>
      apiFetch<void>(`/api/admin/monthly-records/${id}`, { method: 'DELETE', headers: adminHeaders(token!) }),
    onSuccess: refresh,
  })

  return {
    records: list.data,
    isLoading: list.isLoading,
    error: list.error,
    create: create.mutateAsync,
    update: (id, input) => update.mutateAsync({ id, input }),
    remove: remove.mutateAsync,
  }
}
