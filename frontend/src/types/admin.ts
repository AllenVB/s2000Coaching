import type { Member } from '@/types/member'

export interface CoachNote {
  id: string
  body: string
  createdAt: string
  updatedAt: string
}

export type OrderStatus = 'PENDING' | 'MOCK_PAID' | 'FAILED'

export interface OrderSummary {
  orderNumber: string
  customerName: string
  email: string
  phone: string
  packageName: string
  priceMinor: number
  status: OrderStatus
  createdAt: string
}

export interface AdminDashboard {
  totalMembers: number
  activeMembers: number
  /** Window the server used for `staleMembers`, so the UI does not hardcode it. */
  staleAfterDays: number
  pendingMembers: Member[]
  /** Active members with no check-in inside that window. */
  staleMembers: Member[]
  recentOrders: OrderSummary[]
}

export interface UpdateMemberInput {
  fullName: string
  phone: string
  heightCm: number
  weightKg: number
  sportsBackground: string
}
