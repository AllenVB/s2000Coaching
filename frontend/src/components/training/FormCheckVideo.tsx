import { PlayIcon } from '@/components/ui/icons'
import type { FormCheck } from '@/types/formCheck'

/**
 * A form-check clip is only a link to YouTube: it opens there in a new tab and is never played inside
 * the site. An embedded player per request would load a player per row and slow the page for everyone.
 * The address is built from the video id alone, so what opens is always a YouTube watch page.
 */
export function FormCheckVideo({ check }: { check: Pick<FormCheck, 'video' | 'exerciseName'> }) {
  if (!check.video) {
    return <p className="text-xs text-error">Video bağlantısı okunamadı.</p>
  }

  const start = check.video.startSeconds > 0 ? `&t=${check.video.startSeconds}s` : ''
  return (
    <a
      href={`https://www.youtube.com/watch?v=${encodeURIComponent(check.video.id)}${start}`}
      target="_blank"
      rel="noopener noreferrer"
      aria-label={`${check.exerciseName} form videosunu YouTube'da aç`}
      className="inline-flex items-center gap-1.5 rounded-lg bg-surface-elevated px-3 py-1.5 text-xs font-semibold text-text-primary transition-colors hover:bg-surface-high"
    >
      <PlayIcon className="h-3 w-3 text-accent-orange" />
      Videoyu YouTube'da aç
    </a>
  )
}
