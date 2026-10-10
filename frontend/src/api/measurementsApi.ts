import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { Measurement, UpsertMeasurementInput } from '@/types/measurement'
import type { Member } from '@/types/member'

const MEMBER_KEY = ['member-measurements']
const adminKey = (memberId: string) => ['admin-member-measurements', memberId]

/* ---------- member side ---------- */

export function useMyMeasurements() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () =>
      apiFetch<Measurement[]>('/api/member/measurements', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export function useSaveMyMeasurement() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpsertMeasurementInput) =>
      apiFetch<Measurement>('/api/member/measurements', {
        method: 'POST',
        headers: memberHeaders(session!.token),
        body: JSON.stringify(input),
      }),
    onSuccess: (saved) => {
      queryClient.setQueryData<Measurement[]>(MEMBER_KEY, (old) => {
        // The API upserts by date, so replace a same-day row rather than appending one.
        const rest = (old ?? []).filter((row) => row.id !== saved.id)
        return [saved, ...rest].sort((a, b) => b.measuredOn.localeCompare(a.measuredOn))
      })
    },
  })
}

export function useDeleteMyMeasurement() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ measurementId }: { measurementId: string }) =>
      apiFetch<void>(`/api/member/measurements/${measurementId}`, {
        method: 'DELETE',
        headers: memberHeaders(session!.token),
      }),
    onSuccess: (_result, { measurementId }) => {
      queryClient.setQueryData<Measurement[]>(MEMBER_KEY, (old) =>
        old?.filter((row) => row.id !== measurementId),
      )
    },
  })
}

export function useGrantHealthDataConsent() {
  const { session, updateMember } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: () =>
      apiFetch<Member>('/api/member/consent/health-data', {
        method: 'POST',
        headers: memberHeaders(session!.token),
      }),
    onSuccess: (member) => {
      // Two readers of the profile: the stored session and the `member-profile` query, which
      // `useMemberProfileSync` prefers. Both have to move, or the page keeps the stale copy.
      updateMember(member)
      queryClient.setQueryData<Member>(['member-profile'], member)
    },
  })
}

/* ---------- coach side ---------- */

export function useMemberMeasurements(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId),
    queryFn: () =>
      apiFetch<Measurement[]>(`/api/admin/members/${memberId}/measurements`, {
        headers: adminHeaders(token!),
      }),
    enabled: Boolean(token),
  })
}

export function useDeleteMemberMeasurement(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ measurementId }: { measurementId: string }) =>
      apiFetch<void>(`/api/admin/measurements/${measurementId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: (_result, { measurementId }) => {
      queryClient.setQueryData<Measurement[]>(adminKey(memberId), (old) =>
        old?.filter((row) => row.id !== measurementId),
      )
      queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] })
    },
  })
}
