import { useMemo, useState } from 'react'

import type { PhotoPose, ProgressPhoto } from '@/types/member'

import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { PHOTO_POSES } from '@/types/member'

const MONTH_FORMAT: Intl.DateTimeFormatOptions = { month: 'long', year: 'numeric' }

function monthLabel(month: string): string {
  const [year, number] = month.split('-').map(Number)
  return new Date(year, number - 1, 1).toLocaleDateString('tr-TR', MONTH_FORMAT)
}

/**
 * Side-by-side comparison the member sets up: which poses, and which two months. Each chosen pose is a
 * row with the "before" month on the left and the "after" month on the right, small enough to fit two on
 * a phone; tapping a photo opens it large with zoom. Only the same pose is ever put next to each other.
 */
export function PhotoComparison({ photos }: { photos: ProgressPhoto[] }) {
  // Newest photo wins when a pose has more than one in a month (older uploads were not limited).
  const byKey = useMemo(() => {
    const map = new Map<string, ProgressPhoto>()
    for (const photo of [...photos].sort((a, b) => a.takenOn.localeCompare(b.takenOn))) {
      map.set(`${photo.takenOn.slice(0, 7)}|${photo.pose}`, photo)
    }
    return map
  }, [photos])

  const months = useMemo(
    () => [...new Set(photos.map((photo) => photo.takenOn.slice(0, 7)))].sort(),
    [photos],
  )

  const [chosenPoses, setChosenPoses] = useState<PhotoPose[] | null>(null)
  const [beforeMonth, setBeforeMonth] = useState<string | null>(null)
  const [afterMonth, setAfterMonth] = useState<string | null>(null)

  if (months.length < 2) {
    return (
      <section className="rounded-2xl border border-dashed border-border-strong bg-surface/40 p-5 sm:p-6">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Karşılaştırma</h2>
        <p className="mt-2 text-sm leading-relaxed text-text-secondary">
          Farklı iki ayda fotoğraf yüklediğinizde, istediğiniz pozları ve ayları seçip yan yana karşılaştırabileceksiniz.
        </p>
      </section>
    )
  }

  const before = beforeMonth && months.includes(beforeMonth) ? beforeMonth : months[0]
  const after = afterMonth && months.includes(afterMonth) ? afterMonth : months[months.length - 1]
  const poses = chosenPoses ?? PHOTO_POSES.map((p) => p.value)

  const toggle = (pose: PhotoPose) =>
    setChosenPoses(poses.includes(pose) ? poses.filter((p) => p !== pose) : [...poses, pose])

  const select = 'h-10 w-full rounded-xl border border-border bg-surface-elevated px-3 text-sm text-text-primary'

  return (
    <section className="rounded-2xl border border-border bg-surface p-4 sm:p-6">
      <div className="mb-4 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Karşılaştırma</h2>
        <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
          Pozları ve iki ayı seçin. Büyütmek için fotoğrafa dokunun.
        </p>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <label className="text-xs font-medium text-text-secondary">
          Önce (ay)
          <select value={before} onChange={(e) => setBeforeMonth(e.target.value)} className={`${select} mt-1`}>
            {months.map((month) => (
              <option key={month} value={month}>{monthLabel(month)}</option>
            ))}
          </select>
        </label>
        <label className="text-xs font-medium text-text-secondary">
          Sonra (ay)
          <select value={after} onChange={(e) => setAfterMonth(e.target.value)} className={`${select} mt-1`}>
            {months.map((month) => (
              <option key={month} value={month}>{monthLabel(month)}</option>
            ))}
          </select>
        </label>
      </div>

      <div className="mt-4 flex flex-wrap gap-2" role="group" aria-label="Karşılaştırılacak pozlar">
        {PHOTO_POSES.map(({ value, label }) => (
          <button
            key={value}
            type="button"
            aria-pressed={poses.includes(value)}
            onClick={() => toggle(value)}
            className={`h-9 rounded-xl px-3 text-[11px] font-semibold tracking-wide transition-colors ${
              poses.includes(value)
                ? 'bg-primary text-canvas'
                : 'border border-border text-text-secondary hover:text-text-primary'
            }`}
          >
            {label}
          </button>
        ))}
      </div>

      {poses.length === 0 ? (
        <p className="mt-5 text-xs text-text-muted">Karşılaştırmak için en az bir poz seçin.</p>
      ) : (
        <div className="mt-5 space-y-5">
          {PHOTO_POSES.filter((p) => poses.includes(p.value)).map(({ value, label }) => (
            <div key={value}>
              <h3 className="mb-2 text-xs font-bold uppercase tracking-wider text-text-secondary">{label}</h3>
              <div className="mx-auto grid max-w-md grid-cols-2 gap-3">
                <Slot photo={byKey.get(`${before}|${value}`)} month={before} label={label} caption="Önce" />
                <Slot photo={byKey.get(`${after}|${value}`)} month={after} label={label} caption="Sonra" />
              </div>
            </div>
          ))}
        </div>
      )}
    </section>
  )
}

function Slot({ photo, month, label, caption }: { photo: ProgressPhoto | undefined; month: string; label: string; caption: string }) {
  return (
    <figure>
      {photo ? (
        <ProgressPhotoImage
          photo={photo}
          zoomable
          caption={`${monthLabel(month)} · ${label}`}
          className="aspect-[3/4] w-full bg-surface-elevated"
        />
      ) : (
        <div className="flex aspect-[3/4] w-full items-center justify-center rounded-xl border border-dashed border-border p-2 text-center text-[11px] text-text-muted">
          Bu ay bu poz yok
        </div>
      )}
      <figcaption className="mt-1 text-center text-[11px] text-text-muted">
        {caption} · {monthLabel(month)}
      </figcaption>
    </figure>
  )
}
