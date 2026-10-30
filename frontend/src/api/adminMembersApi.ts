import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import type { AdminDashboard, Attention, CoachNote, UpdateMemberInput } from '@/types/admin'
import type { BodyAnalysis } from '@/types/caseStudy'
import type { CreateMemberInput, CreateMemberResponse, Member, ResendCodeResponse } from '@/types/member'

const QUERY_KEY = ['admin-members']

const memberKey = (memberId: string) => ['admin-member', memberId]
const notesKey = (memberId: string) => ['admin-member-notes', memberId]

/** Probe used by the key prompt: a 200 means the key is accepted, a 401 that it is not. */
export function fetchMembersWithToken(token: string) {
  return apiFetch<Member[]>('/api/admin/members', { headers: adminHeaders(token) })
}

export function useMembers() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: () => fetchMembersWithToken(token!),
    enabled: Boolean(token),
  })
}

export function useCreateMember() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: CreateMemberInput) =>
      apiFetch<CreateMemberResponse>('/api/admin/members', {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: ({ member: created }) => {
      queryClient.setQueryData<Member[]>(QUERY_KEY, (old) => [created, ...(old ?? [])])
      queryClient.setQueryData<Member>(memberKey(created.id), created)
      queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] })
    },
  })
}

export function useResendVerificationCode() {
  const { token } = useAdminAuth()

  return useMutation({
    mutationFn: ({ memberId }: { memberId: string }) =>
      apiFetch<ResendCodeResponse>(`/api/admin/members/${memberId}/verification-code`, {
        method: 'POST',
        headers: adminHeaders(token!),
      }),
  })
}

export function useCompleteRegistration() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ memberId, code }: { memberId: string; code: string }) =>
      apiFetch<Member>(`/api/admin/members/${memberId}/verification`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify({ code }),
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData<Member[]>(QUERY_KEY, (old) =>
        old?.map((member) => (member.id === updated.id ? updated : member)),
      )
      queryClient.setQueryData<Member>(memberKey(updated.id), updated)
      queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] })
    },
  })
}

export function useDeleteMember() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ memberId }: { memberId: string }) =>
      apiFetch<void>(`/api/admin/members/${memberId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: (_result, { memberId }) => {
      queryClient.setQueryData<Member[]>(QUERY_KEY, (old) =>
        old?.filter((member) => member.id !== memberId),
      )
      queryClient.removeQueries({ queryKey: memberKey(memberId) })
      queryClient.removeQueries({ queryKey: notesKey(memberId) })
      queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] })
    },
  })
}

export function useAdminDashboard() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-dashboard'],
    queryFn: () => apiFetch<AdminDashboard>('/api/admin/dashboard', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useMember(memberId: string | undefined) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: memberKey(memberId ?? ''),
    queryFn: () => apiFetch<Member>(`/api/admin/members/${memberId}`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token) && Boolean(memberId),
  })
}

export function useUpdateMember(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: UpdateMemberInput) =>
      apiFetch<Member>(`/api/admin/members/${memberId}`, {
        method: 'PUT',
        headers: adminHeaders(token!),
        body: JSON.stringify(input),
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData<Member>(memberKey(memberId), updated)
      queryClient.setQueryData<Member[]>(QUERY_KEY, (old) =>
        old?.map((member) => (member.id === updated.id ? updated : member)),
      )
    },
  })
}

/** The coach choosing a password for a member; the member's own sessions end. */
export function useSetMemberPassword(memberId: string) {
  const { token } = useAdminAuth()

  return useMutation({
    mutationFn: (newPassword: string) =>
      apiFetch<void>(`/api/admin/members/${memberId}/password`, {
        method: 'PUT',
        headers: adminHeaders(token!),
        body: JSON.stringify({ newPassword }),
      }),
  })
}

export function useCoachNotes(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: notesKey(memberId),
    queryFn: () => apiFetch<CoachNote[]>(`/api/admin/members/${memberId}/notes`, {
      headers: adminHeaders(token!),
    }),
    enabled: Boolean(token),
  })
}

export function useAddCoachNote(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ body }: { body: string }) =>
      apiFetch<CoachNote>(`/api/admin/members/${memberId}/notes`, {
        method: 'POST',
        headers: adminHeaders(token!),
        body: JSON.stringify({ body }),
      }),
    onSuccess: (created) => {
      queryClient.setQueryData<CoachNote[]>(notesKey(memberId), (old) => [created, ...(old ?? [])])
    },
  })
}

export function useDeleteCoachNote(memberId: string) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ noteId }: { noteId: string }) =>
      apiFetch<void>(`/api/admin/notes/${noteId}`, {
        method: 'DELETE',
        headers: adminHeaders(token!),
      }),
    onSuccess: (_result, { noteId }) => {
      queryClient.setQueryData<CoachNote[]>(notesKey(memberId), (old) =>
        old?.filter((note) => note.id !== noteId),
      )
    },
  })
}

/** Who needs the coach today, with the reasons; computed on the server for the whole roster. */
export function useAttention() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-attention'],
    queryFn: () => apiFetch<Attention>('/api/admin/attention', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
    // The coach keeps this open; a few minutes old is fine, but not a day old.
    refetchInterval: 5 * 60 * 1000,
  })
}

/** The case-study engine run on one member's check-ins and food log; coach only. */
export function useMemberAnalysis(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-member-analysis', memberId],
    queryFn: () =>
      apiFetch<BodyAnalysis>(`/api/admin/members/${memberId}/analysis`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
    staleTime: 0,
  })
}
