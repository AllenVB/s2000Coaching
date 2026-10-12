import { useState } from 'react'

import type { ProgressPhoto } from '@/types/member'

import { useDeleteMemberPhoto, useMemberPhotos } from '@/api/photosApi'
import { PhotoComparison } from '@/components/member/PhotoComparison'
import { ProgressPhotoImage } from '@/components/member/ProgressPhotoImage'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { CoachPhotoSourceProvider } from '@/context/PhotoSourceContext'
import { formatDate } from '@/lib/date'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'
import { PHOTO_POSES } from '@/types/member'

/**
 * Coach-side read of the member's photos. There is no upload here on purpose: a photo of
 * someone's body is theirs to put there. The coach can remove one, the same as a check-in.
 */
export function MemberPhotosTab() {
  return (
    <CoachPhotoSourceProvider>
      <PhotosView />
    </CoachPhotoSourceProvider>
  )
}

function PhotosView() {
  const { member } = useMemberDetail()
  const { data: photos, isLoading, isError, error } = useMemberPhotos(member.id)
  const deletePhoto = useDeleteMemberPhoto(member.id)
  const [confirming, setConfirming] = useState<ProgressPhoto | null>(null)

  if (isLoading) {
    return <div className="h-48 animate-pulse rounded-2xl border border-border bg-surface" />
  }

  if (isError) {
    return (
      <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
        Fotoğraflar yüklenemedi: {error.message}
      </p>
    )
  }

  if (!photos?.length) {
    return (
      <section className="rounded-2xl border border-dashed border-border-strong bg-surface/40 p-6">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
          Fotoğraf Yok
        </h2>
        <p className="mt-2 text-sm leading-relaxed text-text-secondary">
          {member.fullName} henüz gelişim fotoğrafı yüklemedi. Fotoğrafları yalnızca üyenin
          kendisi ekleyebilir.
        </p>
      </section>
    )
  }

  return (
    <div className="space-y-6">
      <PhotoComparison photos={photos} />

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
              {photo.note ? (
                <p className="mt-1 text-[11px] leading-relaxed text-text-secondary">{photo.note}</p>
              ) : null}
              <Button
                type="button"
                variant="ghost"
                size="sm"
                className="mt-1 px-0"
                onClick={() => setConfirming(photo)}
              >
                Sil
              </Button>
            </li>
          ))}
        </ul>
      </section>

      {confirming ? (
        <ConfirmDialog
          title="Fotoğrafı Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deletePhoto.isPending}
          error={deletePhoto.isError ? deletePhoto.error.message : null}
          description={`${member.fullName} üyesinin ${formatDate(confirming.takenOn)} tarihli fotoğrafı kalıcı olarak silinecek. Bu işlem geri alınamaz.`}
          onCancel={() => setConfirming(null)}
          onConfirm={() =>
            deletePhoto.mutate({ photoId: confirming.id }, { onSuccess: () => setConfirming(null) })
          }
        />
      ) : null}
    </div>
  )
}

function poseLabel(pose: ProgressPhoto['pose']): string {
  return PHOTO_POSES.find(({ value }) => value === pose)?.label ?? pose
}
