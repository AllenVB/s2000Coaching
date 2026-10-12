import { useState } from 'react'

import { useAddCoachNote, useCoachNotes, useDeleteCoachNote } from '@/api/adminMembersApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { textareaClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'
import { formatDateTime } from '@/lib/date'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'

/** Coach-only notes; there is no member-facing endpoint for this data. */
export function MemberNotesTab() {
  const { member } = useMemberDetail()
  const { data: notes, isLoading, isError, error } = useCoachNotes(member.id)
  const addNote = useAddCoachNote(member.id)
  const deleteNote = useDeleteCoachNote(member.id)

  const [body, setBody] = useState('')
  const [deletingNoteId, setDeletingNoteId] = useState<string | null>(null)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = body.trim()
    if (trimmed.length === 0) {
      return
    }
    addNote.mutate({ body: trimmed }, { onSuccess: () => setBody('') })
  }

  return (
    <div className="space-y-6">
      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-2 text-sm font-bold uppercase tracking-wider text-text-primary">Not Ekle</h2>
        <p className="mb-4 text-xs text-text-secondary">
          Bu notları yalnızca siz görürsünüz; üye alanında görünmez.
        </p>
        <form className="space-y-3" onSubmit={submit}>
          <textarea
            value={body}
            onChange={(event) => setBody(event.target.value)}
            rows={3}
            maxLength={5000}
            placeholder="Görüşme notu, sakatlık durumu, hedef değişikliği…"
            aria-label="Not"
            className={textareaClasses}
          />
          {addNote.isError ? <p className="text-xs text-error">{addNote.error.message}</p> : null}
          <Button type="submit" disabled={addNote.isPending || body.trim().length === 0}>
            {addNote.isPending ? 'Ekleniyor…' : 'Notu Ekle'}
          </Button>
        </form>
      </section>

      {isLoading ? (
        <div className="h-24 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Notlar yüklenemedi: {error.message}
        </p>
      ) : notes && notes.length > 0 ? (
        <ul className="space-y-3">
          {notes.map((note) => (
            <li key={note.id} className="rounded-2xl border border-border bg-surface p-5">
              <div className="mb-2 flex items-start justify-between gap-4">
                <span className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">
                  {formatDateTime(note.createdAt)}
                </span>
                <button
                  type="button"
                  onClick={() => setDeletingNoteId(note.id)}
                  aria-label="Notu sil"
                  className="text-text-muted transition-colors hover:text-error"
                >
                  <TrashIcon className="h-4 w-4" />
                </button>
              </div>
              <p className="whitespace-pre-line text-sm leading-relaxed text-text-secondary">{note.body}</p>
            </li>
          ))}
        </ul>
      ) : (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          Bu üye için henüz not eklenmedi.
        </p>
      )}

      {deletingNoteId ? (
        <ConfirmDialog
          title="Notu Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteNote.isPending}
          error={deleteNote.isError ? deleteNote.error.message : null}
          description="Bu notu silmek istediğinize emin misiniz?"
          onCancel={() => setDeletingNoteId(null)}
          onConfirm={() =>
            deleteNote.mutate({ noteId: deletingNoteId }, { onSuccess: () => setDeletingNoteId(null) })
          }
        />
      ) : null}
    </div>
  )
}
