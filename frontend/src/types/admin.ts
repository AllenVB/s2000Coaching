import type { MemberGender } from '@/types/member'
import type { Member } from '@/types/member'

export interface CoachNote {
  id: string
  body: string
  createdAt: string
  updatedAt: string
}

import type { OrderStatus, PaymentMethod } from '@/types/order'

export type { OrderStatus }

export interface OrderSummary {
  orderNumber: string
  customerName: string
  email: string
  phone: string
  cityAge: string | null
  packageName: string
  priceMinor: number
  status: OrderStatus
  createdAt: string
  /** The member this order belongs to, or null if the buyer is not one yet. */
  memberId: string | null
  paymentMethod: PaymentMethod
  /** When the money was confirmed; null until then (and always null for the development gateway). */
  paidAt: string | null
}

export interface AdminDashboard {
  totalMembers: number
  activeMembers: number
  /** Window the server used for `staleMembers`, so the UI does not hardcode it. */
  staleAfterDays: number
  pendingMembers: Member[]
  /** Active members with no check-in inside that window. */
  staleMembers: Member[]
  /** Members whose last message to the coach is still unread. */
  awaitingReply: Member[]
  recentOrders: OrderSummary[]
}

export interface UpdateMemberInput {
  email: string
  fullName: string
  gender: MemberGender | null
  age: number | null
  phone: string
  heightCm: number
  weightKg: number
  sportsBackground: string
}

export interface AttentionReason {
  /** Stable code for the UI: MESSAGE, NO_WORKOUT, NO_MEASUREMENT, NO_INTAKE, MEMBERSHIP_ENDING, MEMBERSHIP_ENDED. */
  code: string
  label: string
  /** 1 (worth a glance) to 5 (act today). */
  severity: number
}

export interface AttentionItem {
  memberId: string
  fullName: string
  score: number
  reasons: AttentionReason[]
}

export interface Attention {
  items: AttentionItem[]
  /** Bank transfers waiting for the coach to confirm the money arrived. */
  pendingTransfers: OrderSummary[]
}
