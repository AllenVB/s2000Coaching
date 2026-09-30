import { useQuery } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api'
import type { ExperienceCategory } from '@/types/experience'

export function useExperienceCategories() {
  return useQuery({
    queryKey: ['experience-categories'],
    queryFn: () => apiFetch<ExperienceCategory[]>('/api/experience-categories'),
    staleTime: 60 * 1000,
  })
}
