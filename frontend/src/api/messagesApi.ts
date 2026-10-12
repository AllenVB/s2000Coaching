import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { Message } from '@/types/message'

const MEMBER_KEY = ['member-messages']
const adminKey = (memberId: string) => ['admin-member-messages', memberId]

/** Fetching the thread also marks the other side's messages read, server-side. */
export function useMyMessages() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () => apiFetch<Message[]>('/api/member/messages', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export function useSendMyMessage() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ body }: { body: string }) =>
      apiFetch<Message>('/api/member/messages', {
        method: 'POST',
        headers: memberHeaders(session!.token),
        body: JSON.stringify({ body }),
      }),
    onSuccess: (sent) => {
      queryClient.setQueryData<Message[]>(MEMBER_KEY, (old) => [...(old ?? []), sent])
    },
  })
}

export function useMemberMessages(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId),
    queryFn: () =>
      apiFetch<Message[]>(`/api/admin/members/${memberId}/messages`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useSendCoachMessage(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ body }: { body: string }) =>
      apiFetch<Message>(`/api/admin/members/${memberId}/messages`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify({ body }),
      }),
    onSuccess: (sent) => {
      queryClient.setQueryData<Message[]>(adminKey(memberId), (old) => [...(old ?? []), sent])
      // Replying clears this member from the panel's reply queue.
      queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] })
    },
  })
}
