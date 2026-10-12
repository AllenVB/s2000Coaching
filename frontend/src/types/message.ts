export type MessageSender = 'MEMBER' | 'COACH'

export interface Message {
  id: string
  sender: MessageSender
  body: string
  createdAt: string
  /** Set when the other side opened the thread; null means still unread by them. */
  readAt: string | null
}
