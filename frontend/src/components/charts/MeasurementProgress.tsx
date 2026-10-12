import { useMemo, useState } from 'react'

import { ProgressChart } from '@/components/charts/ProgressChart'
import { MEASURE_FIELDS, type MeasureKey, type Measurement } from '@/types/measurement'

/**
 * Chart plus its measure switcher. One series at a time on one axis — switching the measure
 * swaps the series instead of adding a second scale.
 */
export function MeasurementProgress({ measurements }: { measurements: Measurement[] }) {
  const [measure, setMeasure] = useState<MeasureKey>('weightKg')

  // Which measures the member actually recorded; an empty one would just draw a blank chart.
  const available = useMemo(
    () => MEASURE_FIELDS.filter((field) => measurements.some((row) => row[field.key] !== null)),
    [measurements],
  )

  const active = available.find((field) => field.key === measure) ?? available[0]

  const points = useMemo(() => {
    if (!active) {
      return []
    }
    return measurements
      .filter((row) => row[active.key] !== null)
      .map((row) => ({ date: row.measuredOn, value: Number(row[active.key]) }))
      .sort((a, b) => a.date.localeCompare(b.date))
  }, [measurements, active])

  if (!active || points.length === 0) {
    return null
  }

  const first = points[0].value
  const last = points[points.length - 1].value
  const delta = last - first

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-4 flex flex-wrap items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
            {active.label} Değişimi
          </h2>
          {points.length > 1 ? (
            <p className="mt-1 text-xs text-text-secondary">
              İlk ölçümden bu yana{' '}
              <span className={delta === 0 ? 'text-text-primary' : 'font-semibold text-text-primary'}>
                {delta > 0 ? '+' : ''}
                {delta.toFixed(1)} {active.unit}
              </span>
            </p>
          ) : null}
        </div>

        {available.length > 1 ? (
          <div className="flex flex-wrap gap-1.5">
            {available.map((field) => (
              <button
                key={field.key}
                type="button"
                onClick={() => setMeasure(field.key)}
                aria-pressed={field.key === active.key}
                className={`h-8 rounded-lg border px-2.5 text-xs font-semibold transition-colors ${
                  field.key === active.key
                    ? 'border-primary bg-primary/10 text-primary'
                    : 'border-border bg-surface text-text-secondary hover:text-text-primary'
                }`}
              >
                {field.label}
              </button>
            ))}
          </div>
        ) : null}
      </div>

      <ProgressChart points={points} unit={active.unit} label={active.label} />
    </section>
  )
}
