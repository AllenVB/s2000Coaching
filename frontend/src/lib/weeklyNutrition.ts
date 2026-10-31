import { todayIso } from '@/lib/date'
import type { Macros } from '@/types/weeklyNutrition'

const DAY_MS = 86_400_000

/** kcal in a gram of each: carbohydrate and protein 4, fat 9. The server uses the same numbers. */
const KCAL_PER_GRAM = { carbs: 4, protein: 4, fat: 9 }

// Dates are handled as UTC midnight so a daylight-saving change cannot move a day.
function parse(iso: string): number {
  const [year, month, day] = iso.split('-').map(Number)
  return Date.UTC(year, month - 1, day)
}

function format(ms: number): string {
  return new Date(ms).toISOString().slice(0, 10)
}

/** The Monday of the week `iso` falls in. */
export function mondayOf(iso: string): string {
  const ms = parse(iso)
  const sinceMonday = (new Date(ms).getUTCDay() + 6) % 7
  return format(ms - sinceMonday * DAY_MS)
}

/** This week's Monday and the ones before it, newest first. */
export function recentWeeks(count: number): string[] {
  const thisMonday = parse(mondayOf(todayIso()))
  return Array.from({ length: count }, (_, index) => format(thisMonday - index * 7 * DAY_MS))
}

/** How far back a week may be filled in; the server allows the same number of weeks. */
export const MAX_WEEKS_BACK = 156

/** The earliest Monday that can still be entered. */
export function earliestWeek(): string {
  const thisMonday = parse(mondayOf(todayIso()))
  return format(thisMonday - MAX_WEEKS_BACK * 7 * DAY_MS)
}

export function addDays(iso: string, days: number): string {
  return format(parse(iso) + days * DAY_MS)
}

/** "2026-10-26" as "26.10.2026". */
export function formatDay(iso: string): string {
  const [year, month, day] = iso.split('-')
  return `${day}.${month}.${year}`
}

/** "26.10.2026 – 01.11.2026" for the week starting on `weekStart`. */
export function formatWeek(weekStart: string): string {
  return `${formatDay(weekStart)} – ${formatDay(addDays(weekStart, 6))}`
}

function round1(value: number): number {
  return Math.round((value + Number.EPSILON) * 10) / 10
}

/** The week's calories split by percentage into grams, spread over `days` days (1 = the whole week). */
export function macrosOf(
  calories: number,
  carbsPercent: number,
  fatPercent: number,
  proteinPercent: number,
  days = 1,
): Macros {
  return {
    calories: round1(calories / days),
    carbsG: round1(((calories * carbsPercent) / 100 / KCAL_PER_GRAM.carbs) / days),
    fatG: round1(((calories * fatPercent) / 100 / KCAL_PER_GRAM.fat) / days),
    proteinG: round1(((calories * proteinPercent) / 100 / KCAL_PER_GRAM.protein) / days),
  }
}

/** "1750" → "1.750", for reading big numbers; one decimal only when there is one. */
export function formatAmount(value: number): string {
  return value.toLocaleString('tr-TR', { maximumFractionDigits: 1 })
}
