import { useRef, useState } from 'react'
import { useSearchParams } from 'react-router-dom'

import type { WorkoutLog } from '@/types/training'

import { useDeleteMyWorkoutLog, useMyTrainingPlan, useMyTrainingStats, useMyWorkoutLogs } from '@/api/trainingApi'
import { useMemberWeeklyNutrition } from '@/api/weeklyNutritionApi'
import { ActivityLog } from '@/components/history/ActivityLog'
import { DailyIntakeLog } from '@/components/history/DailyIntakeLog'
import { WeekTracker } from '@/components/history/WeekTracker'
import { WorkoutTableLogger } from '@/components/history/WorkoutTableLogger'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { WeeklyNutritionAlbum } from '@/components/nutrition/WeeklyNutritionAlbum'
import { TrainingStatsView } from '@/components/training/TrainingStatsView'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { TrashIcon } from '@/components/ui/icons'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { SectionTabs, tabFromParam } from '@/components/ui/SectionTabs'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { currentMonthIso, formatDate, todayIso } from '@/lib/date'
import { formatMonth, groupByMonth, exerciseCount, totalReps } from '@/lib/workoutStats'

const TABS = [
  { value: 'antrenman', label: 'Antrenman' },
  { value: 'beslenme', label: 'Beslenme' },
  { value: 'aktivite', label: 'Kardiyo & Adım' },
] as const

/**
 * "Geçmiş": where the member writes down what they actually did — the repetitions of each set of the
 * assigned programme, and each day's food, macros and water — and looks back at it. Past days are
 * entered by choosing their date.
 */
export function MemberHistoryPage() {
  const { member } = useMemberProfileSync()
  const [params, setParams] = useSearchParams()
  const tab = tabFromParam(params.get('sekme'), [...TABS])

  return (
    <>
      <PageHeading
        title="Geçmiş"
        description="Yaptığınız antrenmanın tekrar sayılarını, günlük beslenmenizi, adım ve kardiyonuzu buraya girersiniz; geçmiş günleri tarihini seçerek girebilir, düzeltebilirsiniz."
      />
      <SectionTabs label="Kayıt türü" tabs={[...TABS]} value={tab} onChange={(next) => setParams({ sekme: next }, { replace: true })} />
      {tab === 'antrenman' ? (
        <WorkoutHistory />
      ) : member?.healthDataConsentAt ? (
        tab === 'aktivite' ? <ActivityLog /> : <NutritionHistory />
      ) : member ? (
        <HealthDataConsent />
      ) : null}
    </>
  )
}

function NutritionHistory() {
  const weekly = useMemberWeeklyNutrition()
  return (
    <div className="space-y-10">
      <DailyIntakeLog />
      <section>
        <h2 className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">Haftalık Özet ve Fotoğraf</h2>
        <p className="mb-4 text-xs leading-relaxed text-text-secondary">
          Haftanın toplam kalorisi, yüzdeleri ve tek fotoğrafı; geçmiş haftalar da girilebilir.
        </p>
        <WeeklyNutritionAlbum source={weekly} />
      </section>
    </div>
  )
}

function WorkoutHistory() {
  const { data: plan, isLoading: planLoading } = useMyTrainingPlan()
  const { data: logs, isPending, error } = useMyWorkoutLogs()
  const { data: stats } = useMyTrainingStats()
  const deleteLog = useDeleteMyWorkoutLog()
  const [deleting, setDeleting] = useState<WorkoutLog | null>(null)
  const [target, setTarget] = useState<{ dayId: string; date: string } | null>(null)
  const loggerRef = useRef<HTMLDivElement>(null)

  if (isPending || planLoading) {
    return <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
  }
  if (error) {
    return (
      <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
        Geçmişiniz yüklenemedi: {error.message}
      </p>
    )
  }

  // Only days of the programme the member is on can be opened again; an older programme's days are not shown.
  const activeDayIds = new Set(plan?.days.map((day) => day.id) ?? [])
  const current = target ?? (plan?.days[0] ? { dayId: plan.days[0].id, date: todayIso() } : null)
  const edit = (log: WorkoutLog) => {
    setTarget({ dayId: log.dayId, date: log.performedOn })
    loggerRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
  const months = groupByMonth(logs ?? [])

  return (
    <div className="space-y-6">
      <div ref={loggerRef}>
        {plan && current ? (
          <WorkoutTableLogger plan={plan} logs={logs ?? []} dayId={current.dayId} date={current.date} onChange={setTarget} />
        ) : (
          <ComingSoonCard
            title="Antrenman Programınız Hazırlanıyor"
            description="Koçunuz programınızı atadığında tekrar sayılarınızı buradaki tabloya girebileceksiniz."
          />
        )}
      </div>

      <WeekTracker trainedDates={new Set((logs ?? []).map((log) => log.performedOn))} />

      {logs && logs.length > 0 ? (
        <>
          <SummaryRow logs={logs} />
          {stats ? <TrainingStatsView stats={stats} /> : null}
          <div className="space-y-6">
            {months.map(({ month, logs: monthLogs }) => (
              <section key={month}>
                <h2 className="mb-3 text-[11px] font-semibold uppercase tracking-wider text-text-muted">
                  {formatMonth(month)} · {monthLogs.length} antrenman
                </h2>
                <ul className="space-y-2.5">
                  {monthLogs.map((log) => (
                    <LogRow
                      key={log.id}
                      log={log}
                      onEdit={activeDayIds.has(log.dayId) ? () => edit(log) : undefined}
                      onDelete={() => setDeleting(log)}
                    />
                  ))}
                </ul>
              </section>
            ))}
          </div>
        </>
      ) : (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          Henüz kayıtlı antrenmanınız yok. Yukarıdaki tabloyu doldurup kaydedin.
        </p>
      )}

      {deleting ? (
        <ConfirmDialog
          title="Antrenmanı Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deleteLog.isPending}
          error={deleteLog.isError ? deleteLog.error.message : null}
          description={`${formatDate(deleting.performedOn)} tarihli "${deleting.dayName}" antrenman kaydını silmek istediğinize emin misiniz? Setleri de silinir.`}
          onCancel={() => setDeleting(null)}
          onConfirm={() => deleteLog.mutate(deleting.id, { onSuccess: () => setDeleting(null) })}
        />
      ) : null}
    </div>
  )
}

function SummaryRow({ logs }: { logs: WorkoutLog[] }) {
  const sets = logs.reduce((sum, log) => sum + log.sets.length, 0)
  const thisMonth = logs.filter((log) => log.performedOn.startsWith(currentMonthIso()))

  return (
    <div className="grid gap-3 sm:grid-cols-3">
      <SummaryCard label="Toplam Antrenman" value={String(logs.length)} />
      <SummaryCard label="Bu Ay" value={String(thisMonth.length)} />
      <SummaryCard label="Toplam Set" value={sets.toLocaleString('tr-TR')} />
    </div>
  )
}

function SummaryCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-2xl border border-border bg-surface p-5">
      <span className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">{label}</span>
      <p className="mt-1.5 font-display text-2xl font-bold text-text-primary">{value}</p>
    </div>
  )
}

function LogRow({ log, onEdit, onDelete }: { log: WorkoutLog; onEdit?: () => void; onDelete: () => void }) {
  return (
    <li className="rounded-xl border border-border bg-surface p-4">
      <div className="flex flex-wrap items-baseline justify-between gap-x-4 gap-y-1">
        <span className="font-semibold text-text-primary">{log.dayName}</span>
        <span className="flex items-center gap-3 text-xs text-text-muted">
          {formatDate(log.performedOn)}
          {onEdit ? (
            <button
              type="button"
              onClick={onEdit}
              className="font-semibold uppercase tracking-wide text-primary hover:underline"
              aria-label={`${formatDate(log.performedOn)} antrenmanını düzenle`}
            >
              Düzenle
            </button>
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
        {exerciseCount(log)} hareket · {log.sets.length} set · {totalReps(log)} tekrar
      </p>
      {log.note ? (
        <p className="mt-2 whitespace-pre-line border-t border-border pt-2 text-xs leading-relaxed text-text-secondary">{log.note}</p>
      ) : null}
    </li>
  )
}
