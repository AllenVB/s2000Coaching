import { todayIso } from '@/lib/date'

/** A Monday-to-Sunday block, as ISO dates. */
export interface WeekRange {
  start: string
  end: string
}

const DAY_MS = 24 * 60 * 60 * 1000

function shift(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d) + days * DAY_MS).toISOString().slice(0, 10)
}

/** The Monday of the week containing `iso`. */
export function mondayOf(iso: string): string {
  const [y, m, d] = iso.split('-').map(Number)
  const weekday = new Date(Date.UTC(y, m - 1, d)).getUTCDay() // 0 = Sunday
  return shift(iso, -((weekday + 6) % 7))
}

/**
 * The current week and the `count - 1` before it, newest first. Weeks always run Monday to Sunday and
 * there is never one that starts after today: the future cannot be reported on.
 */
export function recentWeeks(count: number, today: string = todayIso()): WeekRange[] {
  const thisMonday = mondayOf(today)
  return Array.from({ length: count }, (_, index) => {
    const start = shift(thisMonday, -7 * index)
    return { start, end: shift(start, 6) }
  })
}

/** The day a week's weight is recorded on: today for the current week, otherwise the week's last day. */
export function weighInDate(week: WeekRange, today: string = todayIso()): string {
  return week.end > today ? today : week.end
}

export function formatWeek(week: WeekRange): string {
  const fmt = (iso: string, withYear: boolean) =>
    new Date(`${iso}T12:00:00`).toLocaleDateString('tr-TR', {
      day: 'numeric',
      month: 'long',
      ...(withYear ? { year: 'numeric' } : {}),
    })
  return `${fmt(week.start, false)} – ${fmt(week.end, true)}`
}
