import { useEffect, useState } from 'react'

import { usePhotoObjectUrl } from '@/api/photosApi'
import { CloseIcon } from '@/components/ui/icons'

const STEPS = [1, 1.5, 2, 3, 4]

/** A photo at full size with zoom buttons; tap the photo to step in. Esc or the backdrop closes it. */
export function PhotoLightbox({ photoId, caption, onClose }: { photoId: string; caption: string; onClose: () => void }) {
  const { url, error } = usePhotoObjectUrl(photoId)
  const [step, setStep] = useState(0)

  useEffect(() => {
    const onKey = (event: KeyboardEvent) => {
      if (event.key === 'Escape') onClose()
    }
    window.addEventListener('keydown', onKey)
    const previous = document.body.style.overflow
    document.body.style.overflow = 'hidden'
    return () => {
      window.removeEventListener('keydown', onKey)
      document.body.style.overflow = previous
    }
  }, [onClose])

  const zoom = STEPS[step]
  const button =
    'flex h-10 min-w-10 items-center justify-center rounded-lg border border-white/20 bg-black/60 px-3 text-sm font-semibold text-white disabled:opacity-40'

  return (
    <div role="dialog" aria-modal="true" aria-label={caption} className="fixed inset-0 z-[70] flex flex-col bg-black/90">
      <div className="flex items-center justify-between gap-3 p-3">
        <p className="min-w-0 truncate text-xs font-semibold text-white">{caption}</p>
        <div className="flex shrink-0 gap-2">
          <button type="button" className={button} aria-label="Uzaklaştır" disabled={step === 0} onClick={() => setStep((s) => Math.max(0, s - 1))}>
            −
          </button>
          <button type="button" className={button} aria-label="Yakınlaştır" disabled={step === STEPS.length - 1} onClick={() => setStep((s) => Math.min(STEPS.length - 1, s + 1))}>
            +
          </button>
          <button type="button" className={button} aria-label="Kapat" onClick={onClose}>
            <CloseIcon className="h-4 w-4" />
          </button>
        </div>
      </div>
      {/* Clicking the dark area closes; the photo itself scrolls once it is larger than the screen. */}
      <div className="min-h-0 flex-1 overflow-auto" onClick={onClose}>
        {error ? (
          <p className="p-6 text-center text-sm text-white">Fotoğraf yüklenemedi.</p>
        ) : url ? (
          <img
            src={url}
            alt={caption}
            onClick={(event) => {
              event.stopPropagation()
              setStep((s) => (s + 1) % STEPS.length)
            }}
            style={{ width: `${zoom * 100}%`, maxWidth: zoom === 1 ? '100%' : 'none' }}
            className={`mx-auto block ${zoom === 1 ? 'max-h-full object-contain' : 'cursor-zoom-out'} ${zoom === 1 ? 'cursor-zoom-in' : ''}`}
          />
        ) : (
          <div className="mx-auto mt-10 h-64 w-48 animate-pulse rounded-xl bg-white/10" />
        )}
      </div>
    </div>
  )
}
