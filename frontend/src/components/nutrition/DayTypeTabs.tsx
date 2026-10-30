import { DAY_TYPES, type NutritionDayType } from '@/types/nutrition'

/** The switch between a plan's training-day half and its rest-day half. */
export function DayTypeTabs({
  value,
  onChange,
  labels = 'long',
}: {
  value: NutritionDayType
  onChange: (next: NutritionDayType) => void
  labels?: 'long' | 'short'
}) {
  return (
    <div role="tablist" aria-label="Gün türü" className="inline-grid grid-cols-2 rounded-full border border-border bg-canvas p-1 text-xs font-semibold uppercase tracking-wide">
      {DAY_TYPES.map((entry) => (
        <button
          key={entry.value}
          type="button"
          role="tab"
          aria-selected={value === entry.value}
          onClick={() => onChange(entry.value)}
          className={`rounded-full px-4 py-2 transition-colors ${
            value === entry.value ? 'bg-primary text-canvas' : 'text-text-secondary hover:text-text-primary'
          }`}
        >
          {labels === 'long' ? entry.label : entry.short}
        </button>
      ))}
    </div>
  )
}
