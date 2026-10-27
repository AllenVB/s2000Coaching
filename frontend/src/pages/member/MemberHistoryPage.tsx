import type { WorkoutLog } from '@/types/training'

import { useMyTrainingStats, useMyWorkoutLogs } from '@/api/trainingApi'
import { TrainingStatsView } from '@/components/training/TrainingStatsView'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { formatDate } from '@/lib/date'
import { formatMonth, groupByMonth, totalReps, totalVolumeKg } from '@/lib/workoutStats'

/** Every session the member has recorded, newest first, grouped by month. */
export function MemberHistoryPage() {
  const { data: logs, isPending, error } = useMyWorkoutLogs()
  const { data: stats } = useMyTrainingStats()

  if (isPending) {
    return (
      <>
        <PageHeading title="Antrenman Geçmişi" />
        <p className="text-sm text-text-secondary">Yükleniyor…</p>
      </>
    )
  }

  if (error) {
    return (
      <>
        <PageHeading title="Antrenman Geçmişi" />
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Geçmişiniz yüklenemedi: {error.message}
        </p>
      </>
    )
  }

  if (!logs?.length) {
    return (
      <>
        <PageHeading title="Antrenman Geçmişi" />
        <ComingSoonCard
          title="Henüz Kayıtlı Antrenman Yok"
          description="Bir antrenmanı tamamlayıp kaydettiğinizde burada listelenecek."
        />
      </>
    )
  }

  const months = groupByMonth(logs)

  return (
    <>
      <PageHeading
        title="Antrenman Geçmişi"
        description={`${logs.length} antrenman kaydınız var.`}
      />

      <SummaryRow logs={logs} />

      {stats ? (
        <div className="mt-6">
          <TrainingStatsView stats={stats} />
        </div>
      ) : null}

      <div className="mt-6 space-y-6">
        {months.map(({ month, logs: monthLogs }) => (
          <section key={month}>
            <h2 className="mb-3 text-[11px] font-semibold uppercase tracking-wider text-text-muted">
              {formatMonth(month)} · {monthLogs.length} antrenman
            </h2>
            <ul className="space-y-2.5">
              {monthLogs.map((log) => (
                <LogRow key={log.id} log={log} />
              ))}
            </ul>
          </section>
        ))}
      </div>
    </>
  )
}

function SummaryRow({ logs }: { logs: WorkoutLog[] }) {
  const volume = logs.reduce((sum, log) => sum + totalVolumeKg(log), 0)
  const thisMonth = logs.filter((log) => log.performedOn.startsWith(new Date().toISOString().slice(0, 7)))

  return (
    <div className="grid gap-3 sm:grid-cols-3">
      <SummaryCard label="Toplam Antrenman" value={String(logs.length)} />
      <SummaryCard label="Bu Ay" value={String(thisMonth.length)} />
      <SummaryCard
        label="Toplam Kaldırılan"
        value={volume > 0 ? `${Math.round(volume).toLocaleString('tr-TR')} kg` : '—'}
      />
    </div>
  )
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-5">
      <span className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">
        {label}
      </span>
      <p className="mt-1.5 font-display text-2xl font-bold text-text-primary">{value}</p>
    </div>
  )
}

function LogRow({ log }: { log: WorkoutLog }) {
  const volume = totalVolumeKg(log)

  return (
    <li className="rounded-xl border border-border bg-surface p-4">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <span className="font-semibold text-text-primary">{log.dayName}</span>
        <span className="text-xs text-text-muted">{formatDate(log.performedOn)}</span>
      </div>
      <p className="mt-1.5 text-xs text-text-secondary">
        {log.sets.length} set · {totalReps(log)} tekrar
        {volume > 0 ? ` · ${Math.round(volume).toLocaleString('tr-TR')} kg` : ''}
      </p>
      {log.note ? (
        <p className="mt-2 whitespace-pre-line border-t border-border pt-2 text-xs leading-relaxed text-text-secondary">
          {log.note}
        </p>
      ) : null}
    </li>
  )
}
