import { useEffect, useRef, useState } from 'react'

import { Button } from '@/components/ui/Button'
import { textareaClasses } from '@/components/ui/FormField'
import { formatDateTime } from '@/lib/date'
import type { Message, MessageSender } from '@/types/message'

/**
 * The thread, shared by both portals. `viewpoint` decides which side is "mine" and so which
 * bubbles sit on the right.
 */
export function MessageThread({
  messages,
  viewpoint,
  isLoading,
  error,
  onSend,
  isSending,
  sendError,
  emptyLabel,
}: {
  messages: Message[] | undefined
  viewpoint: MessageSender
  isLoading: boolean
  error?: Error | null
  onSend: (body: string) => void
  isSending: boolean
  sendError?: string | null
  emptyLabel: string
}) {
  const [body, setBody] = useState('')
  const endRef = useRef<HTMLDivElement>(null)

  // Keep the newest message in view as the thread grows.
  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'nearest' })
  }, [messages])

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = body.trim()
    if (trimmed.length === 0) {
      return
    }
    onSend(trimmed)
    setBody('')
  }

  return (
    <div className="space-y-4">
      <section className="rounded-2xl border border-border bg-surface p-5">
        {isLoading ? (
          <div className="h-32 animate-pulse rounded-xl bg-surface-elevated" />
        ) : error ? (
          <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
            Mesajlar yüklenemedi: {error.message}
          </p>
        ) : messages && messages.length > 0 ? (
          <ul className="max-h-[420px] space-y-3 overflow-y-auto pr-1">
            {messages.map((message) => {
              const isMine = message.sender === viewpoint
              return (
                <li key={message.id} className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
                  <div
                    className={`max-w-[80%] rounded-2xl px-4 py-3 ${
                      isMine
                        ? 'bg-primary/15 text-text-primary'
                        : 'border border-border bg-surface-elevated text-text-primary'
                    }`}
                  >
                    <p className="whitespace-pre-line text-sm leading-relaxed">{message.body}</p>
                    <p className="mt-1.5 text-[10px] uppercase tracking-wider text-text-muted">
                      {message.sender === 'COACH' ? 'Koç' : 'Üye'} · {formatDateTime(message.createdAt)}
                      {isMine && message.readAt === null ? ' · okunmadı' : ''}
                    </p>
                  </div>
                </li>
              )
            })}
            <div ref={endRef} />
          </ul>
        ) : (
          <p className="text-sm italic text-text-muted">{emptyLabel}</p>
        )}
      </section>

      <form className="space-y-3" onSubmit={submit}>
        <textarea
          value={body}
          onChange={(event) => setBody(event.target.value)}
          rows={3}
          maxLength={4000}
          placeholder="Mesajınızı yazın…"
          aria-label="Mesaj"
          className={textareaClasses}
        />
        {sendError ? <p className="text-xs text-error">{sendError}</p> : null}
        <Button type="submit" disabled={isSending || body.trim().length === 0}>
          {isSending ? 'Gönderiliyor…' : 'Gönder'}
        </Button>
      </form>
    </div>
  )
}
