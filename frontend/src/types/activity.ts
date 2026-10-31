export interface Activity {
  id: string
  /** ISO date (yyyy-mm-dd). */
  performedOn: string
  steps: number | null
  cardioMinutes: number | null
  updatedAt: string
}

export interface UpsertActivityInput {
  performedOn: string
  steps: number | null
  cardioMinutes: number | null
}
