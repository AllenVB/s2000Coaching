import { useState } from 'react'
import { Link } from 'react-router-dom'

import type { WorkoutLog } from '@/types/training'

import { useDeleteMyWorkoutLog, useMyTrainingPlan, useMyTrainingStats, useMyWorkoutLogs } from '@/api/trainingApi'
import { TrainingStatsView } from '@/components/training/TrainingStatsView'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { Field, inputClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { currentMonthIso, formatDate, todayIso } from '@/lib/date'
import { formatMonth, groupByMonth, totalReps, totalVolumeKg } from '@/lib/workoutStats'

/** Every session the member has recorded, newest first, grouped by month. */
export function MemberHistoryPage() {
  const { data: logs, isPending, error } = useMyWorkoutLogs()
  const { data: stats } = useMyTrainingStats()
  const { data: plan } = useMyTrainingPlan()
  // Only days of the plan the member is on can be opened again; an older plan's days are not shown.
  const activeDayIds = new Set(plan?.days.map((day) => day.id) ?? [])
  const deleteLog = useDeleteMyWorkoutLog()
  const [deleting, setDeleting] = useState<WorkoutLog | null>(null)

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
        <div className="mt-6">
          <PastWorkoutForm days={plan?.days ?? []} />
        </div>
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

      <div className="mt-6">
        <PastWorkoutForm days={plan?.days ?? []} />
      </div>

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
                <LogRow key={log.id} log={log} editable={activeDayIds.has(log.dayId)} onDelete={() => setDeleting(log)} />
              ))}
            </ul>
          </section>
        ))}
      </div>

      {deleting ? (
        <ConfirmDialog
          title="Antrenmanı Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteLog.isPending}
          error={deleteLog.isError ? deleteLog.error.message : null}
          description={`${formatDate(deleting.performedOn)} tarihli "${deleting.dayName}" antrenman kaydını silmek istediğinize emin misiniz? Setleri ve notu da silinir.`}
          onCancel={() => setDeleting(null)}
          onConfirm={() => deleteLog.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        />
      ) : null}
    </>
  )
}

function SummaryRow({ logs }: { logs: WorkoutLog[] }) {
  const volume = logs.reduce((sum, log) => sum + totalVolumeKg(log), 0)
  const thisMonth = logs.filter((log) => log.performedOn.startsWith(currentMonthIso()))

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

/**
 * Adding a session from a day that has gone by: pick which day of the programme and which date, and
 * land on that day's page already recording it. A date that already has a session opens it to correct.
 */
function PastWorkoutForm({ days }: { days: { id: string; name: string }[] }) {
  const today = todayIso()
  const [dayId, setDayId] = useState('')
  const [date, setDate] = useState(today)

  if (days.length === 0) {
    return null
  }

  const chosenDay = dayId || days[0].id
  const valid = /^\d{4}-\d{2}-\d{2}$/.test(date) && date <= today

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Geçmiş Antrenman Ekle</h2>
      <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
        Kaydetmeyi unuttuğunuz bir antrenmanı, yaptığınız günü seçerek girin. Aynı güne daha önce kayıt
        girdiyseniz o açılır ve kaydedince güncellenir.
      </p>
      <div className="mt-4 flex flex-wrap items-end gap-3">
        <div className="min-w-[160px] flex-1">
          <Field label="Antrenman günü">
            <select value={chosenDay} onChange={(event) => setDayId(event.target.value)} className={inputClasses}>
              {days.map((day) => (
                <option key={day.id} value={day.id}>
                  {day.name}
                </option>
              ))}
            </select>
          </Field>
        </div>
        <div className="min-w-[160px] flex-1">
          <Field label="Hangi tarihte yaptınız?">
            <input value={date} onChange={(event) => setDate(event.target.value)} type="date" max={today} className={inputClasses} />
          </Field>
        </div>
        {valid ? (
          <Link to={`/uye/antrenman/${chosenDay}?tarih=${date}&kayit=1`}>
            <Button type="button">Devam</Button>
          </Link>
        ) : (
          <Button type="button" disabled>
            Devam
          </Button>
        )}
      </div>
    </section>
  )
}

function LogRow({ log, editable, onDelete }: { log: WorkoutLog; editable: boolean; onDelete: () => void }) {
  const volume = totalVolumeKg(log)

  return (
    <li className="rounded-xl border border-border bg-surface p-4">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <span className="font-semibold text-text-primary">{log.dayName}</span>
        <span className="flex items-center gap-3 text-xs text-text-muted">
          {formatDate(log.performedOn)}
          {editable ? (
            <Link
              to={`/uye/antrenman/${log.dayId}?tarih=${log.performedOn}&kayit=1`}
              className="font-semibold uppercase tracking-wide text-primary hover:underline"
              aria-label={`${formatDate(log.performedOn)} antrenmanını düzenle`}
            >
              Düzenle
            </Link>
          ) : null}
          <button
            type="button"
            onClick={onDelete}
            aria-label={`${formatDate(log.performedOn)} antrenmanını sil`}
            className="text-text-muted transition-colors hover:text-error"
          >
            <TrashIcon className="h-4 w-4" />
          </button>
        </span>
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
