import { useState } from 'react'

import type { PhotoPose, ProgressPhoto } from '@/types/member'

import { useUploadMyPhoto } from '@/api/photosApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { currentMonthIso, todayIso } from '@/lib/date'
import { PHOTO_POSES } from '@/types/member'

/** Mirrors `app.photos.max-size-bytes`; the server enforces it, this only says so earlier. */
const MAX_BYTES = 8 * 1024 * 1024
const ACCEPTED = 'image/jpeg,image/png,image/webp'

/**
 * The monthly set: five fixed poses. The member picks all five, presses Kaydet and confirms once —
 * nothing is sent before that, because a wrong photo cannot be swapped afterwards. Poses already stored
 * for this month are shown as done and not asked for again.
 */
export function PhotoUploadForm({ photos }: { photos: ProgressPhoto[] }) {
  const upload = useUploadMyPhoto()
  const month = currentMonthIso()
  const done = new Set(photos.filter((p) => p.takenOn.startsWith(month)).map((p) => p.pose))
  const missing = PHOTO_POSES.filter(({ value }) => !done.has(value))

  const [files, setFiles] = useState<Partial<Record<PhotoPose, File>>>({})
  const [errors, setErrors] = useState<Partial<Record<PhotoPose, string>>>({})
  const [confirming, setConfirming] = useState(false)
  const [failure, setFailure] = useState<string | null>(null)
  const [saved, setSaved] = useState(false)

  const pick = (pose: PhotoPose, file: File | null) => {
    setSaved(false)
    setFailure(null)
    if (file && file.size > MAX_BYTES) {
      setErrors((old) => ({ ...old, [pose]: 'Fotoğraf en fazla 8 MB olabilir.' }))
      setFiles((old) => ({ ...old, [pose]: undefined }))
      return
    }
    setErrors((old) => ({ ...old, [pose]: undefined }))
    setFiles((old) => ({ ...old, [pose]: file ?? undefined }))
  }

  const complete = missing.length > 0 && missing.every(({ value }) => files[value])

  const save = async () => {
    setFailure(null)
    const takenOn = todayIso()
    const stored: PhotoPose[] = []
    try {
      for (const { value } of missing) {
        await upload.mutateAsync({ file: files[value]!, takenOn, pose: value })
        stored.push(value)
      }
      setFiles({})
      setSaved(true)
    } catch (cause) {
      // Whatever went through is already kept; drop it from the pending list so a retry sends the rest.
      setFiles((old) => {
        const next = { ...old }
        stored.forEach((pose) => delete next[pose])
        return next
      })
      setFailure(cause instanceof Error ? cause.message : 'Fotoğraflar yüklenemedi.')
    } finally {
      setConfirming(false)
    }
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-5 sm:p-6">
      <div className="mb-5 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Aylık Fotoğraflar</h2>
        <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
          Her ay 5 fotoğraf istiyoruz: aynı ışıkta ve mesafede, aynı sırayla. Beşini de seçin, sonra Kaydet'e
          basın. Fotoğraflarınızı yalnızca siz ve koçunuz görebilir.
        </p>
      </div>

      {missing.length === 0 ? (
        <p className="rounded-xl border border-success/40 bg-success/5 p-4 text-sm text-success">
          Bu ayın 5 fotoğrafı yüklendi ✓ Sonraki set gelecek ay yüklenebilir.
        </p>
      ) : (
        <>
          <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
            {PHOTO_POSES.map(({ value, label }, index) => (
              <li
                key={value}
                className={`rounded-xl border p-3.5 ${done.has(value) ? 'border-success/40 bg-success/5' : 'border-border bg-surface-elevated'}`}
              >
                <p className="text-xs font-semibold text-text-primary">
                  {index + 1}. {label}
                </p>
                {done.has(value) ? (
                  <p className="mt-1 text-[11px] text-success">Bu ay yüklendi ✓</p>
                ) : (
                  <>
                    <input
                      type="file"
                      accept={ACCEPTED}
                      aria-label={`${label} fotoğrafı`}
                      onChange={(event) => pick(value, event.target.files?.[0] ?? null)}
                      className="mt-2 block w-full text-[11px] text-text-secondary file:mr-2 file:h-8 file:cursor-pointer file:rounded-lg file:border-0 file:bg-surface file:px-3 file:text-[11px] file:font-semibold file:uppercase file:tracking-wide file:text-text-primary"
                    />
                    {files[value] ? (
                      <p className="mt-1 truncate text-[11px] text-text-muted">{files[value]!.name}</p>
                    ) : null}
                    {errors[value] ? <p className="mt-1 text-[11px] text-error">{errors[value]}</p> : null}
                  </>
                )}
              </li>
            ))}
          </ul>

          <p className="mt-4 text-xs text-text-muted">
            {PHOTO_POSES.length - missing.length + missing.filter(({ value }) => files[value]).length} /{' '}
            {PHOTO_POSES.length} fotoğraf seçildi
          </p>
          {failure ? (
            <p role="alert" className="mt-3 rounded-xl border border-error/40 bg-error/10 p-3 text-xs text-error">
              {failure}
            </p>
          ) : null}
          {saved ? <p className="mt-3 text-xs text-success">Fotoğraflarınız kaydedildi.</p> : null}
          <Button type="button" className="mt-4" disabled={!complete || upload.isPending} onClick={() => setConfirming(true)}>
            Kaydet
          </Button>
        </>
      )}

      {confirming ? (
        <ConfirmDialog
          title="Fotoğraflar kaydedilsin mi?"
          description="Fotoğraflar ayda 1 kez yüklenebilir. Kaydettiğiniz fotoğraflar daha sonra değiştirilemez. Onaylıyor musunuz?"
          confirmLabel="Evet, Kaydet"
          isPending={upload.isPending}
          error={null}
          onCancel={() => setConfirming(false)}
          onConfirm={save}
        />
      ) : null}
    </section>
  )
}
