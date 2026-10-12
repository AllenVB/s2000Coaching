import { useState } from 'react'

import {
  useAddPlanDay,
  useAddPlanExercise,
  useDeletePlanDay,
  useDeletePlanExercise,
  useExercises,
} from '@/api/trainingApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { inputClasses } from '@/components/ui/FormField'
import { PlusIcon, TrashIcon } from '@/components/ui/icons'
import { muscleGroupLabel, type PlanDay, type TrainingPlan } from '@/types/training'

/** Day-and-exercise editor, shared by the template page and a member's training tab. */
export function PlanEditor({ plan }: { plan: TrainingPlan }) {
  const addDay = useAddPlanDay()
  const [dayName, setDayName] = useState('')

  const submitDay = (event: React.FormEvent) => {
    event.preventDefault()
    const name = dayName.trim()
    if (name.length > 0) {
      addDay.mutate({ planId: plan.id, name }, { onSuccess: () => setDayName('') })
    }
  }

  return (
    <div className="space-y-4">
      {plan.days.map((day) => (
        <DayCard key={day.id} day={day} />
      ))}

      <form className="flex flex-wrap items-center gap-2" onSubmit={submitDay}>
        <input
          value={dayName}
          onChange={(event) => setDayName(event.target.value)}
          type="text"
          placeholder="Örn: Gün 1 — İtiş"
          aria-label="Yeni gün adı"
          className={`${inputClasses} max-w-xs`}
        />
        <Button type="submit" variant="secondary" disabled={addDay.isPending || dayName.trim().length === 0}>
          <PlusIcon className="h-3.5 w-3.5" />
          Gün Ekle
        </Button>
      </form>

      {addDay.isError ? <p className="text-xs text-error">{addDay.error.message}</p> : null}
    </div>
  )
}

function DayCard({ day }: { day: PlanDay }) {
  const deleteDay = useDeletePlanDay()
  const deleteExercise = useDeletePlanExercise()
  const [isConfirmingDelete, setConfirmingDelete] = useState(false)
  const [isAdding, setAdding] = useState(false)

  return (
    <section className="rounded-2xl border border-border bg-surface-elevated p-5">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3 border-b border-border pb-3">
        <h3 className="text-sm font-bold uppercase tracking-wider text-text-primary">{day.name}</h3>
        <button
          type="button"
          onClick={() => setConfirmingDelete(true)}
          aria-label={`${day.name} gününü sil`}
          className="text-text-muted transition-colors hover:text-error"
        >
          <TrashIcon className="h-4 w-4" />
        </button>
      </div>

      {day.exercises.length === 0 ? (
        <p className="text-sm italic text-text-muted">Bu güne henüz hareket eklenmedi.</p>
      ) : (
        <ul className="space-y-2">
          {day.exercises.map((planExercise) => (
            <li
              key={planExercise.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface px-4 py-3"
            >
              <span className="min-w-0">
                <span className="block font-semibold text-text-primary">{planExercise.exercise.name}</span>
                <span className="block text-xs text-text-muted">
                  {muscleGroupLabel(planExercise.exercise.muscleGroup)} · {planExercise.setCount} set ×{' '}
                  {planExercise.reps}
                  {planExercise.targetWeightKg !== null ? ` · ${planExercise.targetWeightKg} kg` : ''}
                  {planExercise.restSeconds !== null ? ` · ${planExercise.restSeconds} sn dinlenme` : ''}
                </span>
                {planExercise.note ? (
                  <span className="mt-0.5 block text-xs text-text-secondary">{planExercise.note}</span>
                ) : null}
              </span>
              <button
                type="button"
                onClick={() => deleteExercise.mutate({ planExerciseId: planExercise.id })}
                aria-label={`${planExercise.exercise.name} hareketini çıkar`}
                className="text-text-muted transition-colors hover:text-error"
              >
                <TrashIcon className="h-4 w-4" />
              </button>
            </li>
          ))}
        </ul>
      )}

      {isAdding ? (
        <AddExerciseForm dayId={day.id} onDone={() => setAdding(false)} />
      ) : (
        <Button type="button" variant="ghost" size="sm" className="mt-3" onClick={() => setAdding(true)}>
          <PlusIcon className="h-3.5 w-3.5" />
          Hareket Ekle
        </Button>
      )}

      {isConfirmingDelete ? (
        <ConfirmDialog
          title="Günü Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteDay.isPending}
          error={deleteDay.isError ? deleteDay.error.message : null}
          description={`"${day.name}" gününü ve içindeki hareketleri silmek istediğinize emin misiniz?`}
          onCancel={() => setConfirmingDelete(false)}
          onConfirm={() => deleteDay.mutate({ dayId: day.id }, { onSuccess: () => setConfirmingDelete(false) })}
        />
      ) : null}
    </section>
  )
}

function AddExerciseForm({ dayId, onDone }: { dayId: string; onDone: () => void }) {
  const { data: exercises } = useExercises()
  const addExercise = useAddPlanExercise()

  const [exerciseId, setExerciseId] = useState('')
  const [setCount, setSetCount] = useState('3')
  const [reps, setReps] = useState('8-10')
  const [targetWeight, setTargetWeight] = useState('')
  const [rest, setRest] = useState('')

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (!exerciseId) {
      return
    }
    addExercise.mutate(
      {
        dayId,
        input: {
          exerciseId,
          setCount: Number(setCount),
          reps: reps.trim(),
          targetWeightKg: targetWeight === '' ? null : Number(targetWeight),
          restSeconds: rest === '' ? null : Number(rest),
        },
      },
      { onSuccess: onDone },
    )
  }

  return (
    <form className="mt-4 space-y-3 rounded-xl border border-border bg-surface p-4" onSubmit={submit}>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
        <label className="col-span-2 sm:col-span-2">
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Hareket</span>
          <select
            value={exerciseId}
            onChange={(event) => setExerciseId(event.target.value)}
            className={inputClasses}
          >
            <option value="">Seçin…</option>
            {exercises?.map((exercise) => (
              <option key={exercise.id} value={exercise.id}>
                {exercise.name} ({muscleGroupLabel(exercise.muscleGroup)})
              </option>
            ))}
          </select>
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Set</span>
          <input
            value={setCount}
            onChange={(event) => setSetCount(event.target.value)}
            type="number"
            min={1}
            max={20}
            className={inputClasses}
          />
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Tekrar</span>
          <input
            value={reps}
            onChange={(event) => setReps(event.target.value)}
            type="text"
            placeholder="8-10"
            className={inputClasses}
          />
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Hedef (kg)</span>
          <input
            value={targetWeight}
            onChange={(event) => setTargetWeight(event.target.value)}
            type="number"
            step="0.5"
            placeholder="—"
            className={inputClasses}
          />
        </label>
        <label>
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Dinlenme (sn)</span>
          <input
            value={rest}
            onChange={(event) => setRest(event.target.value)}
            type="number"
            step="15"
            placeholder="—"
            className={inputClasses}
          />
        </label>
      </div>

      {addExercise.isError ? <p className="text-xs text-error">{addExercise.error.message}</p> : null}

      <div className="flex flex-wrap gap-2">
        <Button type="submit" size="sm" disabled={addExercise.isPending || !exerciseId}>
          {addExercise.isPending ? 'Ekleniyor…' : 'Ekle'}
        </Button>
        <Button type="button" size="sm" variant="ghost" onClick={onDone}>
          Vazgeç
        </Button>
      </div>
    </form>
  )
}
