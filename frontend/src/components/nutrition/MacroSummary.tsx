import { planTotals, type NutritionPlan } from '@/types/nutrition'

/**
 * Target beside what the listed foods actually add up to. Showing both is the point: the gap
 * is what tells the coach the plan is not finished.
 */
export function MacroSummary({ plan }: { plan: NutritionPlan }) {
  const totals = planTotals(plan)
  const hasTargets =
    plan.targetCalories !== null ||
    plan.targetProteinG !== null ||
    plan.targetCarbsG !== null ||
    plan.targetFatG !== null

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
      <MacroCell label="Kalori" unit="kcal" target={plan.targetCalories} actual={totals.calories} />
      <MacroCell label="Protein" unit="g" target={plan.targetProteinG} actual={totals.proteinG} />
      <MacroCell label="Karbonhidrat" unit="g" target={plan.targetCarbsG} actual={totals.carbsG} />
      <MacroCell label="Yağ" unit="g" target={plan.targetFatG} actual={totals.fatG} />
      {!hasTargets ? (
        <p className="col-span-2 text-[11px] text-text-muted sm:col-span-4">
          Bu plan için makro hedefi girilmemiş; gösterilen değerler öğünlerin toplamıdır.
        </p>
      ) : null}
    </div>
  )
}

function MacroCell({
  label,
  unit,
  target,
  actual,
}: {
  label: string
  unit: string
  target: number | null
  actual: number
}) {
  return (
    <div className="rounded-xl border border-border bg-surface-elevated p-3.5">
      <span className="block text-[11px] font-semibold uppercase tracking-wider text-text-muted">
        {label}
      </span>
      <span className="mt-1 block font-display text-xl font-bold text-text-primary">
        {target ?? actual}
        <span className="ml-1 text-xs font-medium text-text-muted">{unit}</span>
      </span>
      {target !== null ? (
        <span className="mt-0.5 block text-[11px] text-text-secondary">
          planlanan {actual} {unit}
        </span>
      ) : null}
    </div>
  )
}
