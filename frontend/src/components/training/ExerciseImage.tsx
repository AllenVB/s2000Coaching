import { useMemo, useRef, useState } from 'react'

import { useAuthedImage } from '@/api/weeklyNutritionApi'
import { useRemoveExerciseImage, useSetExerciseImage } from '@/api/trainingApi'
import { useAdminAuth } from '@/context/AdminAuthContext'
import { useMemberAuth } from '@/context/MemberAuthContext'
import { adminHeaders } from '@/lib/adminSession'
import { memberHeaders } from '@/lib/memberSession'
import type { Exercise } from '@/types/training'

/** Mirrors `app.photos.max-size-bytes`; the server enforces it, this only says so earlier. */
const MAX_BYTES = 8 * 1024 * 1024

/** The version in the address keeps a browser from showing the old picture after it was replaced. */
function imagePath(base: string, exercise: Exercise): string {
  return `${base}/${exercise.id}/image?v=${encodeURIComponent(exercise.imageUpdatedAt ?? '')}`
}

function Picture({ path, headers, name }: { path: string; headers: Record<string, string> | null; name: string }) {
  const { url, error } = useAuthedImage(path, headers)

  if (error) {
    return <p className="rounded-lg border border-error/40 bg-error/10 p-3 text-[11px] text-error">Görsel yüklenemedi: {error.message}</p>
  }
  if (!url) {
    return <div className="h-48 animate-pulse rounded-xl bg-surface-elevated" />
  }
  return <img src={url} alt={`${name} hareketinin görseli`} className="max-h-[420px] w-full rounded-xl border border-border bg-canvas object-contain" />
}

/** The demonstration image at the start of an exercise on the member's page; nothing when there is none. */
export function MemberExerciseImage({ exercise }: { exercise: Exercise }) {
  const { session } = useMemberAuth()
  const headers = useMemo(() => (session ? memberHeaders(session.token) : null), [session])

  if (!exercise.hasImage) {
    return null
  }
  return (
    <div className="mb-3">
      <Picture path={imagePath('/api/member/exercises', exercise)} headers={headers} name={exercise.name} />
    </div>
  )
}

/** Where the coach uploads, replaces or removes an exercise's image. The image belongs to the exercise, so every plan that uses it shows it. */
export function ExerciseImageEditor({ exercise }: { exercise: Exercise }) {
  const { token } = useAdminAuth()
  const headers = useMemo(() => (token ? adminHeaders(token) : null), [token])
  const setImage = useSetExerciseImage()
  const removeImage = useRemoveExerciseImage()
  const fileInput = useRef<HTMLInputElement>(null)
  const [localError, setLocalError] = useState<string | null>(null)

  const pick = (file: File | null) => {
    setLocalError(null)
    if (!file) {
      return
    }
    if (file.size > MAX_BYTES) {
      setLocalError('Görsel en fazla 8 MB olabilir.')
    } else {
      setImage.mutate({ exerciseId: exercise.id, file })
    }
    if (fileInput.current) {
      fileInput.current.value = ''
    }
  }

  const error = localError ?? (setImage.isError ? setImage.error.message : null) ?? (removeImage.isError ? removeImage.error.message : null)

  return (
    <div className="mt-3 space-y-2 border-t border-border/60 pt-3">
      <span className="block text-[11px] font-semibold uppercase tracking-wider text-text-muted">Görsel</span>
      {exercise.hasImage ? (
        <div className="max-w-sm">
          <Picture path={imagePath('/api/admin/exercises', exercise)} headers={headers} name={exercise.name} />
        </div>
      ) : (
        <p className="text-xs italic text-text-muted">Bu hareketin görseli yok.</p>
      )}
      <div className="flex flex-wrap items-center gap-3">
        <label className="inline-flex h-8 cursor-pointer items-center rounded-lg border border-border-strong bg-surface-elevated px-3 text-xs font-semibold uppercase tracking-wide text-text-primary transition-colors hover:border-primary/60">
          {setImage.isPending ? 'Yükleniyor…' : exercise.hasImage ? 'Görseli Değiştir' : 'Görsel Yükle'}
          <input
            ref={fileInput}
            type="file"
            accept="image/png,image/jpeg"
            className="sr-only"
            disabled={setImage.isPending}
            aria-label={`${exercise.name} görseli`}
            onChange={(event) => pick(event.target.files?.[0] ?? null)}
          />
        </label>
        {exercise.hasImage ? (
          <button
            type="button"
            onClick={() => removeImage.mutate({ exerciseId: exercise.id })}
            disabled={removeImage.isPending}
            className="text-xs font-semibold text-text-muted transition-colors hover:text-error disabled:opacity-50"
          >
            Görseli kaldır
          </button>
        ) : null}
        <span className="text-[11px] text-text-muted">PNG ya da JPEG · en fazla 8 MB</span>
      </div>
      {error ? (
        <p role="alert" className="text-xs text-error">
          {error}
        </p>
      ) : null}
    </div>
  )
}
