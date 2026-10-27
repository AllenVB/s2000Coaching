import { useState } from 'react'

import { useCreateFormCheck, useDeleteFormCheck, useMyFormChecks } from '@/api/formCheckApi'
import { FormCheckVideo } from '@/components/training/FormCheckVideo'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { TrashIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'

/** Send the coach a clip of a lift to look at; the answer comes back on the same card. */
export function MemberFormCheckPage() {
  const { data: checks } = useMyFormChecks()
  const create = useCreateFormCheck()
  const remove = useDeleteFormCheck()

  const [exerciseName, setExerciseName] = useState('')
  const [videoUrl, setVideoUrl] = useState('')
  const [note, setNote] = useState('')

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    create.mutate(
      { exerciseName: exerciseName.trim(), videoUrl: videoUrl.trim(), note: note.trim() || null },
      {
        onSuccess: () => {
          setExerciseName('')
          setVideoUrl('')
          setNote('')
        },
      },
    )
  }

  return (
    <>
      <PageHeading
        title="Form Kontrolü"
        description="Bir hareketin kısa videosunu çekin, koçunuz izleyip yazılı geri bildirim versin."
      />

      <div className="space-y-6">
        <form className="space-y-4 rounded-2xl border border-border bg-surface p-6" onSubmit={submit}>
          <ol className="list-decimal space-y-1 pl-5 text-xs leading-relaxed text-text-secondary">
            <li>Hareketi yandan ya da önden, tüm vücudunuz görünecek şekilde 1–3 tekrar çekin.</li>
            <li>
              Videoyu YouTube'a <strong className="text-text-primary">"Listelenmemiş"</strong> (unlisted) olarak
              yükleyin — yalnızca bağlantıyı bilen izleyebilir.
            </li>
            <li>Bağlantıyı aşağıya yapıştırın.</li>
          </ol>

          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Hareket *">
              <input
                value={exerciseName}
                onChange={(event) => setExerciseName(event.target.value)}
                type="text"
                maxLength={120}
                placeholder="Örn: Squat"
                className={inputClasses}
              />
            </Field>
            <Field label="YouTube bağlantısı *">
              <input
                value={videoUrl}
                onChange={(event) => setVideoUrl(event.target.value)}
                type="text"
                inputMode="url"
                placeholder="https://youtu.be/…"
                className={inputClasses}
              />
            </Field>
          </div>
          <Field label="Not">
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="Neye baktırmak istiyorsunuz? Ağırlık, hangi tekrar, nerede zorlanıyorsunuz…"
              className={textareaClasses}
            />
          </Field>

          {create.isError ? <p className="text-xs text-error">{create.error.message}</p> : null}
          {create.isSuccess ? <p className="text-xs text-success">Videonuz koçunuza iletildi.</p> : null}

          <Button
            type="submit"
            disabled={create.isPending || exerciseName.trim() === '' || videoUrl.trim() === ''}
          >
            {create.isPending ? 'Gönderiliyor…' : 'Koça Gönder'}
          </Button>
        </form>

        {checks && checks.length > 0 ? (
          <section className="space-y-3">
            <h2 className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">Gönderdiklerim</h2>
            {checks.map((check) => (
              <article key={check.id} className="rounded-2xl border border-border bg-surface p-5">
                <div className="mb-3 flex items-start justify-between gap-3">
                  <div>
                    <p className="font-semibold text-text-primary">{check.exerciseName}</p>
                    <p className="text-xs text-text-muted">{formatDate(check.createdAt)}</p>
                  </div>
                  <button
                    type="button"
                    onClick={() => remove.mutate(check.id)}
                    aria-label={`${check.exerciseName} kaydını sil`}
                    className="text-text-muted transition-colors hover:text-error"
                  >
                    <TrashIcon className="h-4 w-4" />
                  </button>
                </div>
                <FormCheckVideo check={check} />
                {check.note ? <p className="mt-3 text-xs leading-relaxed text-text-secondary">{check.note}</p> : null}
                {check.coachReply ? (
                  <div className="mt-3 rounded-xl border border-primary/30 bg-primary/5 p-3.5">
                    <p className="mb-1 text-[11px] font-semibold uppercase tracking-wider text-primary">
                      Koçunuzun geri bildirimi
                    </p>
                    <p className="whitespace-pre-line text-sm leading-relaxed text-text-primary">{check.coachReply}</p>
                  </div>
                ) : (
                  <p className="mt-3 text-[11px] text-text-muted">Koçunuz henüz yanıtlamadı.</p>
                )}
              </article>
            ))}
          </section>
        ) : null}
      </div>
    </>
  )
}
