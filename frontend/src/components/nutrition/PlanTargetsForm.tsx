import { useState } from 'react'

import { useUpdateNutritionPlan } from '@/api/nutritionApi'
import { Button } from '@/components/ui/Button'
import { inputClasses, textareaClasses } from '@/components/ui/FormField'
import type { NutritionPlan } from '@/types/nutrition'

/**
 * The plan-level numbers — daily macros, water and the note shown above the plan. All
 * optional: a coach may prescribe the table without counting.
 */
export function PlanTargetsForm({ plan }: { plan: NutritionPlan }) {
  const update = useUpdateNutritionPlan()

  const [calories, setCalories] = useState(toText(plan.targetCalories))
  const [protein, setProtein] = useState(toText(plan.targetProteinG))
  const [carbs, setCarbs] = useState(toText(plan.targetCarbsG))
  const [fat, setFat] = useState(toText(plan.targetFatG))
  const [water, setWater] = useState(toText(plan.waterLiters))
  const [note, setNote] = useState(plan.note ?? '')

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    update.mutate({
      planId: plan.id,
      input: {
        name: plan.name,
        note: note.trim() || null,
        targetCalories: toNumber(calories),
        targetProteinG: toNumber(protein),
        targetCarbsG: toNumber(carbs),
        targetFatG: toNumber(fat),
        waterLiters: toNumber(water),
      },
    })
  }

  return (
    <form className="space-y-3 rounded-xl border border-border bg-surface-elevated p-4" onSubmit={submit}>
      <div className="grid grid-cols-2 gap-3 sm:grid-cols-5">
        <NumberField label="Kalori (kcal)" value={calories} onChange={setCalories} min={500} max={10000} />
        <NumberField label="Protein (g)" value={protein} onChange={setProtein} min={0} max={1000} />
        <NumberField label="Karbonhidrat (g)" value={carbs} onChange={setCarbs} min={0} max={2000} />
        <NumberField label="Yağ (g)" value={fat} onChange={setFat} min={0} max={500} />
        <NumberField label="Su (litre)" value={water} onChange={setWater} min={0} max={20} step={0.5} />
      </div>
      <label className="block">
        <span className="mb-1.5 block text-xs font-medium text-text-secondary">
          Plan notu (üyenin planın başında göreceği açıklamalar)
        </span>
        <textarea
          value={note}
          onChange={(event) => setNote(event.target.value)}
          maxLength={2000}
          placeholder="Örn: Öğlen öğünü esnektir. Karbonhidratlı öğünlerin yanında havuç tüket."
          className={textareaClasses}
        />
      </label>
      {update.isError ? <p className="text-xs text-error">{update.error.message}</p> : null}
      <div className="flex items-center gap-3">
        <Button type="submit" size="sm" disabled={update.isPending}>
          {update.isPending ? 'Kaydediliyor…' : 'Hedefleri Kaydet'}
        </Button>
        {update.isSuccess && !update.isPending ? (
          <span className="text-xs text-success">Kaydedildi</span>
        ) : null}
      </div>
    </form>
  )
}

function NumberField({
  label,
  value,
  onChange,
  min,
  max,
  step,
}: {
  label: string
  value: string
  onChange: (value: string) => void
  min: number
  max: number
  step?: number
}) {
  return (
    <label>
      <span className="mb-1.5 block text-xs font-medium text-text-secondary">{label}</span>
      <input
        value={value}
        onChange={(event) => onChange(event.target.value)}
        type="number"
        inputMode="decimal"
        min={min}
        max={max}
        step={step}
        placeholder="—"
        className={inputClasses}
      />
    </label>
  )
}

function toText(value: number | null): string {
  return value === null ? '' : String(value)
}

function toNumber(value: string): number | null {
  return value.trim() === '' ? null : Number(value)
}
