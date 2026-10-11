import { useQuery } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'
import type { OrderSummary } from '@/types/admin'

export function useOrders() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-orders'],
    queryFn: () => apiFetch<OrderSummary[]>('/api/admin/orders', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

/** Used to prefill the member form when converting an order into a membership. */
export function useOrder(orderNumber: string | null) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-order', orderNumber],
    queryFn: () =>
      apiFetch<OrderSummary>(`/api/admin/orders/${orderNumber}`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token) && Boolean(orderNumber),
  })
}

export function useMemberOrders(memberId: string) {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: ['admin-member-orders', memberId],
    queryFn: () =>
      apiFetch<OrderSummary[]>(`/api/admin/members/${memberId}/orders`, { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}
