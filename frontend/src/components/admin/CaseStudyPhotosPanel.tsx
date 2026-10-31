import { useRef, useState } from 'react'

import {
  useCaseStudyPhotoPeriods,
  useCaseStudyPhotos,
  useDeleteCaseStudyPhoto,
  useSetCaseStudyPhotosPublished,
  useUploadCaseStudyPhoto,
} from '@/api/caseStudyApi'
import { CaseStudyCoachPhotoSourceProvider } from '@/context/PhotoSourceContext'
import { PeriodPhotos } from '@/components/caseStudy/PeriodPhotos'
import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import { formatDate } from '@/lib/date'
import type { CaseStudy } from '@/types/caseStudy'
import type { PhotoPose } from '@/types/member'
import { PHOTO_POSES } from '@/types/member'
import { todayIso } from '@/lib/date'

/** Mirrors `app.photos.max-size-bytes`; the server enforces it, this only says so earlier. */
const MAX_BYTES = 8 * 1024 * 1024

/**
 * Photos of the coach's own body for a case study.
 *
 * <p>Publishing them is deliberately a different act from publishing the numbers: it has its
 * own switch, its own confirmation, and it only has a public effect while the study itself
 * is published. A body photo is special-category personal data, and agreeing to show a chart
 * is not agreeing to show a body.
 */
export function CaseStudyPhotosPanel({ study }: { study: CaseStudy }) {
  return (
    <CaseStudyCoachPhotoSourceProvider>
      <PanelBody study={study} />
    </CaseStudyCoachPhotoSourceProvider>
  )
}

function PanelBody({ study }: { study: CaseStudy }) {
  const { data: photos, isLoading } = useCaseStudyPhotos(study.id)
  const { data: periods } = useCaseStudyPhotoPeriods(study.id)
  const upload = useUploadCaseStudyPhoto(study.id)
  const remove = useDeleteCaseStudyPhoto(study.id)
  const setPhotosPublished = useSetCaseStudyPhotosPublished(study.id)

  const fileInput = useRef<HTMLInputElement>(null)
  const today = todayIso()
  const [file, setFile] = useState<File | null>(null)
  const [takenOn, setTakenOn] = useState(today)
  const [pose, setPose] = useState<PhotoPose>('ON')
  const [localError, setLocalError] = useState<string | null>(null)
  const [consent, setConsent] = useState(false)

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
    upload.mutate(
      { file, takenOn, pose },
      {
        onSuccess: () => {
          setFile(null)
          if (fileInput.current) {
            fileInput.current.value = ''
          }
        },
      },
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
        Fotoğraflar
      </h2>
      <p className="mb-5 text-xs leading-relaxed text-text-muted">
        Her dönemin başı ve sonu için en yakın tarihli fotoğraf, aynı duruşta yan yana
        gösterilir; aradaki gün farkı sayfada yazılı olur. Yüklenen dosya yeniden kodlanır:
        konum (GPS) ve cihaz bilgisi gibi gizli veriler silinir. Yalnızca JPEG ve PNG kabul edilir.
      </p>

      <form className="space-y-4" onSubmit={submit}>
        <Field label="Fotoğraf *" hint="JPEG veya PNG · en fazla 8 MB">
          <input
            ref={fileInput}
            type="file"
            accept="image/jpeg,image/png"
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
        {localError || upload.isError ? (
          <p className="text-xs text-accent-orange">{localError ?? (upload.error as Error).message}</p>
        ) : null}
        <Button type="submit" disabled={upload.isPending}>
          {upload.isPending ? 'Yükleniyor…' : 'Fotoğrafı Yükle'}
        </Button>
      </form>

      <div className="mt-8">
        {isLoading ? (
          <div className="h-24 animate-pulse rounded-xl bg-surface-high" />
        ) : !photos || photos.length === 0 ? (
          <p className="text-sm italic text-text-muted">Henüz fotoğraf yüklenmedi.</p>
        ) : (
          <ul className="grid grid-cols-2 gap-3 sm:grid-cols-4">
            {photos.map((photo) => (
              <li key={photo.id}>
                <ProgressPhotoImage photo={photo} className="aspect-[3/4] w-full" />
                <p className="mt-1.5 text-[11px] text-text-secondary">
                  {formatDate(photo.takenOn)} ·{' '}
                  {PHOTO_POSES.find((p) => p.value === photo.pose)?.label}
                </p>
                <button
                  type="button"
                  disabled={remove.isPending}
                  onClick={() => remove.mutate(photo.id)}
                  className="text-[11px] text-text-muted underline hover:text-accent-orange"
                >
                  Sil
                </button>
              </li>
            ))}
          </ul>
        )}
        {remove.isError ? (
          <p className="mt-2 text-xs text-accent-orange">{(remove.error as Error).message}</p>
        ) : null}
      </div>

      {photos && photos.length > 0 && periods ? (
        <div className="mt-8 border-t border-border pt-6">
          <h3 className="mb-1 text-xs font-bold uppercase tracking-wider text-text-primary">
            Dönem eşleşmesi önizleme
          </h3>
          <p className="mb-5 text-xs text-text-muted">
            Yayınlarsanız herkesin göreceği eşleşmenin aynısı.
          </p>
          <PeriodPhotos periods={periods} />
        </div>
      ) : null}

      <div className="mt-8 rounded-xl border border-border-strong p-4">
        <h3 className="mb-2 text-xs font-bold uppercase tracking-wider text-text-primary">
          Fotoğrafları herkese aç
        </h3>
        {study.photosPublished ? (
          <>
            <p className="mb-3 text-xs leading-relaxed text-text-secondary">
              Fotoğraflar açık.{' '}
              {study.published
                ? 'Herkese açık vaka adresinde herkes görebiliyor (ana sayfada şu an gösterilmiyor).'
                : 'Ancak vaka çalışması yayında olmadığı için şu an kimse göremiyor.'}
            </p>
            <Button
              type="button"
              variant="secondary"
              disabled={setPhotosPublished.isPending}
              onClick={() => {
                setConsent(false)
                setPhotosPublished.mutate(false)
              }}
            >
              Fotoğrafları gizle
            </Button>
          </>
        ) : (
          <>
            <p className="mb-3 text-xs leading-relaxed text-text-secondary">
              Bu, verilerin yayınından ayrı bir karardır. Vücut fotoğrafı özel nitelikli kişisel
              veridir; yayınlanınca siteyi açan herkes görebilir. Gizlediğinizde bir sonraki
              görüntülemede kaybolur.
            </p>
            <label className="mb-3 flex items-start gap-2 text-xs text-text-primary">
              <input
                type="checkbox"
                checked={consent}
                onChange={(event) => setConsent(event.target.checked)}
                className="mt-0.5"
              />
              <span>
                Bu fotoğrafların herkese açık gösterilmesine kendi rızamla izin veriyorum.
              </span>
            </label>
            <Button
              type="button"
              disabled={!consent || !photos?.length || setPhotosPublished.isPending}
              onClick={() => setPhotosPublished.mutate(true)}
            >
              Fotoğrafları yayınla
            </Button>
          </>
        )}
        {setPhotosPublished.isError ? (
          <p className="mt-2 text-xs text-accent-orange">
            {(setPhotosPublished.error as Error).message}
          </p>
        ) : null}
      </div>
    </section>
  )
}
