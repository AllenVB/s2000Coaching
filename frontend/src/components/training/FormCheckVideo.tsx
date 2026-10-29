import { useState } from 'react'

import { YouTubeEmbed } from '@/components/training/ExerciseVideo'
import { PlayIcon } from '@/components/ui/icons'
import type { FormCheck } from '@/types/formCheck'

/** The member's clip, opened on demand so a page of requests does not load a page of players. */
export function FormCheckVideo({ check }: { check: Pick<FormCheck, 'video' | 'exerciseName'> }) {
  const [open, setOpen] = useState(false)

  if (!check.video) {
    return <p className="text-xs text-error">Video bağlantısı okunamadı.</p>
  }

  return (
    <div>
      <button
        type="button"
        onClick={() => setOpen((current) => !current)}
        aria-expanded={open}
        className="inline-flex items-center gap-1.5 rounded-lg bg-surface-elevated px-3 py-1.5 text-xs font-semibold text-text-primary transition-colors hover:bg-surface-high"
      >
        <PlayIcon className="h-3 w-3 text-accent-orange" />
        {open ? 'Videoyu kapat' : 'Videoyu izle'}
      </button>
      {open ? <YouTubeEmbed video={check.video} title={`${check.exerciseName} — form videosu`} /> : null}
    </div>
  )
}
