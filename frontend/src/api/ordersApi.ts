import { useMutation } from '@tanstack/react-query'

import { apiFetch } from '@/lib/api'
import type { CreateOrderInput, OrderResponse } from '@/types/order'

export function useCreateOrder() {
  return useMutation({
    mutationFn: (input: CreateOrderInput) =>
      apiFetch<OrderResponse>('/api/orders', {
        method: 'POST',
        body: JSON.stringify(input),
      }),
  })
}
