import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { ConversationSummary, Message, MessageTemplate, UnreadSummary } from '@/types/message'

const MEMBER_KEY = ['member-messages']
const MEMBER_UNREAD_KEY = ['member-messages-unread']
const ADMIN_THREADS_KEY = ['admin-message-threads']
const ADMIN_UNREAD_KEY = ['admin-messages-unread']
const TEMPLATES_KEY = ['admin-message-templates']
const adminKey = (memberId: string) => ['admin-member-messages', memberId]

/**
 * How often an open thread and the nav badges re-check the server. Short enough that a reply
 * lands while the other side is still looking at the screen, long enough not to poll hard.
 */
const THREAD_POLL_MS = 15_000
const BADGE_POLL_MS = 60_000

/** Fetching the thread also marks the other side's messages read, server-side. */
export function useMyMessages() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () => apiFetch<Message[]>('/api/member/messages', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
    refetchInterval: THREAD_POLL_MS,
  })
}

/** The nav badge. Separate from the thread because reading the thread clears the badge. */
export function useMyUnreadMessages() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_UNREAD_KEY,
    queryFn: () =>
      apiFetch<UnreadSummary>('/api/member/messages/unread', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
    refetchInterval: BADGE_POLL_MS,
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

export function useMemberMessages(memberId: string | undefined) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId ?? ''),
    queryFn: () =>
      apiFetch<Message[]>(`/api/admin/members/${memberId}/messages`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token) && Boolean(memberId),
    refetchInterval: THREAD_POLL_MS,
  })
}

/** The coach's inbox: every member, waiting-for-reply threads first. */
export function useConversations() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ADMIN_THREADS_KEY,
    queryFn: () =>
      apiFetch<ConversationSummary[]>('/api/admin/messages/threads', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
    refetchInterval: THREAD_POLL_MS,
  })
}

export function useCoachUnreadMessages() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ADMIN_UNREAD_KEY,
    queryFn: () =>
      apiFetch<UnreadSummary>('/api/admin/messages/unread', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
    refetchInterval: BADGE_POLL_MS,
  })
}

export function useSendCoachMessage(memberId: string | undefined) {
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
      queryClient.setQueryData<Message[]>(adminKey(memberId ?? ''), (old) => [...(old ?? []), sent])
      // Replying clears this member from the panel's reply queue and moves the inbox row.
      queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] })
      queryClient.invalidateQueries({ queryKey: ADMIN_THREADS_KEY })
      queryClient.invalidateQueries({ queryKey: ADMIN_UNREAD_KEY })
    },
  })
}

export function useMessageTemplates() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: TEMPLATES_KEY,
    queryFn: () =>
      apiFetch<MessageTemplate[]>('/api/admin/message-templates', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useCreateMessageTemplate() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ title, body }: { title: string; body: string }) =>
      apiFetch<MessageTemplate>('/api/admin/message-templates', {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify({ title, body }),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEMPLATES_KEY }),
  })
}

export function useDeleteMessageTemplate() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ templateId }: { templateId: string }) =>
      apiFetch<void>(`/api/admin/message-templates/${templateId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: TEMPLATES_KEY }),
  })
}
