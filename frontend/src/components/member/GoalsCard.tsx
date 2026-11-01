import { useState } from 'react'

import { useMyActivity } from '@/api/activityApi'
import { useMyIntake } from '@/api/intakeApi'
import { useSetGoals } from '@/api/memberAuthApi'
import { useMyMeasurements } from '@/api/measurementsApi'
import { useMemberRecordSource } from '@/api/monthlyRecordApi'
import { useMyWorkoutLogs } from '@/api/trainingApi'
import { Button } from '@/components/ui/Button'
import { inputClasses } from '@/components/ui/FormField'
import { computeBadges, goalProgress } from '@/lib/badges'
import { derive } from '@/lib/bodyMetrics'
import type { Member } from '@/types/member'
import { parseFigure } from '@/types/monthlyRecord'

/** "Hedeflerim": the member's own goal weight / body fat with a progress bar, and the badges earned so far. */
export function GoalsCard({ member }: { member: Member }) {
  const { data: logs } = useMyWorkoutLogs()
  const { data: intakes } = useMyIntake()
  const { data: activity } = useMyActivity()
  const { data: measurements } = useMyMeasurements()
  const monthly = useMemberRecordSource()
  const setGoals = useSetGoals()
  const [editing, setEditing] = useState(false)
  const [weightText, setWeightText] = useState(member.goalWeightKg?.toString() ?? '')
  const [fatText, setFatText] = useState(member.goalBodyFatPercent?.toString() ?? '')
  const [error, setError] = useState<string | null>(null)

  // Oldest and newest weigh-in; the registration weight stands in until there are measurements.
  const weighed = [...(measurements ?? [])].sort((a, b) => a.measuredOn.localeCompare(b.measuredOn))
  const startWeight = weighed.length > 0 ? Number(weighed[0].weightKg) : Number(member.weightKg)
  const currentWeight = weighed.length > 0 ? Number(weighed[weighed.length - 1].weightKg) : Number(member.weightKg)
  const goalWeight = member.goalWeightKg === null ? null : Number(member.goalWeightKg)
  const weightProgress = goalProgress(startWeight, currentWeight, goalWeight)

  const records = [...(monthly.records ?? [])].sort((a, b) => a.period.localeCompare(b.period))
  const latestFat = records.length > 0 ? derive(records[records.length - 1], member.gender, member.heightCm).bodyFatPercent : null

  const reached =
    goalWeight !== null && (startWeight >= goalWeight ? currentWeight <= goalWeight : currentWeight >= goalWeight)
  const badges = computeBadges({
    workoutDates: (logs ?? []).map((log) => log.performedOn),
    intakeDates: (intakes ?? []).map((row) => row.consumedOn),
    steps: (activity ?? []).flatMap((row) => (row.steps === null ? [] : [{ date: row.performedOn, steps: row.steps }])),
    periods: records.map((record) => record.period),
    goalReached: reached,
  })

  const save = (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    const weight = parseFigure(weightText)
    const fat = parseFigure(fatText)
    if (weight === 'invalid' || (typeof weight === 'number' && (weight < 30 || weight > 300))) {
      setError('Hedef kilo 30 ile 300 kg arasında olmalı.')
      return
    }
    if (fat === 'invalid' || (typeof fat === 'number' && (fat < 3 || fat > 60))) {
      setError('Hedef yağ oranı %3 ile %60 arasında olmalı.')
      return
    }
    setGoals.mutate({ goalWeightKg: weight, goalBodyFatPercent: fat }, { onSuccess: () => setEditing(false) })
  }

  return (
    <section className="mt-6 rounded-2xl border border-border bg-surface p-5 sm:p-6" aria-label="Hedeflerim">
      <div className="mb-4 flex items-center justify-between gap-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Hedeflerim</h2>
        {!editing ? (
          <button type="button" onClick={() => setEditing(true)} className="text-xs font-semibold uppercase tracking-wide text-primary hover:underline">
            {goalWeight === null && member.goalBodyFatPercent === null ? 'Hedef belirle' : 'Düzenle'}
          </button>
        ) : null}
      </div>

      {editing ? (
        <form onSubmit={save} className="grid gap-3 sm:grid-cols-3 sm:items-end">
          <label className="text-xs font-medium text-text-secondary">
            Hedef kilo (kg)
            <input value={weightText} onChange={(e) => setWeightText(e.target.value)} inputMode="decimal" className={`${inputClasses} mt-1`} />
          </label>
          <label className="text-xs font-medium text-text-secondary">
            Hedef yağ oranı (%)
            <input value={fatText} onChange={(e) => setFatText(e.target.value)} inputMode="decimal" className={`${inputClasses} mt-1`} />
          </label>
          <div className="flex gap-2">
            <Button type="submit" size="sm" disabled={setGoals.isPending}>Kaydet</Button>
            <Button type="button" size="sm" variant="ghost" onClick={() => setEditing(false)}>Vazgeç</Button>
          </div>
          {error || setGoals.isError ? <p role="alert" className="text-xs text-error sm:col-span-3">{error ?? setGoals.error?.message}</p> : null}
        </form>
      ) : goalWeight === null && member.goalBodyFatPercent === null ? (
        <p className="text-sm text-text-secondary">Henüz bir hedef belirlemedin. Hedef kilonu ya da yağ oranını yazarsan ilerlemen burada görünür.</p>
      ) : (
        <div className="space-y-4">
          {goalWeight !== null ? (
            <div>
              <p className="text-sm text-text-primary">
                Kilo: <strong>{currentWeight.toLocaleString('tr-TR')} kg</strong> → hedef <strong>{goalWeight.toLocaleString('tr-TR')} kg</strong>
                <span className="ml-2 text-xs text-text-muted">başlangıç {startWeight.toLocaleString('tr-TR')} kg</span>
              </p>
              {weightProgress !== null ? (
                <div className="mt-2" role="progressbar" aria-valuenow={weightProgress} aria-valuemin={0} aria-valuemax={100} aria-label="Hedef kilo ilerlemesi">
                  <div className="h-2.5 overflow-hidden rounded-full bg-surface-elevated">
                    <div className="h-full rounded-full bg-primary" style={{ width: `${weightProgress}%` }} />
                  </div>
                  <p className="mt-1 text-[11px] text-text-muted">%{weightProgress} tamamlandı</p>
                </div>
              ) : null}
            </div>
          ) : null}
          {member.goalBodyFatPercent !== null ? (
            <p className="text-sm text-text-primary">
              Yağ oranı: <strong>{latestFat !== null ? `%${latestFat.toFixed(1)} (tahmin)` : 'henüz hesaplanamadı'}</strong> → hedef{' '}
              <strong>%{Number(member.goalBodyFatPercent).toLocaleString('tr-TR')}</strong>
            </p>
          ) : null}
        </div>
      )}

      <div className="mt-5 border-t border-border pt-4">
        <h3 className="mb-3 text-[11px] font-semibold uppercase tracking-wider text-text-muted">
          Rozetler ({badges.filter((badge) => badge.earned).length} / {badges.length})
        </h3>
        <ul className="grid grid-cols-2 gap-2 sm:grid-cols-5">
          {badges.map((badge) => (
            <li
              key={badge.id}
              title={badge.description}
              className={`rounded-xl border p-2.5 text-center ${badge.earned ? 'border-primary/50 bg-primary/10' : 'border-border opacity-45'}`}
            >
              <span className="block text-lg" aria-hidden>{badge.earned ? '🏅' : '🔒'}</span>
              <span className="block text-[11px] font-semibold text-text-primary">{badge.label}</span>
              <span className="sr-only">{badge.earned ? 'Kazanıldı: ' : 'Kilitli: '}{badge.description}</span>
            </li>
          ))}
        </ul>
      </div>
    </section>
  )
}
