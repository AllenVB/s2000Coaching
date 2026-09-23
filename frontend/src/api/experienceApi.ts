import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api'
import type { ExperienceCategory, ExperienceItem } from '@/types/experience'

const QUERY_KEY = ['experience-categories']

export function useExperienceCategories() {
  return useQuery({
    queryKey: QUERY_KEY,
    queryFn: () => apiFetch<ExperienceCategory[]>('/api/experience-categories'),
    staleTime: 60 * 1000,
  })
}

export function useCreateCategory() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (variables: { name: string }) =>
      apiFetch<ExperienceCategory>('/api/experience-categories', {
        method: 'POST',
        body: JSON.stringify(variables),
      }),
    onSuccess: (created) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) => [...(old ?? []), created])
    },
  })
}

export function useRenameCategory() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ categoryId, name }: { categoryId: string; name: string }) =>
      apiFetch<ExperienceCategory>(`/api/experience-categories/${categoryId}`, {
        method: 'PUT',
        body: JSON.stringify({ name }),
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.map((category) => (category.id === updated.id ? { ...category, name: updated.name } : category)),
      )
    },
  })
}

export function useDeleteCategory() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ categoryId }: { categoryId: string }) =>
      apiFetch<void>(`/api/experience-categories/${categoryId}`, { method: 'DELETE' }),
    onSuccess: (_result, { categoryId }) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.filter((category) => category.id !== categoryId),
      )
    },
  })
}

export function useCreateItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ categoryId, title, description }: { categoryId: string; title: string; description: string }) =>
      apiFetch<ExperienceItem>(`/api/experience-categories/${categoryId}/items`, {
        method: 'POST',
        body: JSON.stringify({ title, description }),
      }),
    onSuccess: (created, { categoryId }) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.map((category) =>
          category.id === categoryId ? { ...category, items: [...category.items, created] } : category,
        ),
      )
    },
  })
}

export function useUpdateItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ itemId, title, description }: { itemId: string; title: string; description: string }) =>
      apiFetch<ExperienceItem>(`/api/experience-items/${itemId}`, {
        method: 'PUT',
        body: JSON.stringify({ title, description }),
      }),
    onSuccess: (updated) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.map((category) => ({
          ...category,
          items: category.items.map((item) => (item.id === updated.id ? updated : item)),
        })),
      )
    },
  })
}

export function useDeleteItem() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ itemId }: { itemId: string }) =>
      apiFetch<void>(`/api/experience-items/${itemId}`, { method: 'DELETE' }),
    onSuccess: (_result, { itemId }) => {
      queryClient.setQueryData<ExperienceCategory[]>(QUERY_KEY, (old) =>
        old?.map((category) => ({
          ...category,
          items: category.items.filter((item) => item.id !== itemId),
        })),
      )
    },
  })
}
