import { useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import { useMyMessages, useSendMyMessage, useMyUnreadMessages } from '@/api/messagesApi'
import { FormCheckPanel } from '@/components/messages/FormCheckPanel'
import { MessageThread } from '@/components/messages/MessageThread'
import { PageHeading } from '@/components/ui/PageHeading'
import { SectionTabs, tabFromParam } from '@/components/ui/SectionTabs'

const TABS = [
  { value: 'mesaj', label: 'Mesaj At' },
  { value: 'form', label: 'Hareket Formu Gönder' },
] as const

/** Quick starts: one tap fills the composer with the opening of a question, which is how a first message gets written. */
const TOPICS = [
  { label: 'Antrenman sorusu', draft: 'Antrenmanla ilgili bir sorum var: ' },
  { label: 'Beslenme sorusu', draft: 'Beslenmeyle ilgili bir sorum var: ' },
  { label: 'İstek / şikayet', draft: 'Programla ilgili isteğim / şikayetim: ' },
  { label: 'Üyelik / ödeme', draft: 'Üyeliğimle ilgili bir sorum var: ' },
]

/**
 * "Mesajlar": a conversation with the coach, and — one tab over — the place to send a link to a video of
 * a lift for the coach to look at and answer.
 */
export function MemberMessagesPage() {
  const [params, setParams] = useSearchParams()
  const tab = tabFromParam(params.get('sekme'), [...TABS])
  // The "Koçuna sor" shortcuts arrive with their question in the address.
  const [draft, setDraft] = useState(params.get('soru') ?? '')
  const { data: unread } = useMyUnreadMessages()

  const { data: messages, isLoading, error } = useMyMessages()
  const sendMessage = useSendMyMessage()

  return (
    <>
      <PageHeading
        title="Mesajlar"
        description="Koçunuza soru sorun, isteğinizi iletin ya da bir hareketinizin videosunu gönderip geri bildirim alın."
      />

      <section className="mb-6 flex items-center gap-4 rounded-2xl border border-primary/30 bg-primary/5 p-5">
        <span className="flex h-12 w-12 shrink-0 items-center justify-center rounded-full bg-primary font-display text-lg font-black text-canvas" aria-hidden>
          K
        </span>
        <div className="min-w-0">
          <p className="font-semibold text-text-primary">Koçunuz</p>
          <p className="text-xs leading-relaxed text-text-secondary">
            {unread && unread.unread > 0
              ? `${unread.unread} yeni yanıtınız var.`
              : 'Yazdıklarınızı koçunuz okuyup buradan yanıtlar.'}
          </p>
        </div>
      </section>

      <SectionTabs label="Mesaj türü" tabs={[...TABS]} value={tab} onChange={(next) => setParams({ sekme: next }, { replace: true })} />

      {tab === 'mesaj' ? (
        <>
          <div className="mb-4 flex flex-wrap gap-2">
            {TOPICS.map((topic) => (
              <button
                key={topic.label}
                type="button"
                onClick={() => setDraft(topic.draft)}
                className="rounded-full border border-border-strong bg-surface-elevated px-3.5 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-primary/60 hover:text-text-primary"
              >
                {topic.label}
              </button>
            ))}
          </div>
          <MessageThread
            // A topic chip or a shortcut replaces the untouched draft with a fresh composer.
            key={draft}
            messages={messages}
            viewpoint="MEMBER"
            isLoading={isLoading}
            error={error}
            onSend={(body) => sendMessage.mutate({ body })}
            isSending={sendMessage.isPending}
            sendError={sendMessage.isError ? sendMessage.error.message : null}
            emptyLabel="Henüz mesaj yok. İlk mesajı siz yazabilirsiniz; yukarıdaki konulardan birine dokunmak başlamayı kolaylaştırır."
            initialDraft={draft || undefined}
          />
        </>
      ) : (
        <>
          <FormCheckPanel />
        </>
      )}
    </>
  )
}
