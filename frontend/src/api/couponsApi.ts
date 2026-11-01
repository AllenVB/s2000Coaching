import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'

import { useAdminAuth } from '@/context/AdminAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { apiFetch } from '@/lib/api'

export interface CouponCheck {
  code: string
  percentOff: number
  originalMinor: number
  discountedMinor: number
}

/** Public: is this code good for this package, and what does the package cost with it. */
export function useCheckCoupon() {
  return useMutation({
    mutationFn: ({ code, packageId }: { code: string; packageId: string }) =>
      apiFetch<CouponCheck>('/api/coupons/validate', { method: 'POST', body: JSON.stringify({ code, packageId }) }),
  })
}

export interface Coupon {
  id: string
  code: string
  percentOff: number
  maxUses: number | null
  usedCount: number
  expiresOn: string | null
  active: boolean
}

const KEY = ['admin-coupons']

export function useCoupons() {
  const { token } = useAdminAuth()

  return useQuery({
    queryKey: KEY,
    queryFn: () => apiFetch<Coupon[]>('/api/admin/coupons', { headers: adminHeaders(token!) }),
    enabled: Boolean(token),
  })
}

export function useCreateCoupon() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (input: { code: string; percentOff: number; maxUses: number | null; expiresOn: string | null }) =>
      apiFetch<Coupon>('/api/admin/coupons', { method: 'POST', headers: adminHeaders(token!), body: JSON.stringify(input) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  })
}

export function useSetCouponActive() {
  const { token } = useAdminAuth()
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: ({ id, active }: { id: string; active: boolean }) =>
      apiFetch<Coupon>(`/api/admin/coupons/${id}/active?active=${active}`, { method: 'PUT', headers: adminHeaders(token!) }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: KEY }),
  })
}
