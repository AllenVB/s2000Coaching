import { useMemo, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router-dom'

import { useMyTrainingPlan, useMyTrainingStats, useMyWorkoutLogs, useSaveWorkoutLog } from '@/api/trainingApi'
import { RestTimer } from '@/components/training/RestTimer'
import { MemberExerciseImage } from '@/components/training/ExerciseImage'
import { ExerciseVideo } from '@/components/training/ExerciseVideo'
import { WeightStrip } from '@/components/training/WeightStrip'
import { Button } from '@/components/ui/Button'
import { inputClasses, textareaClasses } from '@/components/ui/FormField'
import { ArrowLeftIcon, CheckIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'
import type { PlanExercise } from '@/types/training'
import { todayIso } from '@/lib/date'

interface SetEntry {
  reps: string
  weightKg: string
  rpe: string
}

const today = todayIso

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
  const { data: logs } = useMyWorkoutLogs()

  // "?tarih=2026-10-12&kayit=1" opens the day already recording that date — how the history page
  // sends the member to add or correct a session they did earlier.
  const [searchParams] = useSearchParams()
  const requestedDate = searchParams.get('tarih')

  const [entries, setEntries] = useState<Record<string, SetEntry[]> | null>(null)
  const [performedOn, setPerformedOn] = useState(() =>
    requestedDate && /^\d{4}-\d{2}-\d{2}$/.test(requestedDate) && requestedDate <= today() ? requestedDate : today(),
  )
  // The plan is for reading: the sets below are the coach's. The boxes to write what was actually
  // lifted only open when the member chooses to record a session.
  const [logging, setLogging] = useState(() => searchParams.get('kayit') === '1')

  // A session already recorded for this day and date: opening that date edits it rather than starting over.
  const existingLog = useMemo(
    () => (day ? logs?.find((log) => log.dayId === day.id && log.performedOn === performedOn) : undefined),
    [logs, day, performedOn],
  )
  // What the member typed; until they type, the note of the session already recorded for that date.
  const [typedNote, setTypedNote] = useState<string | null>(null)
  const note = typedNote ?? existingLog?.note ?? ''

  // Seeded from the plan: the prescribed set count, filled with the session already recorded for that
  // date, else with what was lifted last time, else with the target.
  const initialEntries = useMemo(() => {
    if (!day) {
      return null
    }
    const seeded: Record<string, SetEntry[]> = {}
    day.exercises.forEach((planExercise) => {
      seeded[planExercise.id] = Array.from({ length: planExercise.setCount }, (_unused, index) => {
        const recorded = existingLog?.sets.find(
          (set) => set.planExerciseId === planExercise.id && set.setNumber === index + 1,
        )
        if (existingLog) {
          return {
            reps: recorded ? String(recorded.reps) : '',
            weightKg: recorded?.weightKg != null ? String(recorded.weightKg) : '',
            rpe: recorded?.rpe != null ? String(recorded.rpe) : '',
          }
        }
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
  }, [day, existingLog])

  const chooseDate = (next: string) => {
    setPerformedOn(next)
    // Another date is another session: start from what that date holds, not from what was typed for this one.
    setEntries(null)
    setTypedNote(null)
  }

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
            ? `Son yapılış: ${formatDate(day.lastPerformedOn)}.`
            : 'Henüz yapılmadı.'}
          {logging
            ? ' Alanlar o tarihte kayıtlı seansla, yoksa son seansla ya da hedefle dolu.'
            : ' Programı koçunuz hazırladı; burada yalnızca görüntülenir.'}
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

            <MemberExerciseImage exercise={planExercise.exercise} />

            {planExercise.note ? (
              <p className="mb-3 text-xs leading-relaxed text-text-secondary">{planExercise.note}</p>
            ) : null}

            <WeightStrip
              history={planExercise.weightHistory ?? []}
              performedOn={performedOn}
              liveWeightKg={
                // Until the member changes something the fields only echo last session, which is
                // not a weight they lifted today.
                isUntouched(values[planExercise.id], initialEntries?.[planExercise.id])
                  ? null
                  : liveTopWeight(values[planExercise.id] ?? [])
              }
            />

            <SuggestionLine suggestion={stats?.exercises.find((s) => s.exerciseId === planExercise.exercise.id)?.suggestion} />

            <div className="mb-3">
              <ExerciseVideo exercise={planExercise.exercise} />
            </div>

            {!logging ? (
              <ul className="space-y-1.5">
                {Array.from({ length: planExercise.setCount }, (_unused, index) => (
                  <li key={index} className="flex items-center gap-3 text-sm">
                    <span className="w-12 shrink-0 text-xs font-semibold uppercase tracking-wider text-text-muted">
                      {index + 1}. set
                    </span>
                    <span className="text-text-primary">{repsText(planExercise.setReps?.[index] || planExercise.reps)}</span>
                  </li>
                ))}
              </ul>
            ) : null}

            <ul className={`space-y-2 ${logging ? '' : 'hidden'}`} aria-hidden={!logging}>
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
                    placeholder={planExercise.setReps?.[index] ? `hedef ${planExercise.setReps[index]}` : 'tekrar'}
                    aria-label={`${planExercise.exercise.name} ${index + 1}. set tekrar`}
                    className={`${inputClasses} min-w-0 flex-1`}
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
                    className={`${inputClasses} min-w-0 flex-1`}
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
                    className={`${inputClasses.replace('w-full', 'w-16')} shrink-0`}
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

        {!logging ? (
          <Button type="button" variant="secondary" className="w-full" onClick={() => setLogging(true)}>
            Antrenmanı Kaydet
          </Button>
        ) : null}

        <section className={`rounded-2xl border border-border bg-surface p-5 ${logging ? '' : 'hidden'}`}>
          <label className="block">
            <span className="mb-1.5 block text-xs font-medium text-text-secondary">
              Bu antrenmanı hangi gün yaptınız?
            </span>
            <input
              value={performedOn}
              onChange={(event) => event.target.value && chooseDate(event.target.value)}
              type="date"
              max={today()}
              aria-label="Antrenman tarihi"
              className={`${inputClasses} max-w-[200px]`}
            />
          </label>
          <div className="mt-2 flex flex-wrap gap-2">
            {[
              { label: 'Bugün', date: today() },
              { label: 'Dün', date: shiftDay(today(), -1) },
            ].map((option) => (
              <button
                key={option.label}
                type="button"
                onClick={() => chooseDate(option.date)}
                className="rounded-full border border-border-strong px-3 py-1 text-[11px] font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-primary/60 hover:text-text-primary"
              >
                {option.label}
              </button>
            ))}
          </div>
          {existingLog ? (
            <p className="mt-3 text-xs text-warning">
              {formatDate(performedOn)} tarihinde bu gün için kayıtlı bir antrenmanınız var; kaydederseniz güncellenir.
            </p>
          ) : null}
          <label className="mt-4 block">
            <span className="mb-1.5 block text-xs font-medium text-text-secondary">Not</span>
            <textarea
              value={note}
              onChange={(event) => setTypedNote(event.target.value)}
              rows={2}
              placeholder="Nasıl geçti, ağrı, enerji…"
              className={textareaClasses}
            />
          </label>
        </section>

        {saveLog.isError ? <p className="text-xs text-error">Kaydedilemedi: {saveLog.error.message}</p> : null}
        {saveLog.isSuccess && 'queued' in saveLog.data ? (
          <p className="rounded-xl border border-warning/40 bg-warning/10 p-3.5 text-xs leading-relaxed text-warning">
            Bağlantı yok: antrenmanınız telefonunuza kaydedildi ve internet gelince otomatik gönderilecek.
            Uygulamayı bu cihazda açık tutmanız yeterli.
          </p>
        ) : null}
        {saveLog.isSuccess && !('queued' in saveLog.data) ? (
          <p className="flex items-center gap-1.5 text-xs text-success">
            <CheckIcon className="h-3.5 w-3.5" /> Antrenmanınız kaydedildi.
          </p>
        ) : null}

        {logging ? (
          <Button type="submit" className="w-full" disabled={saveLog.isPending || day.exercises.length === 0}>
            {saveLog.isPending ? 'Kaydediliyor…' : 'Antrenmanı Kaydet'}
          </Button>
        ) : null}
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

/** "12" and "8-10" are repetitions; "60 sn" or "AMRAP" already say what they are. */
function repsText(value: string): string {
  return /^[\d\s\-–]+$/.test(value) ? `${value} tekrar` : value
}

/** The date `days` away from an ISO date, in the same form. Worked in UTC so a clock change cannot move it. */
function shiftDay(iso: string, days: number): string {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10)
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

function isUntouched(current: SetEntry[] | undefined, initial: SetEntry[] | undefined): boolean {
  return JSON.stringify(current ?? []) === JSON.stringify(initial ?? [])
}

/**
 * The heaviest weight among the sets that would be saved (the ones with reps), which is what
 * today's slot in the weight strip shows; null while no such set has a weight.
 */
function liveTopWeight(entries: SetEntry[]): number | null {
  const weights = entries
    .filter((entry) => entry.reps.trim() !== '' && entry.weightKg.trim() !== '')
    .map((entry) => Number(entry.weightKg))
    .filter((weight) => Number.isFinite(weight))
  return weights.length > 0 ? Math.max(...weights) : null
}

function targetAsText(planExercise: PlanExercise): string {
  return planExercise.targetWeightKg !== null ? String(planExercise.targetWeightKg) : ''
}
