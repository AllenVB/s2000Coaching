import { useState } from 'react'

import { Button } from '@/components/ui/Button'
import { inputClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'

export interface QuickReply {
  id: string
  title: string
  body: string
}

/**
 * Chips above the composer. A chip appends its text to the draft rather than replacing it,
 * so the coach can stack a greeting and a reminder and then edit the result before sending.
 */
export function QuickReplyBar({
  quickReplies,
  draft,
  onUse,
  onSave,
  onDelete,
}: {
  quickReplies: QuickReply[]
  draft: string
  onUse: (body: string) => void
  /** Omitted where the viewer cannot manage the list — then the bar is read-only. */
  onSave?: (quickReply: { title: string; body: string }) => void
  onDelete?: (id: string) => void
}) {
  const [title, setTitle] = useState('')
  const [isNaming, setIsNaming] = useState(false)

  const canSaveDraft = Boolean(onSave) && draft.trim().length > 0

  const save = () => {
    const trimmed = title.trim()
    if (!onSave || trimmed.length === 0 || draft.trim().length === 0) {
      return
    }
    onSave({ title: trimmed, body: draft.trim() })
    setTitle('')
    setIsNaming(false)
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-4">
      <div className="flex flex-wrap items-center justify-between gap-2">
        <h3 className="text-[11px] font-semibold uppercase tracking-widest text-text-muted">
          Hazır yanıtlar
        </h3>
        {canSaveDraft && !isNaming ? (
          <button
            type="button"
            onClick={() => setIsNaming(true)}
            className="text-[11px] font-semibold uppercase tracking-wide text-primary hover:underline"
          >
            Yazdığını kaydet
          </button>
        ) : null}
      </div>

      {quickReplies.length === 0 ? (
        <p className="mt-3 text-xs italic text-text-muted">
          Henüz hazır yanıt yok. Sık yazdığın bir mesajı yazıp “Yazdığını kaydet” ile ekleyebilirsin.
        </p>
      ) : (
        <ul className="mt-3 flex flex-wrap gap-2">
          {quickReplies.map((quickReply) => (
            <li key={quickReply.id} className="flex items-center overflow-hidden rounded-full border border-border bg-surface-elevated">
              <button
                type="button"
                onClick={() => onUse(quickReply.body)}
                title={quickReply.body}
                className="px-3.5 py-1.5 text-xs font-semibold text-text-secondary transition-colors hover:text-text-primary"
              >
                {quickReply.title}
              </button>
              {onDelete ? (
                <button
                  type="button"
                  onClick={() => onDelete(quickReply.id)}
                  aria-label={`${quickReply.title} hazır yanıtını sil`}
                  className="border-l border-border px-2 py-1.5 text-text-muted transition-colors hover:text-error"
                >
                  <TrashIcon className="h-3.5 w-3.5" />
                </button>
              ) : null}
            </li>
          ))}
        </ul>
      )}

      {isNaming ? (
        <div className="mt-3 flex flex-wrap items-center gap-2">
          <input
            value={title}
            onChange={(event) => setTitle(event.target.value)}
            maxLength={80}
            placeholder="Kısa başlık, örn. Ölçüm hatırlatması"
            aria-label="Hazır yanıt başlığı"
            className={`${inputClasses} max-w-xs`}
          />
          <Button type="button" size="sm" onClick={save} disabled={title.trim().length === 0}>
            Kaydet
          </Button>
          <Button
            type="button"
            size="sm"
            variant="ghost"
            onClick={() => {
              setIsNaming(false)
              setTitle('')
            }}
          >
            Vazgeç
          </Button>
        </div>
      ) : null}
    </section>
  )
}
