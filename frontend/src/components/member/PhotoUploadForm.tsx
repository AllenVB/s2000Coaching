import { useRef, useState } from 'react'

import type { PhotoPose } from '@/types/member'

import { useUploadMyPhoto } from '@/api/photosApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { PHOTO_POSES } from '@/types/member'
import { todayIso } from '@/lib/date'

/** Mirrors `app.photos.max-size-bytes`; the server enforces it, this only says so earlier. */
const MAX_BYTES = 8 * 1024 * 1024
const ACCEPTED = 'image/jpeg,image/png,image/webp'

export function PhotoUploadForm() {
  const uploadPhoto = useUploadMyPhoto()
  const fileInput = useRef<HTMLInputElement>(null)
  const [file, setFile] = useState<File | null>(null)
  const [takenOn, setTakenOn] = useState(todayIso)
  const [pose, setPose] = useState<PhotoPose>('ON')
  const [note, setNote] = useState('')
  const [localError, setLocalError] = useState<string | null>(null)

  const today = todayIso()

  const pickFile = (picked: File | null) => {
    setLocalError(null)
    if (picked && picked.size > MAX_BYTES) {
      setLocalError('Fotoğraf en fazla 8 MB olabilir.')
      setFile(null)
      return
    }
    setFile(picked)
  }

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (!file) {
      setLocalError('Önce bir fotoğraf seçin.')
      return
    }
    uploadPhoto.mutate(
      { file, takenOn, pose, note: note.trim() || undefined },
      {
        onSuccess: () => {
          setFile(null)
          setNote('')
          if (fileInput.current) {
            fileInput.current.value = ''
          }
        },
      },
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-5 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
          Fotoğraf Ekle
        </h2>
        <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
          Aynı duruşta, benzer ışıkta ve mesafede çekilen fotoğraflar karşılaştırıldığında
          gelişim gerçekten görünür olur. Fotoğraflarınızı yalnızca siz ve koçunuz görebilir.
        </p>
      </div>

      <form className="space-y-4" onSubmit={submit}>
        <Field label="Fotoğraf *" hint="JPEG, PNG veya WebP · en fazla 8 MB">
          <input
            ref={fileInput}
            type="file"
            accept={ACCEPTED}
            onChange={(event) => pickFile(event.target.files?.[0] ?? null)}
            className="block w-full text-xs text-text-secondary file:mr-3 file:h-9 file:cursor-pointer file:rounded-lg file:border-0 file:bg-surface-elevated file:px-3.5 file:text-xs file:font-semibold file:uppercase file:tracking-wide file:text-text-primary"
          />
        </Field>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
          <Field label="Çekim Tarihi *">
            <input
              type="date"
              value={takenOn}
              max={today}
              onChange={(event) => setTakenOn(event.target.value)}
              className={inputClasses}
            />
          </Field>

          <Field label="Duruş *">
            <select
              value={pose}
              onChange={(event) => setPose(event.target.value as PhotoPose)}
              className={inputClasses}
            >
              {PHOTO_POSES.map(({ value, label }) => (
                <option key={value} value={value}>
                  {label}
                </option>
              ))}
            </select>
          </Field>
        </div>

        <Field label="Not">
          <textarea
            value={note}
            onChange={(event) => setNote(event.target.value)}
            placeholder="İsterseniz bu fotoğrafla ilgili bir not bırakın."
            className={textareaClasses}
          />
        </Field>

        {localError || uploadPhoto.isError ? (
          <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
            {localError ?? uploadPhoto.error?.message}
          </p>
        ) : null}

        <Button type="submit" disabled={uploadPhoto.isPending}>
          {uploadPhoto.isPending ? 'Yükleniyor…' : 'Fotoğrafı Yükle'}
        </Button>
      </form>
    </section>
  )
}
