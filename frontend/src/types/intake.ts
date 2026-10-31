export type IntakeDayType = 'ANTRENMAN' | 'DINLENME'

export const DAY_TYPES: { value: IntakeDayType; label: string }[] = [
  { value: 'ANTRENMAN', label: 'Antrenman günü' },
  { value: 'DINLENME', label: 'Dinlenme günü' },
]

export interface Intake {
  id: string
  /** ISO date (yyyy-mm-dd) — the day the food was eaten. */
  consumedOn: string
  calories: number
  proteinG: number | null
  carbsG: number | null
  fatG: number | null
  sodiumMg: number | null
  note: string | null
  updatedAt: string
  /** Litres drunk that day; null when not written down. */
  waterLiters: number | null
  /** Training or rest day; null when not chosen. */
  dayType: IntakeDayType | null
}

export interface UpsertIntakeInput {
  consumedOn: string
  calories: number
  proteinG?: number | null
  carbsG?: number | null
  fatG?: number | null
  waterLiters?: number | null
  dayType?: IntakeDayType | null
}
