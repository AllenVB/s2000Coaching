import { useMemo, useState } from 'react'

import { useMyNutritionPlan } from '@/api/nutritionApi'
import { useMyWorkoutLogs } from '@/api/trainingApi'
import { useDeleteMyIntake, useMyIntake, useSaveMyIntake } from '@/api/intakeApi'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Field, inputClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'
import { formatDate, todayIso } from '@/lib/date'
import { formatAmount } from '@/lib/weeklyNutrition'
import { targetsOf } from '@/types/nutrition'
import { parseFigure } from '@/types/monthlyRecord'
import { DAY_TYPES, type Intake, type IntakeDayType } from '@/types/intake'

function numberOrNull(text: string): number | null | 'invalid' {
  return parseFigure(text)
}

/** The day's calories, macros and water, typed in by the member; a past day is entered by its date. */
export function DailyIntakeLog() {
  const { data: intakes } = useMyIntake()
  const [date, setDate] = useState(todayIso())
  const existing = useMemo(() => intakes?.find((row) => row.consumedOn === date), [intakes, date])
  const [deleting, setDeleting] = useState<Intake | null>(null)
  const remove = useDeleteMyIntake()

  return (
    <div className="space-y-6">
      {/* Keyed on the date, so a day opens with its own values; saving leaves the form as it is. */}
      {intakes ? (
        <IntakeForm key={date} date={date} onDate={setDate} existing={existing} />
      ) : (
        <div className="h-48 animate-pulse rounded-2xl border border-border bg-surface" />
      )}

      {intakes && intakes.length > 0 ? (
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">Girdiğim Günler</h2>
          <div className="-mx-2 overflow-x-auto">
            <table className="w-full min-w-[520px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-border text-[11px] font-semibold uppercase tracking-wider text-text-muted">
                  <th className="px-2 py-2 text-left">Tarih</th>
                  <th className="px-2 py-2 text-left">Gün</th>
                  <th className="px-2 py-2 text-right">Kalori</th>
                  <th className="px-2 py-2 text-right">K / Y / P (g)</th>
                  <th className="px-2 py-2 text-right">Su</th>
                  <th className="w-16 px-2" />
                </tr>
              </thead>
              <tbody>
                {intakes.map((row) => (
                  <tr key={row.id} className="border-b border-border/60">
                    <td className="px-2 py-2 text-text-primary">{formatDate(row.consumedOn)}</td>
                    <td className="px-2 py-2 text-text-secondary">
                      {DAY_TYPES.find((type) => type.value === row.dayType)?.label.replace(' günü', '') ?? '—'}
                    </td>
                    <td className="px-2 py-2 text-right text-text-primary">{formatAmount(row.calories)} kcal</td>
                    <td className="px-2 py-2 text-right text-text-secondary">
                      {[row.carbsG, row.fatG, row.proteinG].map((value) => value ?? '—').join(' / ')}
                    </td>
                    <td className="px-2 py-2 text-right text-text-secondary">
                      {row.waterLiters !== null ? `${formatAmount(row.waterLiters)} L` : '—'}
                    </td>
                    <td className="px-2 py-2 text-right">
                      <button
                        type="button"
                        onClick={() => setDate(row.consumedOn)}
                        className="mr-3 text-xs font-semibold uppercase tracking-wide text-primary hover:underline"
                        aria-label={`${formatDate(row.consumedOn)} kaydını düzenle`}
                      >
                        Düzenle
                      </button>
                      <button
                        type="button"
                        onClick={() => setDeleting(row)}
                        aria-label={`${formatDate(row.consumedOn)} kaydını sil`}
                        className="align-middle text-text-muted transition-colors hover:text-error"
                      >
                        <TrashIcon className="inline h-4 w-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </section>
      ) : null}

      {deleting ? (
        <ConfirmDialog
          title="Kaydı Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={remove.isPending}
          error={remove.isError ? remove.error.message : null}
          description={`${formatDate(deleting.consumedOn)} tarihli beslenme kaydını silmek istediğinize emin misiniz?`}
          onCancel={() => setDeleting(null)}
          onConfirm={() => remove.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        />
      ) : null}
    </div>
  )
}

function IntakeForm({ date, onDate, existing }: { date: string; onDate: (date: string) => void; existing: Intake | undefined }) {
  const save = useSaveMyIntake()
  const [calories, setCalories] = useState(existing ? String(existing.calories) : '')
  const [carbs, setCarbs] = useState(existing?.carbsG != null ? String(existing.carbsG) : '')
  const [fat, setFat] = useState(existing?.fatG != null ? String(existing.fatG) : '')
  const [protein, setProtein] = useState(existing?.proteinG != null ? String(existing.proteinG) : '')
  const [water, setWater] = useState(existing?.waterLiters != null ? String(existing.waterLiters) : '')
  const { data: workoutLogs } = useMyWorkoutLogs()
  // Suggested from the workout log, but the member decides: nothing is saved unless they pick one.
  const suggested: IntakeDayType | null = workoutLogs?.some((log) => log.performedOn === date) ? 'ANTRENMAN' : null
  const [dayType, setDayType] = useState<IntakeDayType | ''>(existing?.dayType ?? '')
  const [error, setError] = useState<string | null>(null)
  const today = todayIso()
  const { data: plan } = useMyNutritionPlan()
  // The target follows the chosen day type: a training day and a rest day have different calories.
  const targets = plan && dayType !== '' ? targetsOf(plan, dayType === 'DINLENME' ? 'REST' : 'TRAINING') : null
  const macroKcal = macroCalories(carbs, fat, protein)
  // What was eaten, by macros when they are filled in (4/4/9 kcal per gram), else by the calorie field.
  const eaten = macroKcal ?? (typeof parseFigure(calories) === 'number' ? (parseFigure(calories) as number) : null)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    const kcal = numberOrNull(calories)
    const [c, f, p, w] = [carbs, fat, protein, water].map(numberOrNull)
    if (dayType === '') {
      setError('Önce o günün antrenman mı dinlenme günü mü olduğunu seçin.')
      return
    }
    if (kcal === null || kcal === 'invalid' || kcal > 20000) {
      setError('O gün aldığınız kaloriyi yazın (0 – 20.000).')
      return
    }
    if ([c, f, p, w].includes('invalid') || (w !== null && (w as number) > 20)) {
      setError('Makrolar gram, su litre olarak geçerli bir sayı olmalı (su en fazla 20 L).')
      return
    }
    save.mutate({
      consumedOn: date,
      calories: Math.round(kcal),
      carbsG: c === null ? null : Math.round(c as number),
      fatG: f === null ? null : Math.round(f as number),
      proteinG: p === null ? null : Math.round(p as number),
      waterLiters: w as number | null,
      dayType,
    })
  }

  return (
    <form onSubmit={submit} noValidate className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Günlük Beslenme ve Su</h2>
      <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
        O gün aldığınız toplam kaloriyi, makroları ve içtiğiniz suyu yazın. Geçmiş bir günü girmek için tarihi
        değiştirin; aynı güne tekrar kaydederseniz o gün güncellenir.
      </p>
      <div className="mt-4 grid grid-cols-2 gap-4 sm:grid-cols-3">
        <Field label="Tarih *">
          <input
            type="date"
            value={date}
            max={today}
            onChange={(event) => event.target.value && onDate(event.target.value)}
            className={inputClasses}
          />
        </Field>
        <Field label="Gün türü *" hint={dayType === '' && suggested ? 'O gün antrenman kaydınız var.' : 'Kaloriler antrenman ve dinlenme gününe göre değişir.'}>
          <select value={dayType} onChange={(event) => setDayType(event.target.value as IntakeDayType | '')} className={inputClasses}>
            <option value="">Seçin</option>
            {DAY_TYPES.map((type) => (
              <option key={type.value} value={type.value}>{type.label}</option>
            ))}
          </select>
        </Field>
        <Field label="Kalori (kcal) *">
          <input value={calories} onChange={(event) => setCalories(event.target.value)} type="text" inputMode="numeric" autoComplete="off" placeholder="örn. 2400" className={inputClasses} />
        </Field>
        <Field label="Su (litre)">
          <input value={water} onChange={(event) => setWater(event.target.value)} type="text" inputMode="decimal" autoComplete="off" placeholder="örn. 3,5" className={inputClasses} />
        </Field>
        <Field label="Karbonhidrat (g)">
          <input value={carbs} onChange={(event) => setCarbs(event.target.value)} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
        </Field>
        <Field label="Yağ (g)">
          <input value={fat} onChange={(event) => setFat(event.target.value)} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
        </Field>
        <Field label="Protein (g)">
          <input value={protein} onChange={(event) => setProtein(event.target.value)} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
        </Field>
      </div>
      {dayType !== '' ? (
        <TargetRemaining
          dayLabel={DAY_TYPES.find((type) => type.value === dayType)?.label ?? ''}
          target={targets?.calories ?? null}
          eaten={eaten}
          fromMacros={macroKcal !== null}
          remainingMacros={targets ? [
            ['Karbonhidrat', targets.carbsG, parseFigure(carbs)],
            ['Yağ', targets.fatG, parseFigure(fat)],
            ['Protein', targets.proteinG, parseFigure(protein)],
          ] : []}
        />
      ) : null}
      {existing ? <p className="mt-3 text-xs text-warning">Bu tarih için kayıt var; kaydederseniz güncellenir.</p> : null}
      {error ? (
        <p role="alert" className="mt-3 text-xs text-error">
          {error}
        </p>
      ) : null}
      {save.isError ? <p className="mt-3 text-xs text-error">{save.error.message}</p> : null}
      {save.isSuccess ? <p className="mt-3 text-xs text-success">Kaydedildi.</p> : null}
      <Button type="submit" className="mt-4" disabled={save.isPending}>
        {save.isPending ? 'Kaydediliyor…' : existing ? 'Güncelle' : 'Kaydet'}
      </Button>
    </form>
  )
}

/** Calories implied by the typed macros (4 kcal per gram of carbohydrate and protein, 9 of fat); null when none is typed. */
function macroCalories(carbs: string, fat: string, protein: string): number | null {
  const grams = [carbs, fat, protein].map((text) => {
    const value = parseFigure(text)
    return typeof value === 'number' ? value : 0
  })
  return grams.some((value) => value > 0) ? Math.round(grams[0] * 4 + grams[1] * 9 + grams[2] * 4) : null
}

function TargetRemaining({
  dayLabel,
  target,
  eaten,
  fromMacros,
  remainingMacros,
}: {
  dayLabel: string
  target: number | null
  eaten: number | null
  fromMacros: boolean
  remainingMacros: [string, number | null, number | null | 'invalid'][]
}) {
  if (target === null) {
    return (
      <p className="mt-3 rounded-xl border border-border bg-surface-elevated p-3 text-xs text-text-muted">
        {dayLabel} için koçunuzun belirlediği bir kalori hedefi yok.
      </p>
    )
  }
  const remaining = eaten === null ? null : target - eaten
  return (
    <div className="mt-3 rounded-xl border border-primary/30 bg-primary/5 p-3.5 text-xs" aria-live="polite">
      <p className="text-text-primary">
        {dayLabel} hedefi: <strong>{formatAmount(target)} kcal</strong>
        {eaten !== null ? (
          <>
            {' '}· {fromMacros ? 'makrolarınıza göre' : 'girdiğiniz'} {formatAmount(eaten)} kcal
          </>
        ) : null}
      </p>
      {remaining !== null ? (
        <p className={`mt-1 font-semibold ${remaining >= 0 ? 'text-success' : 'text-warning'}`}>
          {remaining >= 0
            ? `Hedefe kalan: ${formatAmount(remaining)} kcal`
            : `Hedefi ${formatAmount(-remaining)} kcal aştınız`}
        </p>
      ) : (
        <p className="mt-1 text-text-muted">Makrolarınızı ya da kalorinizi yazınca hedefe kalan burada görünür.</p>
      )}
      {remainingMacros.some(([, goal]) => goal !== null) ? (
        <p className="mt-1 text-text-secondary">
          Kalan makro:{' '}
          {remainingMacros
            .filter(([, goal]) => goal !== null)
            .map(([name, goal, typed]) => {
              const left = (goal as number) - (typeof typed === 'number' ? typed : 0)
              return `${name} ${left >= 0 ? `${formatAmount(left)} g` : `${formatAmount(-left)} g fazla`}`
            })
            .join(' · ')}
        </p>
      ) : null}
    </div>
  )
}
