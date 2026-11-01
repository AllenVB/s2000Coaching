import { useState } from 'react'

import { ProgressChart } from '@/components/charts/ProgressChart'
import { formatDate } from '@/lib/date'
import type { TrainingStats } from '@/types/training'

/**
 * Records and weekly volume. Every record is an estimate from the sets logged — the page says
 * how it is estimated and shows nothing for a lift that has no usable set, rather than a number
 * that looks like a measurement.
 */
export function TrainingStatsView({ stats }: { stats: TrainingStats }) {
  const withBest = stats.exercises.filter((exercise) => exercise.best !== null)
  const [open, setOpen] = useState<Set<string>>(new Set())
  const toggle = (id: string) =>
    setOpen((current) => {
      const next = new Set(current)
      if (!next.delete(id)) next.add(id)
      return next
    })
  const maxSets = Math.max(1, ...stats.weeklyVolume.map((week) => week.sets))
  const hasVolume = stats.weeklyVolume.some((week) => week.sets > 0)

  return (
    <div className="space-y-6">
      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
          Rekorlar (tahmini 1 tekrar maksimum)
        </h2>
        <p className="mb-4 text-xs leading-relaxed text-text-muted">
          Kayıtlı setlerden Epley formülüyle hesaplanır (ağırlık × (1 + tekrar / 30)); yalnızca 12 tekrar ve
          altındaki, ağırlığı girilmiş setler kullanılır. Gerçek 1 tekrar maksimumunuz değildir.
        </p>
        {withBest.length === 0 ? (
          <p className="text-sm italic text-text-muted">
            Rekor hesaplamak için ağırlıklı set kaydı gerekiyor.
          </p>
        ) : (
          <ul className="space-y-2">
            {withBest.map((exercise) => (
              <li
                key={exercise.exerciseId}
                className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface-elevated px-4 py-3"
              >
                <span className="min-w-0">
                  <span className="flex items-center gap-2 font-semibold text-text-primary">
                    {exercise.name}
                    {exercise.newRecordInLastSession ? (
                      <span className="rounded-full bg-success/15 px-2 py-0.5 text-[11px] font-semibold text-success">
                        Yeni rekor
                      </span>
                    ) : null}
                  </span>
                  <span className="block text-xs text-text-muted">
                    {exercise.best!.weightKg} kg × {exercise.best!.reps} · {formatDate(exercise.best!.on)} ·{' '}
                    {exercise.sessions} seans
                  </span>
                </span>
                <span className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => toggle(exercise.exerciseId)}
                    aria-expanded={open.has(exercise.exerciseId)}
                    aria-label={`${exercise.name} gelişim grafiği`}
                    className="text-xs font-semibold uppercase tracking-wide text-primary hover:underline"
                  >
                    {open.has(exercise.exerciseId) ? 'Grafiği gizle' : 'Gelişim'}
                  </button>
                  <span className="font-display text-xl font-extrabold text-text-primary">
                    {exercise.best!.estimatedOneRepMaxKg} kg
                  </span>
                </span>
                {open.has(exercise.exerciseId) ? (
                  <div className="w-full rounded-xl border border-border bg-canvas p-3">
                    {exercise.progress.length < 2 ? (
                      <p className="p-2 text-xs text-text-muted">Çizgi için en az iki seanslık ağırlıklı kayıt gerekir; şimdilik {exercise.progress.length} var.</p>
                    ) : (
                      <ProgressChart
                        points={exercise.progress.map((point) => ({ date: point.on, value: point.estimatedOneRepMaxKg }))}
                        unit="kg"
                        label={`${exercise.name} tahmini 1 tekrar maksimum`}
                      />
                    )}
                  </div>
                ) : null}
              </li>
            ))}
          </ul>
        )}
      </section>

      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
          Haftalık Set Hacmi (son {stats.weeklyVolume.length} hafta)
        </h2>
        <p className="mb-4 text-xs text-text-muted">Haftada yapılan toplam set sayısı. Günde 4–12 farklı hareket yapıldığı için kaldırılan toplam kiloyu değil, hareketlerden bağımsız olan set sayısını esas alırız; fare ile üzerine gelince ağırlıklı setlerin tonajı da görünür.</p>
        {!hasVolume ? (
          <p className="text-sm italic text-text-muted">Bu dönemde kayıtlı antrenman yok.</p>
        ) : (
          <div
            className="flex h-32 items-end gap-1.5"
            role="img"
            aria-label="Haftalık set sayısı çubuk grafiği"
          >
            {stats.weeklyVolume.map((week) => (
              <div key={week.weekStart} className="group flex h-full flex-1 flex-col justify-end">
                <div
                  className="w-full rounded-t bg-primary/80 transition-colors group-hover:bg-primary"
                  style={{ height: `${Math.max(week.sets > 0 ? 4 : 0, (week.sets / maxSets) * 100)}%` }}
                  title={`${formatDate(week.weekStart)} haftası: ${week.sets} set${week.volumeKg > 0 ? ` · ağırlıklı setlerde ${week.volumeKg} kg` : ''}`}
                />
              </div>
            ))}
          </div>
        )}
      </section>
    </div>
  )
}
