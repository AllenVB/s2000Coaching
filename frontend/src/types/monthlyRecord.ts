export interface MonthlyRecord {
  id: string
  /** "YYYY-MM"; shown to people as "MM/YYYY". */
  period: string
  heightCm: number | null
  weightKg: number | null
  shoulderCm: number | null
  chestCm: number | null
  armCm: number | null
  neckCm: number | null
  waistCm: number | null
  hipCm: number | null
  thighCm: number | null
  updatedAt: string
}

export type FigureKey = Exclude<keyof MonthlyRecord, 'id' | 'period' | 'updatedAt'>

/** Body of a save: the whole table, with an empty figure sent as null. */
export type MonthlyRecordInput = { period?: string } & Record<FigureKey, number | null>

/** The rows of the table, in the order the member fills them in. */
export const FIGURES: { key: FigureKey; label: string; unit: string }[] = [
  { key: 'heightCm', label: 'Boy', unit: 'cm' },
  { key: 'weightKg', label: 'Kilo', unit: 'kg' },
  { key: 'shoulderCm', label: 'Omuz Çevresi', unit: 'cm' },
  { key: 'chestCm', label: 'Göğüs Çevresi', unit: 'cm' },
  { key: 'armCm', label: 'Kol Çevresi', unit: 'cm' },
  { key: 'neckCm', label: 'Boyun Çevresi', unit: 'cm' },
  { key: 'waistCm', label: 'Bel Çevresi', unit: 'cm' },
  { key: 'hipCm', label: 'Kalça Çevresi', unit: 'cm' },
  { key: 'thighCm', label: 'Tek Bacak Çevresi', unit: 'cm' },
]

/** "2026-10" as the "10/2026" people write. */
export function formatPeriod(period: string): string {
  return `${period.slice(5)}/${period.slice(0, 4)}`
}

/**
 * Reads one typed figure. A Turkish keyboard types a decimal comma, so both are accepted; empty
 * means "not measured" (null), and anything else that is not a number is an error rather than
 * quietly becoming zero.
 */
export function parseFigure(text: string): number | null | 'invalid' {
  const trimmed = text.trim().replace(',', '.')
  if (trimmed === '') {
    return null
  }
  const value = Number(trimmed)
  return Number.isFinite(value) && value > 0 ? value : 'invalid'
}
