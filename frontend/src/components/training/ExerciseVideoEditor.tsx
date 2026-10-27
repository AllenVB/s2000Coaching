import { useState } from 'react'

import { useUpdateExercise } from '@/api/trainingApi'
import { ExerciseVideo } from '@/components/training/ExerciseVideo'
import { Button } from '@/components/ui/Button'
import { inputClasses } from '@/components/ui/FormField'
import type { Exercise } from '@/types/training'

/**
 * Where the coach pastes a YouTube link for an exercise. The video belongs to the exercise in
 * the catalogue, so one link serves every plan that prescribes it — and every member's copy of
 * those plans picks it up at once.
 */
export function ExerciseVideoEditor({ exercise }: { exercise: Exercise }) {
  const updateExercise = useUpdateExercise()
  const [editing, setEditing] = useState(false)
  const [value, setValue] = useState(exercise.videoUrl ?? '')

  const save = (event: React.FormEvent) => {
    event.preventDefault()
    updateExercise.mutate(
      {
        exerciseId: exercise.id,
        input: {
          name: exercise.name,
          muscleGroup: exercise.muscleGroup,
          note: exercise.note,
          videoUrl: value.trim() || null,
        },
      },
      { onSuccess: () => setEditing(false) },
    )
  }

  if (!editing) {
    return (
      <div className="mt-1">
        <button
          type="button"
          onClick={() => {
            setValue(exercise.videoUrl ?? '')
            setEditing(true)
          }}
          className="text-xs font-semibold text-accent-orange hover:underline"
        >
          {exercise.videoUrl ? 'Videoyu değiştir' : 'Video ekle'}
        </button>
        <ExerciseVideo exercise={exercise} />
      </div>
    )
  }

  return (
    <form className="mt-2 space-y-2" onSubmit={save}>
      <input
        value={value}
        onChange={(event) => setValue(event.target.value)}
        type="text"
        inputMode="url"
        placeholder="YouTube bağlantısı — https://youtu.be/…"
        aria-label={`${exercise.name} video bağlantısı`}
        className={inputClasses}
      />
      <p className="text-[11px] text-text-muted">
        youtube.com ve youtu.be bağlantıları kabul edilir; başlangıç zamanı (?t=90) korunur.
        Boş bırakıp kaydederseniz video kaldırılır.
      </p>
      {updateExercise.isError ? <p className="text-xs text-error">{updateExercise.error.message}</p> : null}
      <div className="flex gap-2">
        <Button type="submit" size="sm" disabled={updateExercise.isPending}>
          {updateExercise.isPending ? 'Kaydediliyor…' : 'Kaydet'}
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={() => setEditing(false)}>
          Vazgeç
        </Button>
      </div>
    </form>
  )
}
