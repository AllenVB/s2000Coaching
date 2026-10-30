import { useState } from 'react'

import { useMyNutritionPlan } from '@/api/nutritionApi'
import { useMemberWeeklyNutrition } from '@/api/weeklyNutritionApi'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { DayTypeTabs } from '@/components/nutrition/DayTypeTabs'
import { WeeklyNutritionAlbum } from '@/components/nutrition/WeeklyNutritionAlbum'
import { MacroSummary } from '@/components/nutrition/MacroSummary'
import { NutritionTable } from '@/components/nutrition/NutritionTable'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { PrintLink } from '@/components/print/PrintLink'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { dayTypeLabel, type NutritionDayType } from '@/types/nutrition'

export function MemberNutritionPage() {
  const { data: plan, isLoading, isError, error } = useMyNutritionPlan()
  const { member } = useMemberProfileSync()
  const weeklySource = useMemberWeeklyNutrition()
  // The plan is read-only here: the member picks which kind of day to look at, nothing more.
  const [dayType, setDayType] = useState<NutritionDayType>('TRAINING')
  // What is eaten is health data like measurements: nothing is shown or written before consent.
  const weeklyAlbum = member?.healthDataConsentAt ? <WeeklyNutritionAlbum source={weeklySource} /> : <HealthDataConsent />

  if (isLoading) {
    return (
      <>
        <PageHeading title="Beslenme" />
        <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
      </>
    )
  }

  if (isError) {
    return (
      <>
        <PageHeading title="Beslenme" />
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Planınız yüklenemedi: {error.message}
        </p>
      </>
    )
  }

  if (!plan) {
    return (
      <>
        <PageHeading title="Beslenme" />
        <ComingSoonCard
          title="Beslenme Planınız Hazırlanıyor"
          description="Koçunuz planınızı yüklediğinde makro hedefleriniz ve öğünleriniz burada görünecek."
        />
        <div className="mt-6">{weeklyAlbum}</div>
      </>
    )
  }

  return (
    <>
      <PageHeading
        title={plan.name}
        description={plan.note ?? 'Günlük hedefleriniz ve öğünleriniz.'}
        action={<PrintLink />}
      />

      <div className="space-y-6">
        <DayTypeTabs value={dayType} onChange={setDayType} />

        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">
            {dayTypeLabel(dayType)} Hedefleri
          </h2>
          <MacroSummary plan={plan} dayType={dayType} />
        </section>

        <NutritionTable plan={plan} dayType={dayType} />

        {weeklyAlbum}
      </div>
    </>
  )
}
