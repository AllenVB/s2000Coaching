import { useState } from 'react'

import { todayIso } from '@/lib/date'
import { formatWeek, recentWeeks } from '@/lib/weeks'

const DAY_NAMES = ['Pzt', 'Sal', 'Çar', 'Per', 'Cum', 'Cmt', 'Paz']

function addDays(iso: string, days: number): string {
  const [y, m, d] = iso.split('-').map(Number)
  return new Date(Date.UTC(y, m - 1, d + days)).toISOString().slice(0, 10)
}

/**
 * Seven circles, Monday to Sunday: an orange tick means a workout was logged that day. The arrows go back
 * through earlier weeks; the current week is the last one, since there is nothing to show beyond today.
 */
export function WeekTracker({ trainedDates }: { trainedDates: ReadonlySet<string> }) {
  const today = todayIso()
  const [weeksBack, setWeeksBack] = useState(0)
  const week = recentWeeks(weeksBack + 1, today)[weeksBack]
  const days = DAY_NAMES.map((name, index) => ({ name, date: addDays(week.start, index) }))
  const done = days.filter((day) => trainedDates.has(day.date)).length
  const arrow =
    'flex h-9 w-9 items-center justify-center rounded-lg border border-border text-text-secondary hover:text-text-primary disabled:opacity-30'

  return (
    <section className="rounded-2xl border border-border bg-surface p-5 sm:p-6" aria-label="Haftalık antrenman takibi">
      <div className="mb-4 flex items-center justify-between gap-3">
        <div>
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Bu Haftaki Antrenmanlar</h2>
          <p className="mt-0.5 text-[11px] text-text-muted">
            {formatWeek(week)} · {done} gün antrenman
          </p>
        </div>
        <div className="flex gap-2">
          <button type="button" className={arrow} aria-label="Önceki hafta" onClick={() => setWeeksBack((n) => n + 1)}>
            ‹
          </button>
          <button type="button" className={arrow} aria-label="Sonraki hafta" disabled={weeksBack === 0} onClick={() => setWeeksBack((n) => n - 1)}>
            ›
          </button>
        </div>
      </div>

      <ul className="grid grid-cols-7 gap-1.5">
        {days.map((day) => {
          const trained = trainedDates.has(day.date)
          const future = day.date > today
          return (
            <li key={day.date} className="flex flex-col items-center gap-1.5">
              <span className="text-[11px] font-semibold text-text-secondary">{day.name}</span>
              <span
                role="img"
                aria-label={`${day.name} ${trained ? 'antrenman yapıldı' : future ? 'henüz gelmedi' : 'antrenman yok'}`}
                className={`flex h-9 w-9 items-center justify-center rounded-full border-2 text-sm font-bold sm:h-10 sm:w-10 ${
                  trained
                    ? 'border-primary bg-primary text-canvas'
                    : future
                      ? 'border-dashed border-border text-transparent'
                      : 'border-border-strong text-transparent'
                } ${day.date === today ? 'ring-2 ring-primary/40 ring-offset-2 ring-offset-surface' : ''}`}
              >
                ✓
              </span>
            </li>
          )
        })}
      </ul>
    </section>
  )
}

