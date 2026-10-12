import { Link } from 'react-router-dom'

import type { NutritionPlan } from '@/types/nutrition'

import { hasAnyMacro, plannedTotals } from '@/lib/nutritionStats'

/**
 * The day's macro targets at a glance. A plan can carry targets, meal macros, or both:
 * when both exist the bar shows how far the planned meals go towards the target, which is
 * what tells the member whether the plan as written actually adds up.
 */
export function TodayMacrosCard({ plan }: { plan: NutritionPlan }) {
  const planned = plannedTotals(plan)
  const hasTargets =
    plan.targetCalories !== null ||
    plan.targetProteinG !== null ||
    plan.targetCarbsG !== null ||
    plan.targetFatG !== null

  if (!hasTargets && !hasAnyMacro(planned)) {
    return null
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-4 flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
          Bugünün Makroları
        </h2>
        <Link
          to="/uye/beslenme"
          className="text-[11px] font-semibold uppercase tracking-wider text-primary transition-opacity hover:opacity-80"
        >
          Beslenme Planı
        </Link>
      </div>

      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <MacroStat label="Kalori" unit="kcal" target={plan.targetCalories} planned={planned.calories} />
        <MacroStat label="Protein" unit="g" target={plan.targetProteinG} planned={planned.proteinG} />
        <MacroStat label="Karbonhidrat" unit="g" target={plan.targetCarbsG} planned={planned.carbsG} />
        <MacroStat label="Yağ" unit="g" target={plan.targetFatG} planned={planned.fatG} />
      </div>
    </section>
  )
}

function MacroStat({
  label,
  unit,
  target,
  planned,
}: {
  label: string
  unit: string
  target: number | null
  planned: number
}) {
  // Without a target there is nothing to fill towards, so the planned amount stands alone.
  const ratio = target && target > 0 ? Math.min(planned / target, 1) : null

  return (
    <div>
      <span className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">
        {label}
      </span>
      <p className="mt-1 font-display text-xl font-bold text-text-primary">
        {target ?? planned}
        <span className="ml-1 text-xs font-medium text-text-muted">{unit}</span>
      </p>
      {ratio === null ? (
        <p className="mt-1 text-[11px] text-text-muted">planda yazan</p>
      ) : (
        <>
          <div className="mt-1.5 h-1 overflow-hidden rounded-full bg-border">
            <div className="h-full rounded-full bg-primary" style={{ width: `${ratio * 100}%` }} />
          </div>
          <p className="mt-1 text-[11px] text-text-muted">
            {planned > 0 ? `öğünlerde ${planned} ${unit}` : 'öğünlere girilmedi'}
          </p>
        </>
      )}
    </div>
  )
}
