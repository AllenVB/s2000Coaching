import { useState } from 'react'

import {
  useAddPlanDay,
  useAddPlanExercise,
  useDeletePlanDay,
  useDeletePlanExercise,
  useUpdatePlanExercise,
} from '@/api/trainingApi'
import { ExerciseImageEditor } from '@/components/training/ExerciseImage'
import { ExerciseVideoEditor } from '@/components/training/ExerciseVideoEditor'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { inputClasses } from '@/components/ui/FormField'
import { PlusIcon, TrashIcon } from '@/components/ui/icons'
import { MAX_DAYS, MAX_SETS, type PlanDay, type PlanExercise, type TrainingPlan } from '@/types/training'

/**
 * The programme as the coach builds it: "Gün 1" is there from the start and selected, the other
 * days come one at a time with the "+" (up to seven), and the selected day shows its exercises and
 * a form for the next one. An exercise is a name the coach types and its sets: "1.set" is there
 * already, a second "+" adds more (up to ten), and it can be saved with just the first.
 */
export function PlanEditor({ plan }: { plan: TrainingPlan }) {
  const addDay = useAddPlanDay()
  const [selectedId, setSelectedId] = useState<string | null>(null)

  // Whatever was selected may be gone (deleted) or not there yet; fall back to the first day.
  const selected = plan.days.find((day) => day.id === selectedId) ?? plan.days[0]
  const canAddDay = plan.days.length < MAX_DAYS

  const add = () => {
    if (!canAddDay || addDay.isPending) {
      return
    }
    addDay.mutate(
      { planId: plan.id, name: nextDayName(plan.days) },
      {
        onSuccess: (updated) => {
          // The new day is the last one; open it so its first exercise can be typed straight away.
          const newest = [...updated.days].sort((a, b) => b.dayOrder - a.dayOrder)[0]
          if (newest) {
            setSelectedId(newest.id)
          }
        },
      },
    )
  }

  return (
    <div className="space-y-4" data-training-editor>
      <div role="tablist" aria-label="Antrenman günleri" className="flex flex-wrap items-center gap-2">
        {plan.days.map((day) => (
          <button
            key={day.id}
            type="button"
            role="tab"
            aria-selected={selected?.id === day.id}
            onClick={() => setSelectedId(day.id)}
            className={`h-9 rounded-full border px-4 text-xs font-semibold uppercase tracking-wide transition-colors ${
              selected?.id === day.id
                ? 'border-primary bg-primary text-canvas'
                : 'border-border-strong bg-surface-elevated text-text-secondary hover:border-primary/60 hover:text-text-primary'
            }`}
          >
            {day.name}
          </button>
        ))}
        {canAddDay ? (
          <button
            type="button"
            onClick={add}
            disabled={addDay.isPending}
            aria-label={`${nextDayName(plan.days)} ekle`}
            title={`${nextDayName(plan.days)} ekle`}
            className="inline-flex h-9 w-9 items-center justify-center rounded-full border border-dashed border-border-strong text-text-primary transition-colors hover:border-primary hover:text-primary disabled:opacity-50"
          >
            <PlusIcon className="h-4 w-4" />
          </button>
        ) : (
          <span className="text-[11px] text-text-muted">En fazla {MAX_DAYS} gün</span>
        )}
      </div>
      {addDay.isError ? <p className="text-xs text-error">{addDay.error.message}</p> : null}

      {selected ? <DayPanel key={selected.id} day={selected} isOnlyDay={plan.days.length <= 1} /> : null}
    </div>
  )
}

/** The first "Gün N" (N from 1 to 7) that no day of the plan carries yet. */
function nextDayName(days: PlanDay[]): string {
  const taken = new Set(days.map((day) => day.name))
  for (let number = 1; number <= MAX_DAYS; number += 1) {
    if (!taken.has(`Gün ${number}`)) {
      return `Gün ${number}`
    }
  }
  return `Gün ${days.length + 1}`
}

function DayPanel({ day, isOnlyDay }: { day: PlanDay; isOnlyDay: boolean }) {
  const deleteDay = useDeletePlanDay()
  const [isConfirmingDelete, setConfirmingDelete] = useState(false)

  return (
    <section className="rounded-2xl border border-border bg-surface">
      <div className="flex items-center justify-between gap-3 border-b border-border bg-surface-elevated px-5 py-3">
        <h3 className="text-sm font-bold uppercase tracking-wider text-primary">{day.name}</h3>
        {isOnlyDay ? (
          <span className="text-[11px] text-text-muted">Son gün silinemez</span>
        ) : (
          <button
            type="button"
            onClick={() => setConfirmingDelete(true)}
            aria-label={`${day.name} gününü sil`}
            className="text-text-muted transition-colors hover:text-error"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        )}
      </div>

      <div className="space-y-4 p-5">
        {day.exercises.length === 0 ? (
          <p className="text-sm italic text-text-muted">Bu güne henüz egzersiz eklenmedi. Aşağıdan ekleyin.</p>
        ) : null}
        {day.exercises.map((planExercise) => (
          <ExerciseForm key={`${planExercise.id}-${exerciseVersion(planExercise)}`} planExercise={planExercise} />
        ))}

        <ExerciseForm dayId={day.id} />
      </div>

      {isConfirmingDelete ? (
        <ConfirmDialog
          title="Günü Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteDay.isPending}
          error={deleteDay.isError ? deleteDay.error.message : null}
          description={`"${day.name}" gününü ve içindeki egzersizleri silmek istediğinize emin misiniz? Bu güne girilmiş antrenman kayıtları da silinir.`}
          onCancel={() => setConfirmingDelete(false)}
          onConfirm={() => deleteDay.mutate({ dayId: day.id }, { onSuccess: () => setConfirmingDelete(false) })}
        />
      ) : null}
    </section>
  )
}

/** Changes whenever the saved prescription does, so a saved form restarts from what the server now holds. */
function exerciseVersion(planExercise: PlanExercise): string {
  return [
    planExercise.exercise.name,
    (planExercise.setReps ?? []).join('|'),
    planExercise.reps,
    planExercise.setCount,
    planExercise.targetWeightKg,
    planExercise.restSeconds,
    planExercise.note,
  ].join('~')
}

/**
 * What the set boxes start with. One written per set shows as it was; one written the older way
 * (a count and a single free-text reps) shows that text in each of its sets, which is what
 * "3 × 8-10" means. A brand new exercise starts with one empty set: "1.set".
 */
function initialSets(planExercise: PlanExercise | undefined): string[] {
  if (!planExercise) {
    return ['']
  }
  const perSet = planExercise.setReps ?? []
  if (perSet.length > 0) {
    return perSet
  }
  return Array.from({ length: Math.max(1, planExercise.setCount) }, () => planExercise.reps)
}

/** Saved by the exercise form: either a new exercise for `dayId` or a change to `planExercise`. */
function ExerciseForm({ dayId, planExercise }: { dayId?: string; planExercise?: PlanExercise }) {
  const addExercise = useAddPlanExercise()
  const updateExercise = useUpdatePlanExercise()
  const removeExercise = useDeletePlanExercise()

  const [name, setName] = useState(planExercise?.exercise.name ?? '')
  const [sets, setSets] = useState<string[]>(() => initialSets(planExercise))
  const [weight, setWeight] = useState(planExercise?.targetWeightKg != null ? String(planExercise.targetWeightKg) : '')
  const [rest, setRest] = useState(planExercise?.restSeconds != null ? String(planExercise.restSeconds) : '')
  const [note, setNote] = useState(planExercise?.note ?? '')
  const [error, setError] = useState<string | null>(null)
  const [isConfirmingRemove, setConfirmingRemove] = useState(false)

  // An exercise written in the older style with more sets than there are boxes for cannot be
  // edited here without losing sets, so it can only be removed.
  const tooLong = planExercise !== undefined && sets.length > MAX_SETS
  const isPending = addExercise.isPending || updateExercise.isPending

  const setRep = (index: number, value: string) => setSets((current) => current.map((rep, at) => (at === index ? value : rep)))
  const addSet = () => setSets((current) => (current.length < MAX_SETS ? [...current, ''] : current))
  const removeSet = (index: number) => setSets((current) => current.filter((_rep, at) => at !== index))

  const save = (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)

    const typedName = name.trim()
    if (typedName === '') {
      setError('Egzersiz adını yazın.')
      return
    }
    // Empty sets at the end are the ones the coach decided not to add; a gap before a filled one is a mistake.
    const trimmed = sets.map((rep) => rep.trim())
    const last = trimmed.reduce((found, rep, index) => (rep !== '' ? index : found), -1)
    if (last < 0) {
      setError('En az 1.set için tekrar yazın.')
      return
    }
    const gap = trimmed.slice(0, last).findIndex((rep) => rep === '')
    if (gap >= 0) {
      setError(`${gap + 1}.set boş ama sonraki setler dolu; boş seti silin ya da doldurun.`)
      return
    }
    const weightValue = weight.trim() === '' ? null : Number(weight.replace(',', '.'))
    const restValue = rest.trim() === '' ? null : Number(rest)
    if ((weightValue !== null && !(weightValue >= 0)) || (restValue !== null && !(restValue >= 0))) {
      setError('Hedef kg ve dinlenme saniyesi 0 ya da daha büyük bir sayı olmalı.')
      return
    }

    const input = {
      // The same name keeps the catalogue entry (and its video) by id; a new name finds or makes one.
      ...(planExercise && typedName === planExercise.exercise.name
        ? { exerciseId: planExercise.exercise.id }
        : { exerciseName: typedName }),
      setReps: trimmed.slice(0, last + 1),
      targetWeightKg: weightValue,
      restSeconds: restValue,
      note: note.trim() || null,
    }

    if (planExercise) {
      updateExercise.mutate({ planExerciseId: planExercise.id, input }, { onError: (failure) => setError(failure.message) })
    } else if (dayId) {
      addExercise.mutate(
        { dayId, input },
        {
          onSuccess: () => {
            setName('')
            setSets([''])
            setWeight('')
            setRest('')
            setNote('')
          },
          onError: (failure) => setError(failure.message),
        },
      )
    }
  }

  const isNew = planExercise === undefined

  return (
    <form
      onSubmit={save}
      noValidate
      className={`rounded-xl border p-4 ${isNew ? 'border-dashed border-primary/50 bg-primary/5' : 'border-border bg-surface-elevated/50'}`}
    >
      <div className="flex items-start gap-2">
        <label className="min-w-0 flex-1">
          <span className="mb-1 block text-[11px] font-semibold uppercase tracking-wider text-text-muted">
            {isNew ? 'Yeni egzersiz' : 'Egzersiz'}
          </span>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            type="text"
            maxLength={120}
            placeholder="Egzersiz adı (örn. Incline Dumbbell Press)"
            aria-label={isNew ? 'Yeni egzersiz adı' : 'Egzersiz adı'}
            className={inputClasses}
            disabled={tooLong}
          />
        </label>
        {planExercise ? (
          <button
            type="button"
            onClick={() => setConfirmingRemove(true)}
            aria-label={`${planExercise.exercise.name} egzersizini sil`}
            className="mt-6 text-text-muted transition-colors hover:text-error"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        ) : null}
      </div>

      {tooLong && planExercise ? (
        <p className="mt-3 text-xs text-text-secondary">
          {planExercise.setCount} set × {planExercise.reps} (eski biçim) — düzenlemek için egzersizi silip yeniden
          ekleyin.
        </p>
      ) : (
        <>
          <ul className="mt-3 space-y-2">
            {sets.map((rep, index) => (
              <li key={index} className="flex items-center gap-2">
                <span className="w-14 shrink-0 text-xs font-semibold text-text-secondary">{index + 1}.set</span>
                <input
                  value={rep}
                  onChange={(event) => setRep(index, event.target.value)}
                  type="text"
                  maxLength={16}
                  placeholder="Tekrar (örn. 8-10)"
                  aria-label={`${name.trim() || 'Egzersiz'} ${index + 1}.set tekrar`}
                  className={`${inputClasses} min-w-0 flex-1`}
                />
                {index > 0 ? (
                  <button
                    type="button"
                    onClick={() => removeSet(index)}
                    aria-label={`${index + 1}.seti sil`}
                    className="text-text-muted transition-colors hover:text-error"
                  >
                    <TrashIcon className="h-4 w-4" />
                  </button>
                ) : (
                  <span className="w-4" />
                )}
              </li>
            ))}
          </ul>

          <div className="mt-2">
            {sets.length < MAX_SETS ? (
              <button
                type="button"
                onClick={addSet}
                aria-label="Set ekle"
                className="inline-flex items-center gap-1.5 rounded-full border border-dashed border-border-strong px-3 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-primary hover:text-primary"
              >
                <PlusIcon className="h-3.5 w-3.5" />
                Set ekle ({sets.length + 1}.set)
              </button>
            ) : (
              <span className="text-[11px] text-text-muted">En fazla {MAX_SETS} set</span>
            )}
          </div>

          <div className="mt-4 grid grid-cols-2 gap-2 sm:grid-cols-[110px_130px_1fr]">
            <input
              value={weight}
              onChange={(event) => setWeight(event.target.value)}
              type="text"
              inputMode="decimal"
              placeholder="Hedef kg"
              aria-label="Hedef kilo"
              className={inputClasses}
            />
            <input
              value={rest}
              onChange={(event) => setRest(event.target.value)}
              type="text"
              inputMode="numeric"
              placeholder="Dinlenme sn"
              aria-label="Dinlenme saniyesi"
              className={inputClasses}
            />
            <input
              value={note}
              onChange={(event) => setNote(event.target.value)}
              type="text"
              maxLength={2000}
              placeholder="Not (üye görür)"
              aria-label="Egzersiz notu"
              className={`${inputClasses} col-span-2 sm:col-span-1`}
            />
          </div>

          {error ? (
            <p role="alert" className="mt-3 text-xs text-error">
              {error}
            </p>
          ) : null}

          <div className="mt-4">
            <Button type="submit" size="sm" disabled={isPending}>
              {isPending ? 'Kaydediliyor…' : isNew ? 'Egzersizi Kaydet' : 'Güncelle'}
            </Button>
          </div>
        </>
      )}

      {planExercise ? (
        <>
          <ExerciseVideoEditor exercise={planExercise.exercise} />
          <ExerciseImageEditor exercise={planExercise.exercise} />
        </>
      ) : null}

      {isConfirmingRemove && planExercise ? (
        <ConfirmDialog
          title="Egzersizi Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={removeExercise.isPending}
          error={removeExercise.isError ? removeExercise.error.message : null}
          description={`"${planExercise.exercise.name}" egzersizini silmek istediğinize emin misiniz? Bu egzersize girilmiş set kayıtları da silinir.`}
          onCancel={() => setConfirmingRemove(false)}
          onConfirm={() =>
            removeExercise.mutate({ planExerciseId: planExercise.id }, { onSuccess: () => setConfirmingRemove(false) })
          }
        />
      ) : null}
    </form>
  )
}
