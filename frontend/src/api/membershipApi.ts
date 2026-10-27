import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { Membership } from '@/types/membership'

const adminKey = (memberId: string) => ['admin-member-memberships', memberId]

export function useMyMemberships() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: ['member-memberships'],
    queryFn: () =>
      apiFetch<Membership[]>('/api/member/memberships', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export function useMemberMemberships(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId),
    queryFn: () =>
      apiFetch<Membership[]>(`/api/admin/members/${memberId}/memberships`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useGrantMembership(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: { label: string; months: number }) =>
      apiFetch<Membership>(`/api/admin/members/${memberId}/memberships`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminKey(memberId) }),
  })
}

export function useDeleteMembership(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (membershipId: string) =>
      apiFetch<void>(`/api/admin/memberships/${membershipId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: adminKey(memberId) }),
  })
}
