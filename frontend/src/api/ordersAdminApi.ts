import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

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

function useOrderAction(action: 'confirm-payment' | 'cancel') {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (orderNumber: string) =>
      apiFetch<OrderSummary>(`/api/admin/orders/${orderNumber}/${action}`, {
        method: 'POST',
        headers: adminHeaders(token!),
      }),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ['admin-orders'] })
      queryClient.invalidateQueries({ queryKey: ['admin-member-orders'] })
      queryClient.invalidateQueries({ queryKey: ['admin-member-memberships'] })
      queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] })
    },
  })
}

/** The transfer arrived: marks the order paid and grants the membership it bought. */
export function useConfirmPayment() {
  return useOrderAction('confirm-payment')
}

export function useCancelOrder() {
  return useOrderAction('cancel')
}
