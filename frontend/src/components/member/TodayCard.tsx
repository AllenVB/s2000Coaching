import { Link } from 'react-router-dom'

import { useMyIntake } from '@/api/intakeApi'
import { useMyNutritionPlan } from '@/api/nutritionApi'
import { useMyTrainingPlan, useMyWorkoutLogs } from '@/api/trainingApi'
import { WeekTracker } from '@/components/history/WeekTracker'
import { todayIso } from '@/lib/date'
import { formatAmount } from '@/lib/weeklyNutrition'
import { targetsOf } from '@/types/nutrition'

/**
 * The top of Programlarım: what today asks of the member. Whether today's workout is logged and which day of
 * the programme comes next, today's calorie target for the kind of day they chose and how much of it is left,
 * and the week's seven circles. Everything is read from what they already entered; nothing is stored here.
 */
export function TodayCard() {
  const today = todayIso()
  const { data: plan } = useMyTrainingPlan()
  const { data: logs } = useMyWorkoutLogs()
  const { data: nutrition } = useMyNutritionPlan()
  const { data: intakes } = useMyIntake()

  const trainedDates = new Set((logs ?? []).map((log) => log.performedOn))
  const todaysLog = logs?.find((log) => log.performedOn === today)

  // The day after the most recent one trained, in the programme's own order.
  const latest = [...(logs ?? [])].sort((a, b) => b.performedOn.localeCompare(a.performedOn))[0]
  const days = plan?.days ?? []
  const lastIndex = latest ? days.findIndex((day) => day.id === latest.dayId) : -1
  const nextDay = days.length > 0 ? days[(lastIndex + 1) % days.length] : null

  const todaysIntake = intakes?.find((row) => row.consumedOn === today)
  const kind = todaysIntake?.dayType ?? (todaysLog ? 'ANTRENMAN' : null)
  const target = nutrition && kind ? targetsOf(nutrition, kind === 'DINLENME' ? 'REST' : 'TRAINING').calories : null
  const eaten = todaysIntake ? macroOrTotal(todaysIntake) : null

  return (
    <section className="mt-6 space-y-4" aria-label="Bugün">
      <div className="grid gap-3 sm:grid-cols-2">
        <div className="rounded-2xl border border-border bg-surface p-5">
          <h2 className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">Bugünün Antrenmanı</h2>
          {!plan ? (
            <p className="mt-2 text-sm text-text-secondary">Koçunuz henüz bir antrenman programı atamadı.</p>
          ) : todaysLog ? (
            <p className="mt-2 text-sm font-semibold text-success">✓ {todaysLog.dayName} kaydedildi</p>
          ) : (
            <>
              <p className="mt-2 text-sm text-text-primary">
                Bugün için kayıt yok{nextDay ? <> · sıradaki gün: <strong>{nextDay.name}</strong></> : null}
              </p>
              <Link to="/uye/gecmis" className="mt-2 inline-block text-xs font-semibold uppercase tracking-wide text-primary hover:underline">
                Antrenmanı gir →
              </Link>
            </>
          )}
        </div>

        <div className="rounded-2xl border border-border bg-surface p-5">
          <h2 className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">Bugünün Kalorisi</h2>
          {!nutrition ? (
            <p className="mt-2 text-sm text-text-secondary">Koçunuz henüz bir beslenme programı atamadı.</p>
          ) : !kind ? (
            <>
              <p className="mt-2 text-sm text-text-primary">
                Antrenman günü <strong>{formatTarget(targetsOf(nutrition, 'TRAINING').calories)}</strong> · dinlenme günü{' '}
                <strong>{formatTarget(targetsOf(nutrition, 'REST').calories)}</strong>
              </p>
              <Link to="/uye/gecmis?sekme=beslenme" className="mt-2 inline-block text-xs font-semibold uppercase tracking-wide text-primary hover:underline">
                Bugünü gir →
              </Link>
            </>
          ) : (
            <>
              <p className="mt-2 text-sm text-text-primary">
                {kind === 'DINLENME' ? 'Dinlenme' : 'Antrenman'} günü hedefi: <strong>{formatTarget(target)}</strong>
              </p>
              {target !== null && eaten !== null ? (
                <p className={`mt-1 text-sm font-semibold ${target - eaten >= 0 ? 'text-success' : 'text-warning'}`}>
                  {target - eaten >= 0
                    ? `Hedefe kalan: ${formatAmount(target - eaten)} kcal`
                    : `Hedefi ${formatAmount(eaten - target)} kcal aştınız`}
                </p>
              ) : (
                <Link to="/uye/gecmis?sekme=beslenme" className="mt-2 inline-block text-xs font-semibold uppercase tracking-wide text-primary hover:underline">
                  Bugünü gir →
                </Link>
              )}
            </>
          )}
        </div>
      </div>

      <WeekTracker trainedDates={trainedDates} />
    </section>
  )
}

function formatTarget(calories: number | null): string {
  return calories === null ? 'belirlenmedi' : `${formatAmount(calories)} kcal`
}

/** Calories by the macros when they are there (4/4/9 per gram), else the typed total. */
function macroOrTotal(row: { calories: number; proteinG: number | null; carbsG: number | null; fatG: number | null }): number {
  const macros = (row.proteinG ?? 0) * 4 + (row.carbsG ?? 0) * 4 + (row.fatG ?? 0) * 9
  return macros > 0 ? macros : row.calories
}
