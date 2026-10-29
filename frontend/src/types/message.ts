import type { MemberStatus } from '@/types/member'

export type MessageSender = 'MEMBER' | 'COACH'

export interface Message {
  id: string
  sender: MessageSender
  body: string
  createdAt: string
  /** Set when the other side opened the thread; null means still unread by them. */
  readAt: string | null
}

/** One row of the coach's inbox. `lastMessageAt` is null for a thread nobody has started. */
export interface ConversationSummary {
  memberId: string
  fullName: string
  email: string
  status: MemberStatus
  unreadFromMember: number
  awaitingReply: boolean
  lastSender: MessageSender | null
  lastMessagePreview: string | null
  lastMessageAt: string | null
}

/** Badge feed; polled, so reading it never marks anything read. */
export interface UnreadSummary {
  unread: number
  lastMessageAt: string | null
}

/** A reply the coach saved to reuse. */
export interface MessageTemplate {
  id: string
  title: string
  body: string
  updatedAt: string
}
