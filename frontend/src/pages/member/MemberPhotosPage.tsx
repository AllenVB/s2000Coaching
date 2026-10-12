import { useState } from 'react'

import type { ProgressPhoto } from '@/types/member'

import { useDeleteMyPhoto, useMyPhotos } from '@/api/photosApi'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { PhotoComparison } from '@/components/member/PhotoComparison'
import { PhotoUploadForm } from '@/components/member/PhotoUploadForm'
import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { MemberPhotoSourceProvider } from '@/context/PhotoSourceContext'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { formatDate } from '@/lib/date'
import { PHOTO_POSES } from '@/types/member'

export function MemberPhotosPage() {
  return (
    <MemberPhotoSourceProvider>
      <PhotosView />
    </MemberPhotoSourceProvider>
  )
}

function PhotosView() {
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
      <PageHeading
        title="Fotoğraflar"
        description="Tartının göstermediği değişimi gösterirler. Yalnızca siz ve koçunuz görebilir."
      />

      <div className="space-y-6">
        <PhotoUploadForm />

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
  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-4 border-b border-border pb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
        Tüm Fotoğraflar ({photos.length})
      </h2>

      <ul className="grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4">
        {photos.map((photo) => (
          <li key={photo.id}>
            <ProgressPhotoImage photo={photo} className="aspect-[3/4] w-full bg-surface-elevated" />
            <p className="mt-2 text-xs font-semibold text-text-primary">
              {formatDate(photo.takenOn)}
            </p>
            <p className="text-[11px] text-text-muted">{poseLabel(photo.pose)}</p>
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
    </section>
  )
}

function poseLabel(pose: ProgressPhoto['pose']): string {
  return PHOTO_POSES.find(({ value }) => value === pose)?.label ?? pose
}
