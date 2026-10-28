import { Link } from 'react-router-dom'

import type { Measurement } from '@/types/measurement'

import { ChartIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'
import { todayIso } from '@/lib/date'

/**
 * Must match `AdminDashboardService.STALE_AFTER_DAYS`. The coach's dashboard lists members
 * who have not checked in for this long, so a different number here would tell the member
 * they are up to date while the coach is being told to chase them.
 */
export const STALE_AFTER_DAYS = 10

function daysSince(isoDate: string): number {
  // Both sides are snapped to midnight UTC so the answer is a whole number of calendar
  // days, not a fraction that flips depending on the hour the page is opened.
  const then = Date.parse(`${isoDate}T00:00:00Z`)
  const now = Date.parse(`${todayIso()}T00:00:00Z`)
  return Math.floor((now - then) / 86_400_000)
}

/** Nudges the member to weigh in, and says nothing while their check-ins are current. */
export function MeasurementReminder({ measurements }: { measurements: Measurement[] }) {
  const latest = [...measurements].sort((a, b) => b.measuredOn.localeCompare(a.measuredOn))[0]

  if (latest && daysSince(latest.measuredOn) < STALE_AFTER_DAYS) {
    return null
  }

  return (
    <section className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-primary/30 bg-primary/5 p-5">
      <div className="flex items-start gap-3">
        <span className="mt-0.5 flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary/10 text-primary">
          <ChartIcon className="h-4.5 w-4.5" />
        </span>
        <div>
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
            Ölçüm Zamanı
          </h2>
          <p className="mt-1 text-xs leading-relaxed text-text-secondary">
            {latest
              ? `Son ölçümünüz ${formatDate(latest.measuredOn)} tarihinde, ${daysSince(latest.measuredOn)} gün önce. Koçunuzun gelişiminizi takip edebilmesi için yeni bir ölçüm girin.`
              : 'Henüz ölçüm girmediniz. İlk ölçümünüz, gelişiminizin karşılaştırılacağı başlangıç noktası olacak.'}
          </p>
        </div>
      </div>
      <Link
        to="/uye/olcumler"
        className="inline-flex h-10 items-center justify-center rounded-xl bg-primary px-4 text-xs font-semibold uppercase tracking-wide text-canvas transition-colors hover:bg-primary-hover"
      >
        Ölçüm Gir
      </Link>
    </section>
  )
}
