import { useMemo, useState } from 'react'

import { useSaveWorkoutLog } from '@/api/trainingApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import { formatDate, todayIso } from '@/lib/date'
import type { TrainingPlan, WorkoutLog } from '@/types/training'

interface Cell {
  reps: string
  weightKg: string
}

/** The date `days` away from an ISO date, in the same form; worked in UTC so a clock change cannot move it. */
function shiftDay(iso: string, days: number): string {
  const [year, month, day] = iso.split('-').map(Number)
  return new Date(Date.UTC(year, month - 1, day + days)).toISOString().slice(0, 10)
}

/**
 * The member's workout table. The rows are the exercises of a day of the programme the coach assigned
 * and the columns its sets ("1.set", "2.set" …); the member only fills in the number of repetitions (and,
 * if they like, the weight) under each set. A past day is entered by choosing its date, and a date that
 * already has a session opens it to correct.
 */
export function WorkoutTableLogger({
  plan,
  logs,
  dayId,
  date,
  onChange,
}: {
  plan: TrainingPlan
  logs: WorkoutLog[]
  dayId: string
  date: string
  onChange: (next: { dayId: string; date: string }) => void
}) {
  const today = todayIso()
  const day = plan.days.find((candidate) => candidate.id === dayId) ?? plan.days[0]
  const existing = useMemo(
    () => (day ? logs.find((log) => log.dayId === day.id && log.performedOn === date) : undefined),
    [logs, day, date],
  )

  if (!day) {
    return null
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Antrenman Tablosu</h2>
      <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
        Günü ve tarihi seçin; her setin altına yaptığınız tekrar sayısını yazın. Geçmiş bir günü girmek için
        o tarihi seçin. Aynı gün ve tarihe tekrar kaydederseniz o antrenman güncellenir.
      </p>

      <div className="mt-4 flex flex-wrap items-end gap-3">
        <div className="min-w-[160px] flex-1">
          <Field label="Antrenman günü">
            <select value={day.id} onChange={(event) => onChange({ dayId: event.target.value, date })} className={inputClasses}>
              {plan.days.map((candidate) => (
                <option key={candidate.id} value={candidate.id}>
                  {candidate.name}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <div className="min-w-[160px] flex-1">
          <Field label="Hangi tarihte yaptınız?">
            <input
              type="date"
              value={date}
              max={today}
              aria-label="Antrenman tarihi"
              onChange={(event) => event.target.value && onChange({ dayId: day.id, date: event.target.value })}
              className={inputClasses}
            />
          </Field>
        </div>
        <div className="flex gap-2 pb-0.5">
          {[
            { label: 'Bugün', value: today },
            { label: 'Dün', value: shiftDay(today, -1) },
          ].map((option) => (
            <button
              key={option.label}
              type="button"
              onClick={() => onChange({ dayId: day.id, date: option.value })}
              className="h-11 rounded-full border border-border-strong px-3.5 text-[11px] font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-primary/60 hover:text-text-primary"
            >
              {option.label}
            </button>
          ))}
        </div>
      </div>

      {existing ? (
        <p className="mt-3 text-xs text-warning">
          {formatDate(date)} tarihinde bu gün için kayıtlı bir antrenmanınız var; değerler onunla dolu, kaydederseniz
          güncellenir.
        </p>
      ) : null}

      {/* Keyed on the day and date, so another one starts from its own values (and saving keeps the form as it is). */}
      <LoggerTable key={`${day.id}|${date}`} day={day} date={date} existing={existing} />
    </section>
  )
}

function LoggerTable({ day, date, existing }: { day: TrainingPlan['days'][number]; date: string; existing: WorkoutLog | undefined }) {
  const save = useSaveWorkoutLog()
  const columns = Math.max(1, ...day.exercises.map((planExercise) => planExercise.setCount))

  const [cells, setCells] = useState<Record<string, Cell[]>>(() => {
    const seeded: Record<string, Cell[]> = {}
    day.exercises.forEach((planExercise) => {
      seeded[planExercise.id] = Array.from({ length: planExercise.setCount }, (_unused, index) => {
        const recorded = existing?.sets.find(
          (set) => set.planExerciseId === planExercise.id && set.setNumber === index + 1,
        )
        return {
          reps: recorded ? String(recorded.reps) : '',
          weightKg: recorded?.weightKg != null ? String(recorded.weightKg) : '',
        }
      })
    })
    return seeded
  })
  const [error, setError] = useState<string | null>(null)

  const change = (exerciseId: string, index: number, patch: Partial<Cell>) =>
    setCells((current) => ({
      ...current,
      [exerciseId]: current[exerciseId].map((cell, at) => (at === index ? { ...cell, ...patch } : cell)),
    }))

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    const sets = []
    for (const planExercise of day.exercises) {
      for (const [index, cell] of (cells[planExercise.id] ?? []).entries()) {
        // A set with no repetitions was not done; leave it out rather than storing a zero.
        if (cell.reps.trim() === '') {
          continue
        }
        const reps = Number(cell.reps)
        const weight = cell.weightKg.trim() === '' ? null : Number(cell.weightKg.replace(',', '.'))
        if (!Number.isInteger(reps) || reps < 0 || reps > 1000 || (weight !== null && !(weight >= 0 && weight <= 999))) {
          setError(`${planExercise.exercise.name} ${index + 1}. set için geçerli bir tekrar sayısı (ve kilo) yazın.`)
          return
        }
        sets.push({ planExerciseId: planExercise.id, setNumber: index + 1, reps, weightKg: weight })
      }
    }
    if (sets.length === 0) {
      setError('En az bir setin tekrar sayısını yazın.')
      return
    }
    save.mutate({ dayId: day.id, performedOn: date, note: existing?.note ?? null, sets })
  }

  if (day.exercises.length === 0) {
    return <p className="mt-4 text-sm italic text-text-muted">Bu güne henüz egzersiz eklenmedi.</p>
  }

  return (
    <form onSubmit={submit} noValidate className="mt-5">
      <div className="-mx-2 overflow-x-auto">
        <table className="w-full border-collapse text-sm" style={{ minWidth: 150 + columns * 92 }}>
          <thead>
            <tr className="border-b border-border text-[11px] font-semibold uppercase tracking-wider text-text-muted">
              <th scope="col" className="sticky left-0 z-10 bg-surface px-2 py-2 text-left">
                Egzersiz
              </th>
              {Array.from({ length: columns }, (_unused, index) => (
                <th key={index} scope="col" className="px-1 py-2 text-center">
                  {index + 1}.set
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {day.exercises.map((planExercise) => (
              <tr key={planExercise.id} className="border-b border-border/60">
                <th scope="row" className="sticky left-0 z-10 w-[150px] bg-surface px-2 py-2 text-left align-top font-medium text-text-primary">
                  {planExercise.exercise.name}
                </th>
                {Array.from({ length: columns }, (_unused, index) => {
                  const cell = cells[planExercise.id]?.[index]
                  if (!cell) {
                    return (
                      <td key={index} className="px-1 py-2 text-center text-text-muted">
                        —
                      </td>
                    )
                  }
                  const target = planExercise.setReps?.[index] || planExercise.reps
                  return (
                    <td key={index} className="px-1 py-2 align-top">
                      <input
                        value={cell.reps}
                        onChange={(event) => change(planExercise.id, index, { reps: event.target.value })}
                        type="text"
                        inputMode="numeric"
                        autoComplete="off"
                        placeholder={target}
                        aria-label={`${planExercise.exercise.name} ${index + 1}. set tekrar`}
                        className="h-10 w-full min-w-[64px] rounded-lg border border-border bg-surface px-2 text-center text-sm text-text-primary focus:border-primary focus:outline-none"
                      />
                      <input
                        value={cell.weightKg}
                        onChange={(event) => change(planExercise.id, index, { weightKg: event.target.value })}
                        type="text"
                        inputMode="decimal"
                        autoComplete="off"
                        placeholder="kg"
                        aria-label={`${planExercise.exercise.name} ${index + 1}. set kilo`}
                        className="mt-1 h-8 w-full min-w-[64px] rounded-lg border border-transparent bg-surface-elevated px-2 text-center text-xs text-text-secondary focus:border-primary focus:outline-none"
                      />
                    </td>
                  )
                })}
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="mt-2 text-[11px] text-text-muted">Kutudaki soluk sayı koçunuzun hedefidir; alttaki küçük kutu isteğe bağlı kiloyu içindir.</p>

      {error ? (
        <p role="alert" className="mt-3 text-xs text-error">
          {error}
        </p>
      ) : null}
      {save.isError ? <p className="mt-3 text-xs text-error">Kaydedilemedi: {save.error.message}</p> : null}
      {save.isSuccess && 'queued' in save.data ? (
        <p className="mt-3 rounded-xl border border-warning/40 bg-warning/10 p-3.5 text-xs leading-relaxed text-warning">
          Bağlantı yok: antrenmanınız telefonunuza kaydedildi ve internet gelince otomatik gönderilecek.
        </p>
      ) : null}
      {save.isSuccess && !('queued' in save.data) ? (
        <p className="mt-3 text-xs text-success">Antrenmanınız kaydedildi.</p>
      ) : null}

      <Button type="submit" className="mt-4" disabled={save.isPending}>
        {save.isPending ? 'Kaydediliyor…' : existing ? 'Güncelle' : 'Kaydet'}
      </Button>
    </form>
  )
}
