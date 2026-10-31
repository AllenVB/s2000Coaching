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
}

export interface UpsertIntakeInput {
  consumedOn: string
  calories: number
  proteinG?: number | null
  carbsG?: number | null
  fatG?: number | null
  waterLiters?: number | null
}
