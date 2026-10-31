import type { WorkoutLog } from '@/types/training'

/** How many different exercises a session had a set for (4–12 is typical, so this reads better than a kg total). */
export function exerciseCount(log: WorkoutLog): number {
  return new Set(log.sets.map((set) => set.planExerciseId)).size
}

export function totalReps(log: WorkoutLog): number {
  return log.sets.reduce((sum, set) => sum + set.reps, 0)
}

/** Sessions per calendar month, newest month first — the shape the history page groups by. */
export function groupByMonth(logs: WorkoutLog[]): { month: string; logs: WorkoutLog[] }[] {
  const byMonth = new Map<string, WorkoutLog[]>()

  for (const log of [...logs].sort((a, b) => b.performedOn.localeCompare(a.performedOn))) {
    // `performedOn` is an ISO date, so slicing is safe and avoids a timezone shift that
    // `new Date()` would introduce for sessions near midnight.
    const key = log.performedOn.slice(0, 7)
    const bucket = byMonth.get(key)
    if (bucket) {
      bucket.push(log)
    } else {
      byMonth.set(key, [log])
    }
  }

  return [...byMonth.entries()].map(([month, monthLogs]) => ({ month, logs: monthLogs }))
}

export function formatMonth(month: string): string {
  const [year, monthNumber] = month.split('-')
  return new Date(Number(year), Number(monthNumber) - 1, 1).toLocaleDateString('tr-TR', {
    month: 'long',
    year: 'numeric',
  })
}
