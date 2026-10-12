import { usePhotoObjectUrl } from '@/api/photosApi'
import { formatDate } from '@/lib/date'
import type { ProgressPhoto } from '@/types/member'

/**
 * One photo. The bytes come through an authenticated fetch rather than a plain `<img src>`,
 * because the endpoint needs a bearer token. Which endpoint and which token comes from
 * `PhotoSourceContext`, so this works on both sides without being told where it is.
 */
export function ProgressPhotoImage({
  photo,
  className = '',
}: {
  photo: ProgressPhoto
  className?: string
}) {
  const { url, error } = usePhotoObjectUrl(photo.id)

  if (error) {
    return (
      <div
        className={`flex items-center justify-center rounded-xl border border-error/40 bg-error/10 p-4 text-center text-[11px] leading-relaxed text-error ${className}`}
      >
        Fotoğraf yüklenemedi: {error.message}
      </div>
    )
  }

  if (!url) {
    return <div className={`animate-pulse rounded-xl bg-surface-elevated ${className}`} />
  }

  return (
    <img
      src={url}
      alt={`${formatDate(photo.takenOn)} tarihli gelişim fotoğrafı`}
      className={`rounded-xl object-cover ${className}`}
    />
  )
}
