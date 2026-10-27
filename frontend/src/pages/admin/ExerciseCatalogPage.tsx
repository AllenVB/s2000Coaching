import { useState } from 'react'

import { useCreateExercise, useDeleteExercise, useExercises } from '@/api/trainingApi'
import { ExerciseVideoEditor } from '@/components/training/ExerciseVideoEditor'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Field, inputClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { TrashIcon } from '@/components/ui/icons'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { MUSCLE_GROUPS, muscleGroupLabel, type MuscleGroup } from '@/types/training'

/** The catalogue every plan draws from — written once, reused across members. */
export function ExerciseCatalogPage() {
  const { data: exercises, isLoading, isError, error } = useExercises()
  useSignOutOn401(error)

  const createExercise = useCreateExercise()
  const deleteExercise = useDeleteExercise()

  const [name, setName] = useState('')
  const [muscleGroup, setMuscleGroup] = useState<MuscleGroup>('GOGUS')
  const [videoUrl, setVideoUrl] = useState('')
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = name.trim()
    if (trimmed.length === 0) {
      return
    }
    createExercise.mutate(
      { name: trimmed, muscleGroup, videoUrl: videoUrl.trim() || null },
      {
        onSuccess: () => {
          setName('')
          setVideoUrl('')
        },
      },
    )
  }

  const deletingExercise = exercises?.find((exercise) => exercise.id === deletingId)

  return (
    <>
      <PageHeading
        title="Hareketler"
        description="Planlarda kullanacağınız hareket katalogu. Bir planda kullanılan hareket silinemez."
      />

      <div className="space-y-6">
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">Hareket Ekle</h2>
          <form className="space-y-4" onSubmit={submit}>
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
              <Field label="Ad *">
                <input
                  value={name}
                  onChange={(event) => setName(event.target.value)}
                  type="text"
                  placeholder="Örn: Incline Dumbbell Press"
                  className={inputClasses}
                />
              </Field>
              <Field label="Kas Grubu *">
                <select
                  value={muscleGroup}
                  onChange={(event) => setMuscleGroup(event.target.value as MuscleGroup)}
                  className={inputClasses}
                >
                  {MUSCLE_GROUPS.map((group) => (
                    <option key={group.value} value={group.value}>
                      {group.label}
                    </option>
                  ))}
                </select>
              </Field>
              <Field label="Video Bağlantısı">
                <input
                  value={videoUrl}
                  onChange={(event) => setVideoUrl(event.target.value)}
                  type="text"
                  inputMode="url"
                  placeholder="YouTube bağlantısı — https://youtu.be/…"
                  className={inputClasses}
                />
              </Field>
            </div>

            {createExercise.isError ? (
              <p className="text-xs text-error">{createExercise.error.message}</p>
            ) : null}

            <Button type="submit" disabled={createExercise.isPending || name.trim().length === 0}>
              {createExercise.isPending ? 'Ekleniyor…' : 'Hareketi Ekle'}
            </Button>
          </form>
        </section>

        {isLoading ? (
          <div className="h-32 animate-pulse rounded-2xl border border-border bg-surface" />
        ) : isError ? (
          <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
            Hareketler yüklenemedi: {error.message}
          </p>
        ) : (
          <section className="rounded-2xl border border-border bg-surface p-6">
            <h2 className="mb-4 border-b border-border pb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
              Katalog ({exercises?.length ?? 0})
            </h2>
            <ul className="space-y-2">
              {exercises?.map((exercise) => (
                <li
                  key={exercise.id}
                  className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface-elevated px-4 py-3"
                >
                  <span className="min-w-0">
                    <span className="block font-semibold text-text-primary">{exercise.name}</span>
                    <span className="block text-xs text-text-muted">
                      {muscleGroupLabel(exercise.muscleGroup)}
                      {exercise.videoUrl ? ' · video var' : ''}
                    </span>
                    <ExerciseVideoEditor exercise={exercise} />
                  </span>
                  <button
                    type="button"
                    onClick={() => setDeletingId(exercise.id)}
                    aria-label={`${exercise.name} hareketini sil`}
                    className="text-text-muted transition-colors hover:text-error"
                  >
                    <TrashIcon className="h-4 w-4" />
                  </button>
                </li>
              ))}
            </ul>
          </section>
        )}
      </div>

      {deletingExercise ? (
        <ConfirmDialog
          title="Hareketi Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteExercise.isPending}
          error={deleteExercise.isError ? deleteExercise.error.message : null}
          description={`"${deletingExercise.name}" hareketini katalogdan silmek istediğinize emin misiniz?`}
          onCancel={() => {
            deleteExercise.reset()
            setDeletingId(null)
          }}
          onConfirm={() =>
            deleteExercise.mutate(
              { exerciseId: deletingExercise.id },
              { onSuccess: () => setDeletingId(null) },
            )
          }
        />
      ) : null}
    </>
  )
}
