import { useQuery } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import type { Intake } from '@/types/intake'

const adminKey = (memberId: string) => ['admin-member-intake', memberId]

export function useMemberIntake(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: adminKey(memberId),
    queryFn: () =>
      apiFetch<Intake[]>(`/api/admin/members/${memberId}/intake`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}
