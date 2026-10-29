import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import { memberHeaders } from '@/lib/memberSession'
import type { CreateStoryInput, PublicStory, Story, UpdateStoryInput } from '@/types/story'

const ADMIN_KEY = ['admin-success-stories']
const MEMBER_KEY = ['member-success-stories']
const PUBLIC_KEY = ['success-stories']

// --- public ----------------------------------------------------------------------------

export function usePublishedStories() {
  return useQuery({
    queryKey: PUBLIC_KEY,
    queryFn: () => apiFetch<PublicStory[]>('/api/success-stories'),
    staleTime: 60 * 1000,
  })
}

// --- coach -----------------------------------------------------------------------------

export function useAdminStories() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ADMIN_KEY,
    queryFn: () => apiFetch<Story[]>('/api/admin/success-stories', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

function useAdminStoryMutation<TInput>(request: (input: TInput) => { path: string; method: string; body?: unknown }) {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: TInput) => {
      const { path, method, body } = request(input)
      return apiFetch<Story | void>(path, {
        method,
        headers: adminHeaders(token!),
        ...(body !== undefined ? { body: JSON.stringify(body) } : {}),
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ADMIN_KEY })
      queryClient.invalidateQueries({ queryKey: PUBLIC_KEY })
    },
  })
}

export const useCreateStory = () =>
  useAdminStoryMutation((input: CreateStoryInput) => ({
    path: '/api/admin/success-stories',
    method: 'POST',
    body: input,
  }))

export const useUpdateStory = () =>
  useAdminStoryMutation(({ id, input }: { id: string; input: UpdateStoryInput }) => ({
    path: `/api/admin/success-stories/${id}`,
    method: 'PUT',
    body: input,
  }))

export const useAddStoryPhoto = () =>
  useAdminStoryMutation(
    ({ id, progressPhotoId, caption }: { id: string; progressPhotoId: string; caption: string }) => ({
      path: `/api/admin/success-stories/${id}/photos`,
      method: 'POST',
      body: { progressPhotoId, caption },
    }),
  )

export const useRemoveStoryPhoto = () =>
  useAdminStoryMutation((photoId: string) => ({
    path: `/api/admin/success-story-photos/${photoId}`,
    method: 'DELETE',
  }))

export const useStoryAction = () =>
  useAdminStoryMutation(
    ({ id, action }: { id: string; action: 'send-for-consent' | 'publish' | 'unpublish' }) => ({
      path: `/api/admin/success-stories/${id}/${action}`,
      method: 'POST',
    }),
  )

export const useDeleteStory = () =>
  useAdminStoryMutation((id: string) => ({ path: `/api/admin/success-stories/${id}`, method: 'DELETE' }))

// --- member ----------------------------------------------------------------------------

export function useMyStories() {
  const { session } = useMemberAuth()

  return useQuery({
    queryKey: MEMBER_KEY,
    queryFn: () => apiFetch<Story[]>('/api/member/success-stories', { headers: memberHeaders(session!.token) }),
    enabled: Boolean(session),
  })
}

export function useMyStoryDecision() {
  const { session } = useMemberAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, decision }: { id: string; decision: 'consent' | 'decline' | 'revoke' }) =>
      apiFetch<Story>(`/api/member/success-stories/${id}/${decision}`, {
        method: 'POST',
        headers: memberHeaders(session!.token),
        // Consent has to be an explicit true; the server refuses the call without it.
        ...(decision === 'consent' ? { body: JSON.stringify({ agreed: true }) } : {}),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: MEMBER_KEY })
      queryClient.invalidateQueries({ queryKey: PUBLIC_KEY })
    },
  })
}
