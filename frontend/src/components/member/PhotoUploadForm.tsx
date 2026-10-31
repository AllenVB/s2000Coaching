import { useRef, useState } from 'react'

import type { PhotoPose, ProgressPhoto } from '@/types/member'

import { useUploadMyPhoto } from '@/api/photosApi'
import { Field, inputClasses } from '@/components/ui/FormField'
import { todayIso } from '@/lib/date'
import { PHOTO_POSES } from '@/types/member'

/** Mirrors `app.photos.max-size-bytes`; the server enforces it, this only says so earlier. */
const MAX_BYTES = 8 * 1024 * 1024
const ACCEPTED = 'image/jpeg,image/png,image/webp'

/**
 * The monthly set: five fixed poses, one photo each. A pose already uploaded for the chosen
 * month shows as done instead of offering a second upload, which the server would refuse.
 */
export function PhotoUploadForm({ photos }: { photos: ProgressPhoto[] }) {
  const [month, setMonth] = useState(() => todayIso().slice(0, 7))
  const done = new Set(photos.filter((p) => p.takenOn.startsWith(month)).map((p) => p.pose))

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-5 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Aylık Fotoğraflar</h2>
        <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
          Her ay 5 fotoğraf istiyoruz: aynı ışıkta ve mesafede, aynı sırayla. Böylece gelişim aydan aya
          gerçekten karşılaştırılabilir. Fotoğraflarınızı yalnızca siz ve koçunuz görebilir.
        </p>
      </div>

      <div className="mb-5 max-w-[12rem]">
        <Field label="Ay">
          <input
            type="month"
            value={month}
            max={todayIso().slice(0, 7)}
            onChange={(event) => event.target.value && setMonth(event.target.value)}
            className={inputClasses}
          />
        </Field>
      </div>

      <p className="mb-3 text-xs font-medium text-text-secondary">
        {done.size} / {PHOTO_POSES.length} fotoğraf tamam
      </p>
      <ul className="grid grid-cols-1 gap-3 sm:grid-cols-2">
        {PHOTO_POSES.map(({ value, label }, index) => (
          <PoseSlot key={`${month}-${value}`} index={index + 1} pose={value} label={label} month={month}
            done={done.has(value)} />
        ))}
      </ul>
    </section>
  )
}

function PoseSlot({ index, pose, label, month, done }: {
  index: number
  pose: PhotoPose
  label: string
  month: string
  done: boolean
}) {
  const upload = useUploadMyPhoto()
  const input = useRef<HTMLInputElement>(null)
  const [localError, setLocalError] = useState<string | null>(null)

  // The photo is dated today when the month is the current one, otherwise the first of the month.
  const today = todayIso()
  const takenOn = month === today.slice(0, 7) ? today : `${month}-01`

  const pick = (file: File | null) => {
    setLocalError(null)
    if (!file) {
      return
    }
    if (file.size > MAX_BYTES) {
      setLocalError('Fotoğraf en fazla 8 MB olabilir.')
      return
    }
    upload.mutate({ file, takenOn, pose }, {
      onSettled: () => {
        if (input.current) {
          input.current.value = ''
        }
      },
    })
  }

  return (
    <li className={`rounded-xl border p-3.5 ${done ? 'border-success/40 bg-success/5' : 'border-border bg-surface-elevated'}`}>
      <p className="text-xs font-semibold text-text-primary">
        {index}. {label}
      </p>
      {done ? (
        <p className="mt-1 text-[11px] text-success">Yüklendi ✓</p>
      ) : (
        <>
          <input
            ref={input}
            type="file"
            accept={ACCEPTED}
            aria-label={`${label} fotoğrafı`}
            disabled={upload.isPending}
            onChange={(event) => pick(event.target.files?.[0] ?? null)}
            className="mt-2 block w-full text-[11px] text-text-secondary file:mr-2 file:h-8 file:cursor-pointer file:rounded-lg file:border-0 file:bg-surface file:px-3 file:text-[11px] file:font-semibold file:uppercase file:tracking-wide file:text-text-primary"
          />
          {upload.isPending ? <p className="mt-1 text-[11px] text-text-muted">Yükleniyor…</p> : null}
          {localError || upload.isError ? (
            <p className="mt-1 text-[11px] text-error">{localError ?? upload.error?.message}</p>
          ) : null}
        </>
      )}
    </li>
  )
}

