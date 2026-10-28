import type { WeightEntry } from '@/types/training'

/** The strip is ten slots wide: "Antrenman 1" to "Antrenman 10" before it starts to slide. */
export const STRIP_SLOTS = 10

interface Slot {
  number: number
  weightKg: number | null
  isCurrent: boolean
}

/**
 * The slots to draw: numbered from the member's first session of this exercise, ten wide, with
 * the session being logged now as the last one that has a value. A date that already has a
 * session is that session (saving again corrects it); any other date is the next one.
 */
export function stripSlots(history: WeightEntry[], performedOn: string, liveWeightKg: number | null): Slot[] {
  const existing = history.find((entry) => entry.performedOn === performedOn)
  const lastNumber = history.length > 0 ? history[history.length - 1].sessionNumber : 0
  const currentNumber = existing ? existing.sessionNumber : lastNumber + 1
  const start = Math.max(1, currentNumber - (STRIP_SLOTS - 1))

  return Array.from({ length: STRIP_SLOTS }, (_unused, index) => {
    const number = start + index
    const isCurrent = number === currentNumber
    const saved = history.find((entry) => entry.sessionNumber === number)
    return {
      number,
      // What is typed in the form wins for today's slot; the others are what was saved.
      weightKg: isCurrent ? (liveWeightKg ?? saved?.topWeightKg ?? null) : (saved?.topWeightKg ?? null),
      isCurrent,
    }
  })
}

/** How much the top weight moved between the first and the latest slot that has one. */
export function stripChange(slots: Slot[]): number | null {
  const weights = slots.map((slot) => slot.weightKg).filter((weight): weight is number => weight !== null)
  if (weights.length < 2) {
    return null
  }
  return Math.round((weights[weights.length - 1] - weights[0]) * 100) / 100
}

/**
 * Ağırlık takibi: one cell per session of this exercise, so progress reads left to right. The
 * numbers come from the sessions the member already logs — there is nothing to type here, which
 * keeps a single place where a weight is entered.
 */
export function WeightStrip({
  history,
  performedOn,
  liveWeightKg,
}: {
  history: WeightEntry[]
  performedOn: string
  liveWeightKg: number | null
}) {
  const slots = stripSlots(history, performedOn, liveWeightKg)
  const change = stripChange(slots)

  return (
    <div className="mb-3">
      <div className="mb-1.5 flex flex-wrap items-baseline justify-between gap-2">
        <span className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">Ağırlık takibi</span>
        {change !== null ? (
          <span className={`text-[11px] font-semibold ${change > 0 ? 'text-success' : 'text-text-muted'}`}>
            {change > 0 ? '+' : ''}
            {change} kg
          </span>
        ) : null}
      </div>
      <ol className="grid grid-cols-5 gap-1.5">
        {slots.map((slot) => (
          <li
            key={slot.number}
            aria-current={slot.isCurrent ? 'step' : undefined}
            className={`rounded-lg border px-1 py-1.5 text-center ${
              slot.isCurrent
                ? 'border-dashed border-primary bg-primary/5'
                : slot.weightKg !== null
                  ? 'border-border bg-surface-elevated'
                  : 'border-border/60 bg-transparent'
            }`}
          >
            <span className="block text-[9px] font-semibold uppercase leading-tight tracking-wide text-text-muted">
              Antrenman {slot.number}
            </span>
            <span
              className={`mt-0.5 block text-sm font-bold ${
                slot.weightKg !== null ? 'text-text-primary' : 'text-text-muted/60'
              }`}
            >
              {slot.weightKg !== null ? slot.weightKg : '—'}
            </span>
          </li>
        ))}
      </ol>
    </div>
  )
}
