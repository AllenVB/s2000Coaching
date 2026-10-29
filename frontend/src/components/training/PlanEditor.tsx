import { useRef, useState } from 'react'

import {
  useAddPlanDay,
  useAddPlanExercise,
  useDeletePlanDay,
  useDeletePlanExercise,
  useExercises,
  useUpdatePlanExercise,
} from '@/api/trainingApi'
import { ExerciseVideoEditor } from '@/components/training/ExerciseVideoEditor'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { PlusIcon, TrashIcon } from '@/components/ui/icons'
import { MAX_SET_CELLS, muscleGroupLabel, type PlanDay, type PlanExercise, type TrainingPlan } from '@/types/training'

const SET_NUMBERS = Array.from({ length: MAX_SET_CELLS }, (_unused, index) => index + 1)

// Cells look like plain text until pointed at, so a filled plan reads like the printed one.
const cellClasses =
  'h-9 w-full rounded-md border border-transparent bg-transparent px-2 text-center text-sm text-text-primary transition-colors hover:border-border focus:border-primary focus:bg-surface focus:outline-none'
const smallFieldClasses =
  'h-8 w-full rounded-md border border-border bg-surface px-2 text-xs text-text-primary focus:border-primary focus:outline-none'

/**
 * Day-and-exercise editor, shared by the template page and a member's training tab. A day is
 * a table: the exercise, then the rep target of set 1 to 4 (none beyond the first is
 * required), saved when a cell loses focus. The "+" row at the foot adds an exercise.
 */
export function PlanEditor({ plan }: { plan: TrainingPlan }) {
  const addDay = useAddPlanDay()
  const [dayName, setDayName] = useState('')

  const add = () => {
    const name = dayName.trim()
    if (name.length > 0 && !addDay.isPending) {
      addDay.mutate({ planId: plan.id, name }, { onSuccess: () => setDayName('') })
    }
  }

  return (
    <div className="space-y-4" data-training-editor>
      {plan.days.map((day) => (
        <DayCard key={day.id} day={day} />
      ))}

      <div className="flex flex-wrap items-center gap-2">
        <input
          value={dayName}
          onChange={(event) => setDayName(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') {
              event.preventDefault()
              add()
            }
          }}
          type="text"
          placeholder={`Örn: Gün ${plan.days.length + 1}`}
          aria-label="Yeni gün adı"
          className="h-9 w-full max-w-xs rounded-lg border border-border bg-surface px-3 text-sm text-text-primary focus:border-primary focus:outline-none"
        />
        <button
          type="button"
          onClick={add}
          disabled={addDay.isPending || dayName.trim().length === 0}
          aria-label="Gün ekle"
          className="inline-flex h-9 items-center gap-1.5 rounded-full border border-border-strong bg-surface-elevated px-3.5 text-xs font-semibold uppercase tracking-wide text-text-primary transition-colors hover:border-primary/60 disabled:cursor-not-allowed disabled:opacity-50"
        >
          <PlusIcon className="h-3.5 w-3.5" />
          Gün Ekle
        </button>
      </div>

      {addDay.isError ? <p className="text-xs text-error">{addDay.error.message}</p> : null}
    </div>
  )
}

function DayCard({ day }: { day: PlanDay }) {
  const deleteDay = useDeletePlanDay()
  const [isConfirmingDelete, setConfirmingDelete] = useState(false)

  return (
    <section className="overflow-hidden rounded-2xl border border-border bg-surface">
      <div className="flex items-center justify-between gap-3 border-b border-border bg-surface-elevated px-5 py-3">
        <h3 className="text-sm font-bold uppercase tracking-wider text-primary">{day.name}</h3>
        <button
          type="button"
          onClick={() => setConfirmingDelete(true)}
          aria-label={`${day.name} gününü sil`}
          className="text-text-muted transition-colors hover:text-error"
        >
          <TrashIcon className="h-4 w-4" />
        </button>
      </div>

      <div className="overflow-x-auto">
        <table className="w-full min-w-[540px] border-collapse">
          <thead>
            <tr className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">
              <th scope="col" className="px-4 py-2 text-left">
                Hareket
              </th>
              {SET_NUMBERS.map((number) => (
                <th key={number} scope="col" className="w-16 px-1 py-2 text-center">
                  Set {number}
                </th>
              ))}
              <th scope="col" className="w-10">
                <span className="sr-only">İşlem</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {day.exercises.map((planExercise) => (
              <ExerciseRows key={planExercise.id} planExercise={planExercise} />
            ))}
            <AddExerciseRow dayId={day.id} />
          </tbody>
        </table>
      </div>

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

/**
 * What the set cells show for an exercise. One written per set shows as it was; one written the
 * older way (a count and a single free-text reps) shows that text in each of its sets, which is
 * what "3 × 8-10" means.
 */
function setCellValues(planExercise: PlanExercise): string[] {
  const perSet = planExercise.setReps ?? []
  if (perSet.length > 0) {
    return perSet
  }
  return Array.from({ length: Math.min(planExercise.setCount, MAX_SET_CELLS) }, () => planExercise.reps)
}

function ExerciseRows({ planExercise }: { planExercise: PlanExercise }) {
  const update = useUpdatePlanExercise()
  const remove = useDeletePlanExercise()
  const setRefs = useRef<(HTMLInputElement | null)[]>([])
  const weightRef = useRef<HTMLInputElement>(null)
  const restRef = useRef<HTMLInputElement>(null)
  const noteRef = useRef<HTMLInputElement>(null)

  const values = setCellValues(planExercise)
  // More sets than there are cells can only come from the older format; editing it here would
  // silently drop the extra sets, so it stays read-only until it is rewritten.
  const isTooLongForCells = planExercise.setCount > MAX_SET_CELLS && (planExercise.setReps ?? []).length === 0

  // Every field is read from the page each time, never from `planExercise`: the server replaces
  // the whole prescription, so a save carrying a stale sibling would quietly undo another edit.
  const save = () => {
    const setReps = SET_NUMBERS.map((number) => setRefs.current[number - 1]?.value.trim() ?? '')
    const weight = weightRef.current?.value.trim() ?? ''
    const rest = restRef.current?.value.trim() ?? ''
    const note = noteRef.current?.value.trim() ?? ''

    const unchanged =
      SET_NUMBERS.every((number) => setReps[number - 1] === (values[number - 1] ?? '')) &&
      weight === (planExercise.targetWeightKg === null ? '' : String(planExercise.targetWeightKg)) &&
      rest === (planExercise.restSeconds === null ? '' : String(planExercise.restSeconds)) &&
      note === (planExercise.note ?? '')
    if (unchanged) {
      return
    }

    update.mutate({
      planExerciseId: planExercise.id,
      input: {
        exerciseId: planExercise.exercise.id,
        setReps,
        targetWeightKg: weight === '' ? null : Number(weight),
        restSeconds: rest === '' ? null : Number(rest),
        note: note || null,
      },
    })
  }

  return (
    <>
      <tr className="border-t border-border/60">
        <td className="px-4 py-1.5">
          <span className="block text-sm font-semibold text-text-primary">{planExercise.exercise.name}</span>
          <span className="block text-[11px] text-text-muted">
            {muscleGroupLabel(planExercise.exercise.muscleGroup)}
          </span>
        </td>
        {isTooLongForCells ? (
          <td colSpan={MAX_SET_CELLS} className="px-1 py-1.5 text-center text-xs text-text-secondary">
            {planExercise.setCount} set × {planExercise.reps} (eski biçim)
          </td>
        ) : (
          SET_NUMBERS.map((number) => (
            <td key={number} className="px-1 py-1.5">
              <input
                ref={(element) => {
                  setRefs.current[number - 1] = element
                }}
                defaultValue={values[number - 1] ?? ''}
                onBlur={save}
                onKeyDown={blurOnEnter}
                maxLength={16}
                placeholder="—"
                aria-label={`${planExercise.exercise.name} set ${number} tekrar`}
                className={cellClasses}
              />
            </td>
          ))
        )}
        <td className="px-2 py-1.5 text-right">
          <button
            type="button"
            onClick={() => remove.mutate({ planExerciseId: planExercise.id })}
            disabled={remove.isPending}
            aria-label={`${planExercise.exercise.name} hareketini çıkar`}
            className="text-text-muted transition-colors hover:text-error disabled:opacity-50"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        </td>
      </tr>
      <tr>
        <td colSpan={MAX_SET_CELLS + 2} className="px-4 pb-3">
          <div className="grid grid-cols-2 gap-2 sm:grid-cols-[100px_100px_1fr]">
            <input
              ref={weightRef}
              defaultValue={planExercise.targetWeightKg ?? ''}
              onBlur={save}
              onKeyDown={blurOnEnter}
              type="number"
              step="0.5"
              min={0}
              placeholder="Hedef kg"
              aria-label={`${planExercise.exercise.name} hedef kilo`}
              className={smallFieldClasses}
            />
            <input
              ref={restRef}
              defaultValue={planExercise.restSeconds ?? ''}
              onBlur={save}
              onKeyDown={blurOnEnter}
              type="number"
              step="15"
              min={0}
              placeholder="Dinlenme sn"
              aria-label={`${planExercise.exercise.name} dinlenme saniyesi`}
              className={smallFieldClasses}
            />
            <input
              ref={noteRef}
              defaultValue={planExercise.note ?? ''}
              onBlur={save}
              onKeyDown={blurOnEnter}
              maxLength={2000}
              placeholder="Not (üye görür)"
              aria-label={`${planExercise.exercise.name} notu`}
              className={`${smallFieldClasses} col-span-2 sm:col-span-1`}
            />
          </div>
          <ExerciseVideoEditor exercise={planExercise.exercise} />
          {update.isError || remove.isError ? (
            <p className="mt-1 text-xs text-error">{(update.error ?? remove.error)?.message}</p>
          ) : null}
        </td>
      </tr>
    </>
  )
}

/** The "+" row at the foot of a day: pick an exercise, type the set targets, "+" or Enter. */
function AddExerciseRow({ dayId }: { dayId: string }) {
  const { data: exercises } = useExercises()
  const addExercise = useAddPlanExercise()
  const selectRef = useRef<HTMLSelectElement>(null)
  const setRefs = useRef<(HTMLInputElement | null)[]>([])

  const add = () => {
    const exerciseId = selectRef.current?.value ?? ''
    if (!exerciseId || addExercise.isPending) {
      selectRef.current?.focus()
      return
    }
    const setReps = SET_NUMBERS.map((number) => setRefs.current[number - 1]?.value.trim() ?? '')
    addExercise.mutate(
      { dayId, input: { exerciseId, setReps } },
      {
        onSuccess: () => {
          setRefs.current.forEach((input) => {
            if (input) input.value = ''
          })
          if (selectRef.current) selectRef.current.value = ''
          selectRef.current?.focus()
        },
      },
    )
  }

  return (
    <>
      <tr className="border-t border-dashed border-border bg-surface-elevated/40">
        <td className="px-3 py-1.5">
          <select
            ref={selectRef}
            defaultValue=""
            aria-label="Eklenecek hareket"
            className="h-9 w-full rounded-md border border-border bg-surface px-2 text-sm text-text-primary focus:border-primary focus:outline-none"
          >
            <option value="">Hareket seçin…</option>
            {exercises?.map((exercise) => (
              <option key={exercise.id} value={exercise.id}>
                {exercise.name} ({muscleGroupLabel(exercise.muscleGroup)})
              </option>
            ))}
          </select>
        </td>
        {SET_NUMBERS.map((number) => (
          <td key={number} className="px-1 py-1.5">
            <input
              ref={(element) => {
                setRefs.current[number - 1] = element
              }}
              onKeyDown={(event) => {
                if (event.key === 'Enter') {
                  event.preventDefault()
                  add()
                }
              }}
              maxLength={16}
              placeholder={number === 1 ? 'tekrar' : '—'}
              aria-label={`Yeni hareket set ${number} tekrar`}
              className={cellClasses}
            />
          </td>
        ))}
        <td className="px-2 py-1.5 text-right">
          <button
            type="button"
            onClick={add}
            disabled={addExercise.isPending}
            aria-label="Hareket ekle"
            className="inline-flex h-7 w-7 items-center justify-center rounded-full bg-primary text-canvas transition-colors hover:bg-primary-hover disabled:opacity-50"
          >
            <PlusIcon className="h-4 w-4" />
          </button>
        </td>
      </tr>
      {addExercise.isError ? (
        <tr>
          <td colSpan={MAX_SET_CELLS + 2} className="px-4 pb-2 text-xs text-error">
            {addExercise.error.message}
          </td>
        </tr>
      ) : null}
    </>
  )
}

function blurOnEnter(event: React.KeyboardEvent<HTMLInputElement>) {
  if (event.key === 'Enter') {
    event.preventDefault()
    event.currentTarget.blur()
  }
}
