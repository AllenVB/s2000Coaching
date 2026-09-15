import { useQuery } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api'
import type { CoachingPackage } from '@/types/package'

export function usePackages() {
  return useQuery({
    queryKey: ['packages'],
    queryFn: () => apiFetch<CoachingPackage[]>('/api/packages'),
    staleTime: 5 * 60 * 1000,
  })
}
