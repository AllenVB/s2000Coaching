export interface Measurement {
  id: string
  /** ISO date (yyyy-mm-dd) — the day the member stepped on the scale, not when they typed it in. */
  measuredOn: string
  weightKg: number
  waistCm: number | null
  chestCm: number | null
  hipCm: number | null
  armCm: number | null
  thighCm: number | null
  note: string | null
  updatedAt: string
}

export interface UpsertMeasurementInput {
  measuredOn: string
  weightKg: number
  waistCm?: number | null
  chestCm?: number | null
  hipCm?: number | null
  armCm?: number | null
  thighCm?: number | null
  note?: string | null
}

/** The measures the progress chart can plot, one at a time. */
export const MEASURE_FIELDS = [
  { key: 'weightKg', label: 'Kilo', unit: 'kg' },
  { key: 'waistCm', label: 'Bel', unit: 'cm' },
  { key: 'chestCm', label: 'Göğüs', unit: 'cm' },
  { key: 'hipCm', label: 'Kalça', unit: 'cm' },
  { key: 'armCm', label: 'Kol', unit: 'cm' },
  { key: 'thighCm', label: 'Bacak', unit: 'cm' },
] as const

export type MeasureKey = (typeof MEASURE_FIELDS)[number]['key']
