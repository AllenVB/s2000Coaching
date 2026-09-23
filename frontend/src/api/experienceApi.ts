import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { apiFetch, ApiError } from '@/lib/api'
import { getStoredEditToken } from '@/lib/editToken'
import type { ExperienceCategory, ExperienceNote } from '@/types/experience'

export function useExperiences() {
  return useQuery({
    queryKey: ['experiences'],
    queryFn: () => apiFetch<ExperienceNote[]>('/api/experiences'),
    staleTime: 60 * 1000,
  })
}

interface UpdateExperienceInput {
  category: ExperienceCategory
  content: string
}

export function useUpdateExperience() {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ category, content }: UpdateExperienceInput) => {
      const token = getStoredEditToken()
      if (!token) {
        throw new ApiError('Düzenleme parolası girilmedi', 401)
      }
      return apiFetch<ExperienceNote>(`/api/experiences/${category}`, {
        method: 'PUT',
        headers: { 'X-Edit-Token': token },
        body: JSON.stringify({ content }),
      })
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['experiences'] })
    },
  })
}
