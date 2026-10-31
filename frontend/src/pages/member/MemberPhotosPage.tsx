import { useState } from 'react'

import type { ProgressPhoto } from '@/types/member'

import { useDeleteMyPhoto, useMyPhotos } from '@/api/photosApi'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { PhotoComparison } from '@/components/member/PhotoComparison'
import { StoryConsentPanel } from '@/components/member/StoryConsentPanel'
import { PhotoUploadForm } from '@/components/member/PhotoUploadForm'
import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { MemberPhotoSourceProvider } from '@/context/PhotoSourceContext'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { formatDate } from '@/lib/date'
import { PHOTO_POSES } from '@/types/member'

/** The photo gallery; `embedded` drops the page heading when it sits inside a tab of Ölçümlerim. */
export function MemberPhotosPage({ embedded = false }: { embedded?: boolean }) {
  return (
    <MemberPhotoSourceProvider>
      <PhotosView embedded={embedded} />
    </MemberPhotoSourceProvider>
  )
}

function PhotosView({ embedded }: { embedded: boolean }) {
  const { member } = useMemberProfileSync()
  const { data: photos, isLoading, isError, error } = useMyPhotos()
  const deletePhoto = useDeleteMyPhoto()
  const [confirming, setConfirming] = useState<ProgressPhoto | null>(null)

  if (!member) {
    return null
  }

  // A body photo is special-category data under KVKK, like a measurement and more so:
  // nothing is shown or written before the member has consented.
  if (!member.healthDataConsentAt) {
    return (
      <>
        <PageHeading
          title="Fotoğraflar"
          description="Gelişim fotoğrafı yükleyebilmek için önce onay vermeniz gerekiyor."
        />
        <HealthDataConsent />
      </>
    )
  }

  return (
    <>
      {embedded ? (
        <p className="mb-6 text-xs leading-relaxed text-text-secondary">
          Tartının göstermediği değişimi gösterirler. Yalnızca siz ve koçunuz görebilir.
        </p>
      ) : (
        <PageHeading
          title="Fotoğraflar"
          description="Tartının göstermediği değişimi gösterirler. Yalnızca siz ve koçunuz görebilir."
        />
      )}

      <div className="space-y-6">
        <StoryConsentPanel />
        <PhotoUploadForm photos={photos ?? []} />

        {isLoading ? (
          <div className="h-48 animate-pulse rounded-2xl border border-border bg-surface" />
        ) : isError ? (
          <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
            Fotoğraflar yüklenemedi: {error.message}
          </p>
        ) : photos?.length ? (
          <>
            <PhotoComparison photos={photos} />
            <PhotoGallery photos={photos} onDelete={setConfirming} />
          </>
        ) : (
          <ComingSoonCard
            title="Henüz Fotoğraf Yok"
            description="İlk fotoğrafınız, sonraki her fotoğrafın karşılaştırılacağı başlangıç noktası olacak."
          />
        )}
      </div>

      {confirming ? (
        <ConfirmDialog
          title="Fotoğrafı Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deletePhoto.isPending}
          error={deletePhoto.isError ? deletePhoto.error.message : null}
          description={`${formatDate(confirming.takenOn)} tarihli fotoğraf kalıcı olarak silinecek. Bu işlem geri alınamaz.`}
          onCancel={() => setConfirming(null)}
          onConfirm={() =>
            deletePhoto.mutate({ photoId: confirming.id }, { onSuccess: () => setConfirming(null) })
          }
        />
      ) : null}
    </>
  )
}

function PhotoGallery({
  photos,
  onDelete,
}: {
  photos: ProgressPhoto[]
  onDelete: (photo: ProgressPhoto) => void
}) {
  const months = groupByMonth(photos)
  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-4 border-b border-border pb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
        Tüm Fotoğraflar ({photos.length})
      </h2>

      {months.map(([month, monthPhotos]) => (
        <div key={month} className="mb-6 last:mb-0">
          <h3 className="mb-3 text-xs font-bold uppercase tracking-wider text-text-secondary">
            {monthLabel(month)}
          </h3>
          <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-5">
            {monthPhotos.map((photo) => (
              <li key={photo.id}>
                <ProgressPhotoImage photo={photo} className="aspect-[3/4] w-full bg-surface-elevated" />
                <p className="mt-2 text-xs font-semibold text-text-primary">{poseLabel(photo.pose)}</p>
                <p className="text-[11px] text-text-muted">{formatDate(photo.takenOn)}</p>
                <Button
                  type="button"
                  variant="ghost"
                  size="sm"
                  className="mt-1 px-0"
                  onClick={() => onDelete(photo)}
                >
                  Sil
                </Button>
              </li>
            ))}
          </ul>
        </div>
      ))}
    </section>
  )
}

function poseLabel(pose: ProgressPhoto['pose']): string {
  return PHOTO_POSES.find(({ value }) => value === pose)?.label ?? pose
}

/** Newest month first; inside a month the five poses keep the order they are asked in. */
function groupByMonth(photos: ProgressPhoto[]): [string, ProgressPhoto[]][] {
  const groups = new Map<string, ProgressPhoto[]>()
  for (const photo of photos) {
    const key = photo.takenOn.slice(0, 7)
    groups.set(key, [...(groups.get(key) ?? []), photo])
  }
  const order = PHOTO_POSES.map((p) => p.value)
  return [...groups.entries()]
    .sort(([a], [b]) => b.localeCompare(a))
    .map(([key, list]) => [key, [...list].sort((a, b) => order.indexOf(a.pose) - order.indexOf(b.pose))])
}

function monthLabel(month: string): string {
  const [year, number] = month.split('-').map(Number)
  return new Date(year, number - 1, 1).toLocaleDateString('tr-TR', { month: 'long', year: 'numeric' })
}
