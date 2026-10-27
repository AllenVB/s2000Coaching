import { useMemo, useState } from 'react'
import { Link, useParams } from 'react-router-dom'

import { useMyTrainingPlan, useMyTrainingStats, useSaveWorkoutLog } from '@/api/trainingApi'
import { RestTimer } from '@/components/training/RestTimer'
import { ExerciseVideo } from '@/components/training/ExerciseVideo'
import { Button } from '@/components/ui/Button'
import { inputClasses, textareaClasses } from '@/components/ui/FormField'
import { ArrowLeftIcon, CheckIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'
import type { PlanExercise } from '@/types/training'

interface SetEntry {
  reps: string
  weightKg: string
  rpe: string
}

const today = () => new Date().toISOString().slice(0, 10)

/**
 * The screen used in the gym: every exercise and every set on one page, pre-filled with last
 * session's numbers so the member only edits what changed.
 */
export function MemberWorkoutDayPage() {
  const { dayId } = useParams<{ dayId: string }>()
  const { data: plan, isLoading } = useMyTrainingPlan()
  const saveLog = useSaveWorkoutLog()
  const { data: stats } = useMyTrainingStats()

  const day = plan?.days.find((candidate) => candidate.id === dayId)

  const [entries, setEntries] = useState<Record<string, SetEntry[]> | null>(null)
  const [note, setNote] = useState('')
  const [performedOn, setPerformedOn] = useState(today)

  // Seeded once from the plan: prescribed set count, pre-filled with what was lifted last time.
  const initialEntries = useMemo(() => {
    if (!day) {
      return null
    }
    const seeded: Record<string, SetEntry[]> = {}
    day.exercises.forEach((planExercise) => {
      seeded[planExercise.id] = Array.from({ length: planExercise.setCount }, (_unused, index) => {
        const previous = planExercise.lastPerformed.find((set) => set.setNumber === index + 1)
        return {
          reps: previous ? String(previous.reps) : '',
          weightKg: previous?.weightKg != null ? String(previous.weightKg) : targetAsText(planExercise),
          // Effort is a fresh judgement each session; carrying last time's number over would
          // record a rating the member never gave.
          rpe: '',
        }
      })
    })
    return seeded
  }, [day])

  if (isLoading) {
    return <div className="h-64 animate-pulse rounded-2xl border border-border bg-surface" />
  }

  if (!day) {
    return (
      <>
        <BackLink />
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          Bu antrenman günü planınızda bulunamadı.
        </p>
      </>
    )
  }

  const values = entries ?? initialEntries ?? {}

  const setValue = (planExerciseId: string, index: number, field: keyof SetEntry, value: string) => {
    setEntries((current) => {
      const base = current ?? initialEntries ?? {}
      const rows = [...(base[planExerciseId] ?? [])]
      rows[index] = { ...rows[index], [field]: value }
      return { ...base, [planExerciseId]: rows }
    })
  }

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const sets = day.exercises.flatMap((planExercise) =>
      (values[planExercise.id] ?? [])
        // A set with no reps was not performed; leave it out rather than storing a zero.
        .map((entry, index) => ({ entry, setNumber: index + 1 }))
        .filter(({ entry }) => entry.reps.trim() !== '')
        .map(({ entry, setNumber }) => ({
          planExerciseId: planExercise.id,
          setNumber,
          reps: Number(entry.reps),
          weightKg: entry.weightKg.trim() === '' ? null : Number(entry.weightKg),
          rpe: entry.rpe.trim() === '' ? null : Number(entry.rpe),
        })),
    )

    saveLog.mutate({ dayId: day.id, performedOn, note: note.trim() || null, sets })
  }

  return (
    <>
      <BackLink />

      <div className="mb-6">
        <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary sm:text-3xl">
          {day.name}
        </h1>
        <p className="mt-2 text-sm text-text-secondary">
          {day.lastPerformedOn
            ? `Son yapılış: ${formatDate(day.lastPerformedOn)} — alanlar o seansın değerleriyle dolu.`
            : 'İlk kez yapıyorsunuz; alanlar hedef değerlerle dolu.'}
        </p>
      </div>

      <form className="space-y-4" onSubmit={submit}>
        {day.exercises.map((planExercise) => (
          <section key={planExercise.id} className="rounded-2xl border border-border bg-surface p-5">
            <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2">
              <h2 className="font-semibold text-text-primary">{planExercise.exercise.name}</h2>
              <span className="text-xs text-text-muted">
                Hedef: {planExercise.setCount} × {planExercise.reps}
                {planExercise.targetWeightKg !== null ? ` @ ${planExercise.targetWeightKg} kg` : ''}
              </span>
            </div>

            {planExercise.note ? (
              <p className="mb-3 text-xs leading-relaxed text-text-secondary">{planExercise.note}</p>
            ) : null}

            <SuggestionLine suggestion={stats?.exercises.find((s) => s.exerciseId === planExercise.exercise.id)?.suggestion} />

            <div className="mb-3">
              <ExerciseVideo exercise={planExercise.exercise} />
            </div>

            <ul className="space-y-2">
              {(values[planExercise.id] ?? []).map((entry, index) => (
                <li key={index} className="flex items-center gap-2">
                  <span className="w-12 shrink-0 text-xs font-semibold uppercase tracking-wider text-text-muted">
                    {index + 1}. set
                  </span>
                  <input
                    value={entry.reps}
                    onChange={(event) => setValue(planExercise.id, index, 'reps', event.target.value)}
                    type="number"
                    inputMode="numeric"
                    min={0}
                    placeholder="tekrar"
                    aria-label={`${planExercise.exercise.name} ${index + 1}. set tekrar`}
                    className={`${inputClasses} flex-1`}
                  />
                  <input
                    value={entry.weightKg}
                    onChange={(event) => setValue(planExercise.id, index, 'weightKg', event.target.value)}
                    type="number"
                    inputMode="decimal"
                    step="0.5"
                    min={0}
                    placeholder="kg"
                    aria-label={`${planExercise.exercise.name} ${index + 1}. set kilo`}
                    className={`${inputClasses} flex-1`}
                  />
                  <input
                    value={entry.rpe}
                    onChange={(event) => setValue(planExercise.id, index, 'rpe', event.target.value)}
                    type="number"
                    inputMode="decimal"
                    step="0.5"
                    min={1}
                    max={10}
                    placeholder="RPE"
                    aria-label={`${planExercise.exercise.name} ${index + 1}. set zorluk (1-10)`}
                    className={`${inputClasses} w-20 shrink-0`}
                  />
                </li>
              ))}
            </ul>

            {planExercise.restSeconds ? (
              <div className="mt-3">
                <RestTimer seconds={planExercise.restSeconds} />
              </div>
            ) : null}
          </section>
        ))}

        <section className="rounded-2xl border border-border bg-surface p-5">
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-text-secondary">Tarih</span>
            <input
              value={performedOn}
              onChange={(event) => setPerformedOn(event.target.value)}
              type="date"
              max={today()}
              className={`${inputClasses} max-w-[200px]`}
            />
          </label>
          <label className="mt-4 block">
            <span className="mb-1.5 block text-xs font-medium text-text-secondary">Not</span>
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              rows={2}
              placeholder="Nasıl geçti, ağrı, enerji…"
              className={textareaClasses}
            />
          </label>
        </section>

        {saveLog.isError ? <p className="text-xs text-error">Kaydedilemedi: {saveLog.error.message}</p> : null}
        {saveLog.isSuccess ? (
          <p className="flex items-center gap-1.5 text-xs text-success">
            <CheckIcon className="h-3.5 w-3.5" /> Antrenmanınız kaydedildi.
          </p>
        ) : null}

        <Button type="submit" className="w-full" disabled={saveLog.isPending || day.exercises.length === 0}>
          {saveLog.isPending ? 'Kaydediliyor…' : 'Antrenmanı Kaydet'}
        </Button>
      </form>
    </>
  )
}

/** What to do next time, with the reasoning; silent when there is nothing to base it on. */
function SuggestionLine({
  suggestion,
}: {
  suggestion: { kind: 'INCREASE' | 'HOLD' | 'INSUFFICIENT'; text: string } | undefined
}) {
  if (!suggestion || suggestion.kind === 'INSUFFICIENT') {
    return null
  }
  return (
    <p
      className={`mb-3 rounded-lg px-3 py-2 text-xs leading-relaxed ${
        suggestion.kind === 'INCREASE' ? 'bg-success/10 text-success' : 'bg-surface-elevated text-text-secondary'
      }`}
    >
      {suggestion.text}
    </p>
  )
}

function BackLink() {
  return (
    <Link
      to="/uye/antrenman"
      className="mb-4 inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-text-muted transition-colors hover:text-text-primary"
    >
      <ArrowLeftIcon className="h-3.5 w-3.5" />
      Antrenman
    </Link>
  )
}

function targetAsText(planExercise: PlanExercise): string {
  return planExercise.targetWeightKg !== null ? String(planExercise.targetWeightKg) : ''
}
