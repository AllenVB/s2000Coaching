/**
 * The zone the business runs on, the same one the server reads as `app.time-zone`. A date the
 * member picks or the page defaults to has to name the same calendar day the server will
 * check it against, so it comes from this zone and not from the device or from UTC: with UTC,
 * anything done between midnight and 03:00 in Turkey lands on the previous day.
 */
export const APP_TIME_ZONE: string = import.meta.env.VITE_APP_TIME_ZONE || 'Europe/Istanbul'

/** `YYYY-MM-DD` for `instant` as the wall clock reads it in `timeZone`. */
export function isoDateIn(timeZone: string, instant: Date = new Date()): string {
  const parts = new Intl.DateTimeFormat('en-US', {
    timeZone,
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  }).formatToParts(instant)
  const part = (type: string) => parts.find((entry) => entry.type === type)?.value ?? ''
  return `${part('year')}-${part('month')}-${part('day')}`
}

/** Today's date in the business zone, as the `YYYY-MM-DD` string date inputs and the API use. */
export function todayIso(): string {
  return isoDateIn(APP_TIME_ZONE)
}

/** This month as `YYYY-MM`, for matching against ISO dates. */
export function currentMonthIso(): string {
  return todayIso().slice(0, 7)
}

export function formatDate(value: string): string {
  return new Date(value).toLocaleDateString('tr-TR', { day: '2-digit', month: 'long', year: 'numeric' })
}

export function formatDateTime(value: string): string {
  return new Date(value).toLocaleString('tr-TR', {
    day: '2-digit',
    month: '2-digit',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}
