import { useMutation, useQuery } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api'
import type { CreateOrderInput, OrderResponse, PaymentMethodOption } from '@/types/order'

export function useCreateOrder() {
  return useMutation({
    mutationFn: (input: CreateOrderInput) =>
      apiFetch<OrderResponse>('/api/orders', {
        method: 'POST',
        body: JSON.stringify(input),
      }),
  })
}

/** What the checkout offers right now: configured real methods, or the dev mock when none. */
export function usePaymentMethods() {
  return useQuery({
    queryKey: ['payment-methods'],
    queryFn: () => apiFetch<PaymentMethodOption[]>('/api/payment-methods'),
    staleTime: 5 * 60 * 1000,
  })
}
