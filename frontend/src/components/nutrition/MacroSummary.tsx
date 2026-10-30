import { hasTargets, planTotals, targetsOf, type NutritionDayType, type NutritionPlan } from '@/types/nutrition'

/**
 * Target beside what the listed foods actually add up to, when the foods carry macros. A plan
 * written as amounts only (the usual table) has nothing to add up, so no "planned 0" is shown
 * — that would read as a mistake rather than as "not counted".
 */
export function MacroSummary({ plan, dayType }: { plan: NutritionPlan; dayType: NutritionDayType }) {
  const totals = planTotals(plan, dayType)
  const targets = targetsOf(plan, dayType)

  return (
    <div className="grid grid-cols-2 gap-3 sm:grid-cols-4">
      <MacroCell label="Kalori" unit="kcal" target={targets.calories} actual={totals.calories} />
      <MacroCell label="Protein" unit="g" target={targets.proteinG} actual={totals.proteinG} />
      <MacroCell label="Karbonhidrat" unit="g" target={targets.carbsG} actual={totals.carbsG} />
      <MacroCell label="Yağ" unit="g" target={targets.fatG} actual={totals.fatG} />
      {!hasTargets(targets) ? (
        <p className="col-span-2 text-[11px] text-text-muted sm:col-span-4">
          Bu gün türü için makro hedefi girilmemiş; gösterilen değerler öğünlerin toplamıdır.
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
  // No target and nothing counted is "not set", not zero.
  const shown = target ?? (actual > 0 ? actual : null)

  return (
    <div className="rounded-xl border border-border bg-surface-elevated p-3.5">
      <span className="block text-[11px] font-semibold uppercase tracking-wider text-text-muted">
        {label}
      </span>
      <span className="mt-1 block font-display text-xl font-bold text-text-primary">
        {shown ?? '—'}
        {shown !== null ? <span className="ml-1 text-xs font-medium text-text-muted">{unit}</span> : null}
      </span>
      {target !== null && actual > 0 ? (
        <span className="mt-0.5 block text-[11px] text-text-secondary">
          planlanan {actual} {unit}
        </span>
      ) : null}
    </div>
  )
}
