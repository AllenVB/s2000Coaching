import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { formatDate } from '@/lib/date'
import { PHOTO_POSES } from '@/types/member'
import { PERIOD_LABELS } from '@/types/caseStudy'
import type { PeriodPhotos as PeriodPhotosData, PhotoMatch, PosePair } from '@/types/caseStudy'

/**
 * How far a photo is from the date it stands in for, in words. The page never lets a photo
 * pass for a day it was not taken on, so the distance is always printed next to it.
 */
export function distanceLabel(days: number): string {
  if (days === 0) {
    return 'tam o gün çekilmiş'
  }
  return days < 0 ? `${-days} gün önce çekilmiş` : `${days} gün sonra çekilmiş`
}

/**
 * Before/after photos for each period of the long record, per pose. Needs a photo source
 * provider above it: the coach's preview and the public page fetch the bytes differently.
 */
export function PeriodPhotos({ periods }: { periods: PeriodPhotosData[] }) {
  if (periods.length === 0) {
    return (
      <p className="text-sm italic text-text-muted">
        Dönem ayrımı yapılamadığı için fotoğraflar dönemlerle eşleştirilemedi.
      </p>
    )
  }

  return (
    <div className="space-y-8">
      {periods.map((period) => (
        <article key={`${period.start}-${period.end}`}>
          <h4 className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
            {PERIOD_LABELS[period.label]}
          </h4>
          <p className="mb-4 text-xs text-text-muted">
            {formatDate(period.start)} – {formatDate(period.end)}
          </p>
          {period.poses.length === 0 ? (
            <p className="rounded-xl border border-dashed border-border-strong p-4 text-xs leading-relaxed text-text-secondary">
              {period.note}
            </p>
          ) : (
            <div className="space-y-6">
              {period.poses.map((pair) => (
                <PoseRow key={pair.pose} pair={pair} />
              ))}
            </div>
          )}
        </article>
      ))}
    </div>
  )
}

function PoseRow({ pair }: { pair: PosePair }) {
  const poseLabel = PHOTO_POSES.find((pose) => pose.value === pair.pose)?.label ?? pair.pose

  return (
    <div>
      <p className="mb-2 text-[11px] font-semibold uppercase tracking-[0.15em] text-accent-orange">
        {poseLabel}
      </p>
      <div className="grid max-w-xl grid-cols-2 gap-3 sm:gap-4">
        <Side title="Dönem başı" match={pair.start} />
        <Side title="Dönem sonu" match={pair.end} />
      </div>
      {pair.note ? (
        <p className="mt-2 text-xs leading-relaxed text-text-muted">{pair.note}</p>
      ) : null}
    </div>
  )
}

function Side({ title, match }: { title: string; match: PhotoMatch | null }) {
  return (
    <figure>
      {match ? (
        <ProgressPhotoImage
          photo={{ id: match.photoId, takenOn: match.takenOn }}
          className="aspect-[3/4] w-full"
        />
      ) : (
        <div className="flex aspect-[3/4] w-full items-center justify-center rounded-xl border border-dashed border-border-strong p-3 text-center text-[11px] text-text-muted">
          Uygun fotoğraf yok
        </div>
      )}
      <figcaption className="mt-2 text-xs leading-relaxed text-text-secondary">
        <span className="font-semibold text-text-primary">{title}</span>
        {match ? (
          <>
            {' · '}
            {formatDate(match.takenOn)}
            <br />
            <span className="text-text-muted">En yakın fotoğraf: {distanceLabel(match.daysFromTarget)}</span>
          </>
        ) : null}
      </figcaption>
    </figure>
  )
}
