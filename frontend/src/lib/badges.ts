/**
 * Badges are worked out from what the member already entered; nothing is stored, so they can never drift
 * from the data (delete a record and the badge that depended on it goes with it). Every rule is a plain
 * count or a run of consecutive days, weeks or months.
 */
export interface BadgeInput {
  /** ISO dates of logged workouts (may repeat). */
  workoutDates: string[]
  /** ISO dates with a food-log entry. */
  intakeDates: string[]
  /** Steps per day. */
  steps: { date: string; steps: number }[]
  /** "YYYY-MM" periods with a monthly table. */
  periods: string[]
  goalReached: boolean
}

export interface Badge {
  id: string
  label: string
  description: string
  earned: boolean
}

const DAY_MS = 86_400_000

const dayNumber = (iso: string) => Math.floor(Date.parse(`${iso}T00:00:00Z`) / DAY_MS)

/** The length of the longest run of consecutive integers in the (unsorted, possibly repeated) values. */
export function longestRun(values: number[]): number {
  const sorted = [...new Set(values)].sort((a, b) => a - b)
  let best = 0
  let run = 0
  sorted.forEach((value, index) => {
    run = index > 0 && value === sorted[index - 1] + 1 ? run + 1 : 1
    best = Math.max(best, run)
  })
  return best
}

/** The Monday-based week number of an ISO date, as a day count divided by seven (Monday-aligned). */
const weekIndex = (iso: string) => Math.floor((dayNumber(iso) + 3) / 7)

const monthIndex = (period: string) => {
  const [year, month] = period.split('-').map(Number)
  return year * 12 + month
}

export function computeBadges(input: BadgeInput): Badge[] {
  const workouts = input.workoutDates.length
  const weekStreak = longestRun(input.workoutDates.map(weekIndex))
  const foodStreak = longestRun(input.intakeDates.map(dayNumber))
  const monthStreak = longestRun(input.periods.map(monthIndex))
  const bestSteps = Math.max(0, ...input.steps.map((row) => row.steps))

  return [
    { id: 'first-workout', label: 'İlk Adım', description: 'İlk antrenmanını kaydettin', earned: workouts >= 1 },
    { id: 'workouts-10', label: '10 Antrenman', description: '10 antrenman kaydettin', earned: workouts >= 10 },
    { id: 'workouts-50', label: '50 Antrenman', description: '50 antrenman kaydettin', earned: workouts >= 50 },
    { id: 'workouts-100', label: '100 Antrenman', description: '100 antrenman kaydettin', earned: workouts >= 100 },
    { id: 'week-streak-4', label: '4 Hafta Seri', description: '4 hafta üst üste her hafta en az bir antrenman', earned: weekStreak >= 4 },
    { id: 'week-streak-12', label: '12 Hafta Seri', description: '12 hafta üst üste her hafta en az bir antrenman', earned: weekStreak >= 12 },
    { id: 'food-7', label: '7 Gün Beslenme', description: '7 gün üst üste yemeğini kaydettin', earned: foodStreak >= 7 },
    { id: 'steps-10k', label: '10.000 Adım', description: 'Bir günde 10.000 adım attın', earned: bestSteps >= 10000 },
    { id: 'months-3', label: '3 Ay Düzen', description: '3 ay üst üste aylık ölçülerini girdin', earned: monthStreak >= 3 },
    { id: 'goal', label: 'Hedefe Ulaştın', description: 'Hedef kilona ulaştın', earned: input.goalReached },
  ]
}

/**
 * How far from the starting weight to the goal weight the member has come, 0 to 100. Works for losing and
 * gaining alike; null when there is no goal or the start already equals it.
 */
export function goalProgress(start: number | null, current: number | null, goal: number | null): number | null {
  if (start === null || current === null || goal === null || start === goal) {
    return null
  }
  const fraction = (start - current) / (start - goal)
  return Math.max(0, Math.min(100, Math.round(fraction * 100)))
}
