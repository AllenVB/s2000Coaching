import { useEffect, useRef, useState } from 'react'

import { Button } from '@/components/ui/Button'
import { textareaClasses } from '@/components/ui/FormField'
import { QuickReplyBar, type QuickReply } from '@/components/messages/QuickReplyBar'
import { formatDateTime } from '@/lib/date'
import type { Message, MessageSender } from '@/types/message'

const MAX_LENGTH = 4000

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
  initialDraft,
  quickReplies,
  onSaveQuickReply,
  onDeleteQuickReply,
}: {
  messages: Message[] | undefined
  viewpoint: MessageSender
  isLoading: boolean
  error?: Error | null
  onSend: (body: string) => void
  isSending: boolean
  sendError?: string | null
  emptyLabel: string
  /**
   * Pre-filled text, e.g. the question a shortcut card started. Read once, at mount: the
   * caller keys the thread on it so a second shortcut starts a fresh composer.
   */
  initialDraft?: string
  quickReplies?: QuickReply[]
  onSaveQuickReply?: (quickReply: { title: string; body: string }) => void
  onDeleteQuickReply?: (id: string) => void
}) {
  const [body, setBody] = useState(initialDraft ?? '')
  const endRef = useRef<HTMLDivElement>(null)
  const composerRef = useRef<HTMLTextAreaElement>(null)

  // Keep the newest message in view as the thread grows.
  useEffect(() => {
    endRef.current?.scrollIntoView({ block: 'nearest' })
  }, [messages])

  const send = () => {
    const trimmed = body.trim()
    if (trimmed.length === 0 || isSending) {
      return
    }
    onSend(trimmed)
    setBody('')
  }

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    send()
  }

  // Enter alone inserts a newline: the composer is multi-line and members type on phones.
  const onKeyDown = (event: React.KeyboardEvent<HTMLTextAreaElement>) => {
    if (event.key === 'Enter' && (event.ctrlKey || event.metaKey)) {
      event.preventDefault()
      send()
    }
  }

  const insert = (text: string) => {
    setBody((current) => (current.trim().length === 0 ? text : `${current.trimEnd()}\n\n${text}`))
    composerRef.current?.focus()
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
            {messages.map((message, index) => {
              const isMine = message.sender === viewpoint
              const daySeparator = dayLabelBefore(messages, index)
              return (
                <li key={message.id}>
                  {daySeparator ? (
                    <p className="my-3 text-center text-[10px] font-semibold uppercase tracking-widest text-text-muted">
                      {daySeparator}
                    </p>
                  ) : null}
                  <div className={`flex ${isMine ? 'justify-end' : 'justify-start'}`}>
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
                        {isMine ? (message.readAt === null ? ' · okunmadı' : ' · okundu') : ''}
                      </p>
                    </div>
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

      {quickReplies ? (
        <QuickReplyBar
          quickReplies={quickReplies}
          draft={body}
          onUse={insert}
          onSave={onSaveQuickReply}
          onDelete={onDeleteQuickReply}
        />
      ) : null}

      <form className="space-y-3" onSubmit={submit}>
        <textarea
          ref={composerRef}
          value={body}
          onChange={(event) => setBody(event.target.value)}
          onKeyDown={onKeyDown}
          rows={3}
          maxLength={MAX_LENGTH}
          placeholder="Mesajınızı yazın…"
          aria-label="Mesaj"
          className={textareaClasses}
        />
        {sendError ? <p className="text-xs text-error">{sendError}</p> : null}
        <div className="flex flex-wrap items-center gap-3">
          <Button type="submit" disabled={isSending || body.trim().length === 0}>
            {isSending ? 'Gönderiliyor…' : 'Gönder'}
          </Button>
          <span className="text-[11px] text-text-muted">
            Ctrl + Enter ile gönder
            {body.length > MAX_LENGTH - 500 ? ` · ${MAX_LENGTH - body.length} karakter kaldı` : ''}
          </span>
        </div>
      </form>
    </div>
  )
}

/** The date heading shown above a message when it is the first one of its day. */
function dayLabelBefore(messages: Message[], index: number): string | null {
  const day = new Date(messages[index].createdAt).toDateString()
  if (index > 0 && new Date(messages[index - 1].createdAt).toDateString() === day) {
    return null
  }
  const today = new Date()
  const yesterday = new Date(today)
  yesterday.setDate(today.getDate() - 1)
  if (day === today.toDateString()) {
    return 'Bugün'
  }
  if (day === yesterday.toDateString()) {
    return 'Dün'
  }
  return new Date(messages[index].createdAt).toLocaleDateString('tr-TR', {
    day: '2-digit',
    month: 'long',
    year: 'numeric',
  })
}
