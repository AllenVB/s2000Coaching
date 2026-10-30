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
}
