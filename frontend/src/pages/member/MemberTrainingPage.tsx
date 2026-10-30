import { Link } from 'react-router-dom'

import { useMyTrainingPlan } from '@/api/trainingApi'
import { PrintLink } from '@/components/print/PrintLink'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { formatDate } from '@/lib/date'

export function MemberTrainingPage() {
  const { data: plan, isLoading, isError, error } = useMyTrainingPlan()

  if (isLoading) {
    return (
      <>
        <PageHeading title="Antrenman" />
        <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
      </>
    )
  }

  if (isError) {
    return (
      <>
        <PageHeading title="Antrenman" />
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Planınız yüklenemedi: {error.message}
        </p>
      </>
    )
  }

  if (!plan) {
    return (
      <>
        <PageHeading title="Antrenman" />
        <ComingSoonCard
          title="Planınız Hazırlanıyor"
          description="Koçunuz antrenman planınızı yüklediğinde günler ve hareketler burada listelenecek."
        />
      </>
    )
  }

  return (
    <>
      <PageHeading
        title={plan.name}
        description={plan.note ?? 'Çalışacağınız günü seçin.'}
        action={<PrintLink />}
      />

      <div className="space-y-3">
        {plan.days.map((day) => (
          <Link
            key={day.id}
            to={`/uye/antrenman/${day.id}`}
            className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-border bg-surface p-5 transition-colors hover:border-white/20"
          >
            <span className="min-w-0">
              <span className="block font-semibold text-text-primary">{day.name}</span>
              <span className="mt-0.5 block text-xs text-text-muted">
                {day.exercises.length} hareket
                {day.lastPerformedOn ? ` · son: ${formatDate(day.lastPerformedOn)}` : ' · henüz yapılmadı'}
              </span>
            </span>
            <span className="text-xs font-semibold uppercase tracking-wide text-primary">Başla →</span>
          </Link>
        ))}

        {plan.days.length === 0 ? (
          <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
            Planınıza henüz gün eklenmedi.
          </p>
        ) : null}
      </div>
    </>
  )
}
