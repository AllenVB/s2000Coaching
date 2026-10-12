import { useMemo, useState } from 'react'

import type { PhotoPose, ProgressPhoto } from '@/types/member'

import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { Field, inputClasses } from '@/components/ui/FormField'
import { formatDate } from '@/lib/date'
import { PHOTO_POSES } from '@/types/member'

/**
 * Two photos of the same pose, side by side. A front shot next to a back shot says nothing
 * about progress, so the pose is picked first and the two dates come from within it.
 */
export function PhotoComparison({ photos }: { photos: ProgressPhoto[] }) {
  const posesWithEnough = useMemo(
    () => PHOTO_POSES.filter(({ value }) => photos.filter((p) => p.pose === value).length >= 2),
    [photos],
  )
  const [pose, setPose] = useState<PhotoPose | null>(null)

  const activePose = pose && posesWithEnough.some((p) => p.value === pose)
    ? pose
    : posesWithEnough[0]?.value ?? null

  // Oldest first, so "before" is on the left where it reads.
  const ofPose = useMemo(
    () =>
      photos
        .filter((photo) => photo.pose === activePose)
        .sort((a, b) => a.takenOn.localeCompare(b.takenOn)),
    [photos, activePose],
  )

  const [beforeId, setBeforeId] = useState<string | null>(null)
  const [afterId, setAfterId] = useState<string | null>(null)

  // Default to the widest span available: the first and the last of this pose.
  const before = ofPose.find((p) => p.id === beforeId) ?? ofPose[0]
  const after = ofPose.find((p) => p.id === afterId) ?? ofPose[ofPose.length - 1]

  if (!activePose || !before || !after) {
    return (
      <section className="rounded-2xl border border-dashed border-border-strong bg-surface/40 p-6">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
          Karşılaştırma
        </h2>
        <p className="mt-2 text-sm leading-relaxed text-text-secondary">
          Aynı duruşa ait en az iki fotoğraf yüklediğinizde önce/sonra karşılaştırması burada
          görünecek.
        </p>
      </section>
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-4 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
          Karşılaştırma
        </h2>
        <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
          Yalnızca aynı duruştaki fotoğraflar karşılaştırılır — önden çekilmiş bir fotoğrafın
          yanına arkadan çekilmiş birini koymak gelişim hakkında bir şey söylemez.
        </p>
      </div>

      {posesWithEnough.length > 1 ? (
        <div className="mb-4 flex flex-wrap gap-2">
          {posesWithEnough.map(({ value, label }) => (
            <button
              key={value}
              type="button"
              onClick={() => {
                setPose(value)
                setBeforeId(null)
                setAfterId(null)
              }}
              className={`h-9 rounded-xl px-3.5 text-xs font-semibold uppercase tracking-wide transition-colors ${
                value === activePose
                  ? 'bg-primary text-canvas'
                  : 'border border-border text-text-secondary hover:text-text-primary'
              }`}
            >
              {label}
            </button>
          ))}
        </div>
      ) : null}

      <div className="grid gap-4 sm:grid-cols-2">
        <PhotoSide caption="Önce" photos={ofPose} selected={before} onSelect={setBeforeId} />
        <PhotoSide caption="Sonra" photos={ofPose} selected={after} onSelect={setAfterId} />
      </div>

      <p className="mt-4 border-t border-border pt-3 text-xs text-text-secondary">
        {daysBetween(before.takenOn, after.takenOn)} günlük değişim
      </p>
    </section>
  )
}

function PhotoSide({
  caption,
  photos,
  selected,
  onSelect,
}: {
  caption: string
  photos: ProgressPhoto[]
  selected: ProgressPhoto
  onSelect: (photoId: string) => void
}) {
  return (
    <div>
      <Field label={caption}>
        <select
          value={selected.id}
          onChange={(event) => onSelect(event.target.value)}
          className={inputClasses}
        >
          {photos.map((photo) => (
            <option key={photo.id} value={photo.id}>
              {formatDate(photo.takenOn)}
            </option>
          ))}
        </select>
      </Field>
      <ProgressPhotoImage photo={selected} className="mt-2.5 aspect-[3/4] w-full bg-surface-elevated" />
      {selected.note ? (
        <p className="mt-2 text-xs leading-relaxed text-text-secondary">{selected.note}</p>
      ) : null}
    </div>
  )
}

function daysBetween(from: string, to: string): number {
  // Snapped to midnight UTC so the answer is a whole number of calendar days.
  return Math.round(
    (Date.parse(`${to}T00:00:00Z`) - Date.parse(`${from}T00:00:00Z`)) / 86_400_000,
  )
}
