export interface CheckIn {
  id: string
  /** The Monday of the week reported (yyyy-mm-dd). */
  weekStart: string
  sleep: number
  stress: number
  energy: number
  hunger: number
  dietAdherencePercent: number | null
  note: string | null
  coachReply: string | null
  coachRepliedAt: string | null
  updatedAt: string
}

export interface UpsertCheckInInput {
  weekOf: string
  sleep: number
  stress: number
  energy: number
  hunger: number
  dietAdherencePercent?: number | null
  note?: string | null
}

/** The four 1–5 questions, with what each end of the scale means. */
export const CHECK_IN_SCALES = [
  { key: 'sleep', label: 'Uyku', low: 'kötü', high: 'çok iyi' },
  { key: 'stress', label: 'Stres', low: 'sakin', high: 'çok stresli' },
  { key: 'energy', label: 'Enerji', low: 'bitkin', high: 'enerjik' },
  { key: 'hunger', label: 'Açlık', low: 'hiç acıkmadım', high: 'sürekli acıktım' },
] as const
