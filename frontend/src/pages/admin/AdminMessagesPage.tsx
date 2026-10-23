import { useEffect, useMemo, useState } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { Link, useSearchParams } from 'react-router-dom'

import {
  useConversations,
  useCreateMessageTemplate,
  useDeleteMessageTemplate,
  useMemberMessages,
  useMessageTemplates,
  useSendCoachMessage,
} from '@/api/messagesApi'
import { MessageThread } from '@/components/messages/MessageThread'
import { Button } from '@/components/ui/Button'
import { inputClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatDateTime } from '@/lib/date'
import type { ConversationSummary } from '@/types/message'

/**
 * The coach's message centre: every thread in one list, the ones waiting on a reply first,
 * so answering members no longer means opening member pages one by one.
 */
export function AdminMessagesPage() {
  const [searchParams, setSearchParams] = useSearchParams()
  const selectedId = searchParams.get('uye')
  const [search, setSearch] = useState('')

  const { data: conversations, isLoading, error } = useConversations()
  useSignOutOn401(error)

  const visible = useMemo(() => {
    if (!conversations) {
      return []
    }
    const term = search.trim().toLocaleLowerCase('tr')
    if (term.length === 0) {
      return conversations
    }
    return conversations.filter(
      (conversation) =>
        conversation.fullName.toLocaleLowerCase('tr').includes(term) ||
        conversation.email.toLocaleLowerCase('tr').includes(term),
    )
  }, [conversations, search])

  const selected = conversations?.find((conversation) => conversation.memberId === selectedId)
  const waiting = conversations?.filter((conversation) => conversation.awaitingReply).length ?? 0

  const select = (memberId: string) => {
    setSearchParams(memberId ? { uye: memberId } : {}, { replace: true })
  }

  return (
    <>
      <PageHeading
        title="Mesajlar"
        description={
          waiting > 0
            ? `${waiting} üye yanıtını bekliyor.`
            : 'Tüm yazışmalar tek yerde. Yanıt bekleyen üye yok.'
        }
      />

      {error ? (
        <p className="mb-6 rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Yazışmalar yüklenemedi: {error.message}
        </p>
      ) : null}

      <div className="grid gap-6 lg:grid-cols-[320px_minmax(0,1fr)]">
        <section className={`${selectedId ? 'hidden lg:block' : ''}`}>
          <input
            value={search}
            onChange={(event) => setSearch(event.target.value)}
            placeholder="Üye ara"
            aria-label="Üye ara"
            className={inputClasses}
          />
          {isLoading ? (
            <div className="mt-4 h-64 animate-pulse rounded-2xl bg-surface-elevated" />
          ) : visible.length === 0 ? (
            <p className="mt-4 text-sm italic text-text-muted">Eşleşen üye yok.</p>
          ) : (
            <ul className="mt-4 max-h-[560px] space-y-2 overflow-y-auto pr-1">
              {visible.map((conversation) => (
                <li key={conversation.memberId}>
                  <ConversationRow
                    conversation={conversation}
                    isSelected={conversation.memberId === selectedId}
                    onSelect={() => select(conversation.memberId)}
                  />
                </li>
              ))}
            </ul>
          )}
        </section>

        <section className={`${selectedId ? '' : 'hidden lg:block'} min-w-0`}>
          {selected ? (
            <ConversationPanel conversation={selected} onBack={() => select('')} />
          ) : (
            <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
              Soldaki listeden bir üye seçin.
            </p>
          )}
        </section>
      </div>
    </>
  )
}

function ConversationRow({
  conversation,
  isSelected,
  onSelect,
}: {
  conversation: ConversationSummary
  isSelected: boolean
  onSelect: () => void
}) {
  return (
    <button
      type="button"
      onClick={onSelect}
      className={`w-full rounded-2xl border p-3.5 text-left transition-colors ${
        isSelected
          ? 'border-primary/60 bg-surface-elevated'
          : 'border-border bg-surface hover:border-white/25'
      }`}
    >
      <div className="flex items-start justify-between gap-2">
        <span className="truncate text-sm font-semibold text-text-primary">{conversation.fullName}</span>
        {conversation.unreadFromMember > 0 ? (
          <span className="shrink-0 rounded-full bg-primary px-2 py-0.5 text-[10px] font-bold text-canvas">
            {conversation.unreadFromMember}
          </span>
        ) : null}
      </div>
      <p className="mt-1 truncate text-xs text-text-muted">
        {conversation.lastMessagePreview
          ? `${conversation.lastSender === 'COACH' ? 'Sen: ' : ''}${conversation.lastMessagePreview}`
          : 'Henüz yazışma yok'}
      </p>
      {conversation.lastMessageAt ? (
        <p className="mt-1 text-[10px] uppercase tracking-wider text-text-muted">
          {formatDateTime(conversation.lastMessageAt)}
        </p>
      ) : null}
    </button>
  )
}

/** The selected thread, with the coach's saved replies attached to the composer. */
function ConversationPanel({
  conversation,
  onBack,
}: {
  conversation: ConversationSummary
  onBack: () => void
}) {
  const queryClient = useQueryClient()
  const memberId = conversation.memberId
  const { data: messages, isLoading, error, isSuccess } = useMemberMessages(memberId)
  const sendMessage = useSendCoachMessage(memberId)
  const { data: templates } = useMessageTemplates()
  const createTemplate = useCreateMessageTemplate()
  const deleteTemplate = useDeleteMessageTemplate()

  // Loading the thread marked the member's messages read server-side, so the list and the
  // nav badge are now stale.
  useEffect(() => {
    if (isSuccess) {
      queryClient.invalidateQueries({ queryKey: ['admin-message-threads'] })
      queryClient.invalidateQueries({ queryKey: ['admin-messages-unread'] })
      queryClient.invalidateQueries({ queryKey: ['admin-dashboard'] })
    }
  }, [isSuccess, memberId, queryClient])

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <div className="min-w-0">
          <h2 className="truncate text-lg font-semibold text-text-primary">{conversation.fullName}</h2>
          <p className="truncate text-xs text-text-muted">{conversation.email}</p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to={`/yonetim/uyeler/${memberId}`}
            className="rounded-full border border-border px-3.5 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-white/25 hover:text-text-primary"
          >
            Üye kartı
          </Link>
          <Button type="button" size="sm" variant="ghost" className="lg:hidden" onClick={onBack}>
            Listeye dön
          </Button>
        </div>
      </div>

      <MessageThread
        messages={messages}
        viewpoint="COACH"
        isLoading={isLoading}
        error={error}
        onSend={(body) => sendMessage.mutate({ body })}
        isSending={sendMessage.isPending}
        sendError={sendMessage.isError ? sendMessage.error.message : null}
        emptyLabel="Bu üyeyle henüz yazışma yok. İlk mesajı siz yazabilirsiniz."
        quickReplies={templates ?? []}
        onSaveQuickReply={(quickReply) => createTemplate.mutate(quickReply)}
        onDeleteQuickReply={(id) => deleteTemplate.mutate({ templateId: id })}
      />
      {createTemplate.isError ? (
        <p className="text-xs text-error">Hazır yanıt kaydedilemedi: {createTemplate.error.message}</p>
      ) : null}
    </div>
  )
}
