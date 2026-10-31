import { useState } from 'react'

import { PhotoLightbox } from '@/components/member/PhotoLightbox'
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
  zoomable = false,
  caption,
}: {
  photo: Pick<ProgressPhoto, 'id' | 'takenOn'>
  className?: string
  /** Tapping opens the photo full size with zoom. */
  zoomable?: boolean
  caption?: string
}) {
  const { url, error } = usePhotoObjectUrl(photo.id)
  const [open, setOpen] = useState(false)

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

  const alt = `${formatDate(photo.takenOn)} tarihli gelişim fotoğrafı`
  const image = <img src={url} alt={alt} className={`rounded-xl object-cover ${className}`} />

  if (!zoomable) {
    return image
  }
  return (
    <>
      <button type="button" onClick={() => setOpen(true)} aria-label={`${alt} — büyüt`} className="block w-full cursor-zoom-in">
        {image}
      </button>
      {open ? <PhotoLightbox photoId={photo.id} caption={caption ?? alt} onClose={() => setOpen(false)} /> : null}
    </>
  )
}
