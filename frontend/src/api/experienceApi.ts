import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch, ApiError } from '@/lib/api'
import { clearStoredEditToken, getStoredEditToken } from '@/lib/editToken'
import type { ExperienceCategory, ExperienceItem } from '@/types/experience'

const QUERY_KEY = ['experience-categories']

export function useExperienceCategories() {
  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: () => apiFetch<ExperienceCategory[]>('/api/experience-categories'),
    staleTime: 60 * 1000,
  })
}

/** Wraps a request so it fails fast (without a network call) when no edit token is stored yet. */
function withEditToken<TVariables, TResult>(
  request: (variables: TVariables, token: string) => Promise<TResult>,
): (variables: TVariables) => Promise<TResult> {
  return (variables) => {
    const token = getStoredEditToken()
    if (!token) {
      throw new ApiError('Düzenleme parolası girilmedi', 401)
    }
    return request(variables, token)
  }
}

/** A wrong/expired token should be forgotten so the next attempt prompts for it again. */
function onAuthError(error: Error) {
  if (error instanceof ApiError && (error.status === 401 || error.status === 403)) {
    clearStoredEditToken()
  }
}

export function useCreateCategory() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: withEditToken<{ name: string }, ExperienceCategory>((variables, token) =>
      apiFetch('/api/experience-categories', {
        method: 'POST',
        headers: { 'X-Edit-Token': token },
        body: JSON.stringify(variables),
      }),
    ),
    onSuccess: (created) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) => [...(old ?? []), created])
    },
    onError: onAuthError,
  })
}

export function useRenameCategory() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: withEditToken<{ categoryId: string; name: string }, ExperienceCategory>(
      ({ categoryId, name }, token) =>
        apiFetch(`/api/experience-categories/${categoryId}`, {
          method: 'PUT',
          headers: { 'X-Edit-Token': token },
          body: JSON.stringify({ name }),
        }),
    ),
    onSuccess: (updated) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.map((category) => (category.id === updated.id ? { ...category, name: updated.name } : category)),
      )
    },
    onError: onAuthError,
  })
}

export function useDeleteCategory() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: withEditToken<{ categoryId: string }, void>(({ categoryId }, token) =>
      apiFetch(`/api/experience-categories/${categoryId}`, {
        method: 'DELETE',
        headers: { 'X-Edit-Token': token },
      }),
    ),
    onSuccess: (_result, { categoryId }) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.filter((category) => category.id !== categoryId),
      )
    },
    onError: onAuthError,
  })
}

export function useCreateItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: withEditToken<{ categoryId: string; title: string; description: string }, ExperienceItem>(
      ({ categoryId, title, description }, token) =>
        apiFetch(`/api/experience-categories/${categoryId}/items`, {
          method: 'POST',
          headers: { 'X-Edit-Token': token },
          body: JSON.stringify({ title, description }),
        }),
    ),
    onSuccess: (created, { categoryId }) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.map((category) =>
          category.id === categoryId ? { ...category, items: [...category.items, created] } : category,
        ),
      )
    },
    onError: onAuthError,
  })
}

export function useUpdateItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: withEditToken<{ itemId: string; title: string; description: string }, ExperienceItem>(
      ({ itemId, title, description }, token) =>
        apiFetch(`/api/experience-items/${itemId}`, {
          method: 'PUT',
          headers: { 'X-Edit-Token': token },
          body: JSON.stringify({ title, description }),
        }),
    ),
    onSuccess: (updated) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.map((category) => ({
          ...category,
          items: category.items.map((item) => (item.id === updated.id ? updated : item)),
        })),
      )
    },
    onError: onAuthError,
  })
}

export function useDeleteItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: withEditToken<{ itemId: string }, void>(({ itemId }, token) =>
      apiFetch(`/api/experience-items/${itemId}`, {
        method: 'DELETE',
        headers: { 'X-Edit-Token': token },
      }),
    ),
    onSuccess: (_result, { itemId }) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.map((category) => ({
          ...category,
          items: category.items.filter((item) => item.id !== itemId),
        })),
      )
    },
    onError: onAuthError,
  })
}
