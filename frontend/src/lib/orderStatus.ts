import type { OrderStatus } from '@/types/order'

/** How an order status reads to the coach, and whether it is good, waiting, or bad news. */
export const ORDER_STATUS: Record<OrderStatus, { label: string; tone: 'success' | 'warning' | 'error' | 'muted' }> = {
  PENDING: { label: 'Beklemede', tone: 'warning' },
  AWAITING_TRANSFER: { label: 'Havale bekleniyor', tone: 'warning' },
  AWAITING_PAYMENT: { label: 'Kart ödemesi bekleniyor', tone: 'warning' },
  PAID: { label: 'Ödendi', tone: 'success' },
  MOCK_PAID: { label: 'Test siparişi', tone: 'muted' },
  FAILED: { label: 'Başarısız', tone: 'error' },
  CANCELLED: { label: 'İptal', tone: 'muted' },
}

export const TONE_CLASSES = {
  success: 'border-success/40 bg-success/10 text-success',
  warning: 'border-warning/40 bg-warning/10 text-warning',
  error: 'border-error/40 bg-error/10 text-error',
  muted: 'border-border bg-surface-elevated text-text-muted',
} as const
