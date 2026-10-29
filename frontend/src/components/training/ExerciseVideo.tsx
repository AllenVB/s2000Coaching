import { useState } from 'react'

import { PlayIcon } from '@/components/ui/icons'
import type { Exercise } from '@/types/training'

/**
 * "Nasıl yapılır?" under an exercise: opens the coach's YouTube video right there.
 *
 * <p>Closed by default and the player is only created on open. A training day lists a dozen
 * exercises, and a dozen live YouTube iframes would cost a phone on gym wifi its bandwidth and
 * battery before anyone pressed play. It also keeps YouTube from setting anything until the
 * member asks for the video: the address is the cookie-less `youtube-nocookie.com` one.
 *
 * <p>The player address is built from the video id alone, never from the stored link, so what
 * ends up in the iframe is always a YouTube embed.
 */
export function ExerciseVideo({ exercise }: { exercise: Pick<Exercise, 'name' | 'video' | 'videoUrl'> }) {
  const [open, setOpen] = useState(false)

  if (!exercise.video) {
    // An old catalogue entry that is not YouTube: no player, but still reachable.
    return exercise.videoUrl && /^https?:\/\//i.test(exercise.videoUrl) ? (
      <a
        href={exercise.videoUrl}
        target="_blank"
        rel="noopener noreferrer"
        className="mt-2 inline-flex items-center gap-1.5 text-xs font-semibold text-accent-orange hover:underline"
      >
        <PlayIcon className="h-3 w-3" />
        Videoyu aç
      </a>
    ) : null
  }

  return (
    <div className="mt-2">
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        className="inline-flex items-center gap-1.5 rounded-lg bg-surface-elevated px-3 py-1.5 text-xs font-semibold text-text-primary transition-colors hover:bg-surface-high"
      >
        <PlayIcon className="h-3 w-3 text-accent-orange" />
        {open ? 'Videoyu kapat' : 'Nasıl yapılır? Videoyu izle'}
      </button>

      {open ? <YouTubeEmbed video={exercise.video} title={`${exercise.name} — hareket videosu`} /> : null}
    </div>
  )
}

/**
 * The player: a YouTube iframe built from the video id alone, on the cookie-less domain, so
 * whatever was stored with the link can never choose where the iframe points.
 */
export function YouTubeEmbed({
  video,
  title,
  autoplay = true,
}: {
  video: { id: string; startSeconds: number }
  title: string
  autoplay?: boolean
}) {
  const params = new URLSearchParams({ rel: '0', playsinline: '1', modestbranding: '1' })
  if (autoplay) {
    params.set('autoplay', '1')
  }
  if (video.startSeconds > 0) {
    params.set('start', String(video.startSeconds))
  }

  return (
    <div className="mt-3 aspect-video w-full max-w-xl overflow-hidden rounded-xl border border-border bg-black">
      <iframe
        src={`https://www.youtube-nocookie.com/embed/${video.id}?${params.toString()}`}
        title={title}
        className="h-full w-full"
        allow="autoplay; encrypted-media; picture-in-picture; fullscreen"
        allowFullScreen
        referrerPolicy="strict-origin-when-cross-origin"
      />
    </div>
  )
}
