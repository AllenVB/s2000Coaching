import type { WorkoutLog } from '@/types/training'

/**
 * Total weight moved in a session: every set's weight times its reps. Bodyweight sets carry
 * no weight, so they contribute nothing here — a session made only of them returns 0, which
 * the history page shows as "—" rather than a misleading zero.
 */
export function totalVolumeKg(log: WorkoutLog): number {
  return log.sets.reduce((sum, set) => sum + (set.weightKg ?? 0) * set.reps, 0)
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
