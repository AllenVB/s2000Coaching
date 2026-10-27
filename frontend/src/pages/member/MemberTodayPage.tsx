import { Link } from 'react-router-dom'

import type { TrainingPlan } from '@/types/training'

import { useMyMeasurements } from '@/api/measurementsApi'
import { useMyNutritionPlan } from '@/api/nutritionApi'
import { useMyTrainingPlan } from '@/api/trainingApi'
import { RenewalNotice } from '@/components/member/MembershipCard'
import { MeasurementReminder } from '@/components/member/MeasurementReminder'
import { TodayMacrosCard } from '@/components/member/TodayMacrosCard'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { formatDate } from '@/lib/date'
import { AppleIcon, CameraIcon, ChartIcon, ClipboardIcon, DumbbellIcon, HistoryIcon } from '@/components/ui/icons'

/** The member's landing screen: what to do today, then the shortcuts into each section. */
export function MemberTodayPage() {
  const { member, error } = useMemberProfileSync()
  const { data: plan } = useMyTrainingPlan()
  const { data: nutritionPlan } = useMyNutritionPlan()
  const { data: measurements } = useMyMeasurements()

  if (!member) {
    return null
  }

  return (
    <>
      <PageHeading
        title={`Merhaba, ${member.fullName.split(' ')[0]}`}
        description="Bugünün programı ve kısayolların."
      />

      {error ? (
        <p className="mb-6 rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Bilgileriniz yenilenemedi: {error.message}
        </p>
      ) : null}

      <div className="space-y-6">
        <RenewalNotice />

        {/* Only once the list has loaded: an empty array while loading would claim the
            member has never checked in. */}
        {measurements ? <MeasurementReminder measurements={measurements} /> : null}

        {plan ? <NextWorkoutCard plan={plan} /> : (
          <ComingSoonCard
            title="Antrenman Planınız Hazırlanıyor"
            description="Koçunuz planınızı yüklediğinde günün programı burada görünecek."
          />
        )}

        {nutritionPlan ? <TodayMacrosCard plan={nutritionPlan} /> : null}

        <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-6">
          <ShortcutCard to="/uye/antrenman" label="Antrenman" icon={DumbbellIcon} />
          <ShortcutCard to="/uye/beslenme" label="Beslenme" icon={AppleIcon} />
          <ShortcutCard to="/uye/olcumler" label="Ölçümler" icon={ChartIcon} />
          <ShortcutCard to="/uye/haftalik" label="Haftalık" icon={ClipboardIcon} />
          <ShortcutCard to="/uye/gecmis" label="Geçmiş" icon={HistoryIcon} />
          <ShortcutCard to="/uye/fotograflar" label="Fotoğraflar" icon={CameraIcon} />
        </div>
      </div>
    </>
  )
}

/**
 * The day to do next: the one least recently performed, which is what a rotating split
 * actually means in practice.
 */
function NextWorkoutCard({ plan }: { plan: TrainingPlan }) {
  const next = [...plan.days].sort((a, b) => {
    if (a.lastPerformedOn === b.lastPerformedOn) return a.dayOrder - b.dayOrder
    if (!a.lastPerformedOn) return -1
    if (!b.lastPerformedOn) return 1
    return a.lastPerformedOn.localeCompare(b.lastPerformedOn)
  })[0]

  if (!next) {
    return (
      <ComingSoonCard
        title={plan.name}
        description="Planınıza henüz gün eklenmedi; koçunuz eklediğinde burada görünecek."
      />
    )
  }

  return (
    <section className="rounded-2xl border border-primary/30 bg-surface p-6">
      <span className="text-[11px] font-semibold uppercase tracking-wider text-primary">
        Sıradaki Antrenman
      </span>
      <h2 className="mt-1.5 font-display text-xl font-bold uppercase tracking-tight text-text-primary">
        {next.name}
      </h2>
      <p className="mt-1.5 text-sm text-text-secondary">
        {next.exercises.length} hareket ·{' '}
        {next.lastPerformedOn ? `son yapılış ${formatDate(next.lastPerformedOn)}` : 'henüz yapılmadı'}
      </p>
      <Link
        to={`/uye/antrenman/${next.id}`}
        className="mt-4 inline-flex h-11 items-center justify-center rounded-xl bg-primary px-5 text-sm font-semibold uppercase tracking-wide text-canvas transition-colors hover:bg-primary-hover"
      >
        Antrenmana Başla
      </Link>
    </section>
  )
}

function ShortcutCard({
  to,
  label,
  icon: Icon,
}: {
  to: string
  label: string
  icon: React.ComponentType<{ className?: string }>
}) {
  return (
    <Link
      to={to}
      className="flex items-center gap-3 rounded-2xl border border-border bg-surface p-5 transition-colors hover:border-white/20"
    >
      <span className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary/10 text-primary">
        <Icon className="h-5 w-5" />
      </span>
      <span className="font-semibold text-text-primary">{label}</span>
    </Link>
  )
}
