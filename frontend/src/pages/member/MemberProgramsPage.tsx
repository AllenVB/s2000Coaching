import { useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { useMyNutritionPlan } from '@/api/nutritionApi'
import { useMyTrainingPlan } from '@/api/trainingApi'
import { RenewalNotice } from '@/components/member/MembershipCard'
import { DayTypeTabs } from '@/components/nutrition/DayTypeTabs'
import { MacroSummary } from '@/components/nutrition/MacroSummary'
import { NutritionTable } from '@/components/nutrition/NutritionTable'
import { PrintLink } from '@/components/print/PrintLink'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { SectionTabs, tabFromParam } from '@/components/ui/SectionTabs'
import { dayTypeLabel, type NutritionDayType } from '@/types/nutrition'

const TABS = [
  { value: 'antrenman', label: 'Antrenman' },
  { value: 'beslenme', label: 'Beslenme' },
] as const

/** "Programlarım": the training and nutrition programmes the coach assigned. The member only reads them. */
export function MemberProgramsPage() {
  const [params, setParams] = useSearchParams()
  const tab = tabFromParam(params.get('sekme'), [...TABS])

  return (
    <>
      <PageHeading
        title="Programlarım"
        description="Koçunuzun size atadığı antrenman ve beslenme programları. Buradan yalnızca görüntülenir; yaptıklarınızı Geçmiş sayfasına girersiniz."
        action={<PrintLink />}
      />
      <RenewalNotice />
      <div className="mt-6">
        <SectionTabs label="Program türü" tabs={[...TABS]} value={tab} onChange={(next) => setParams({ sekme: next }, { replace: true })} />
        {tab === 'antrenman' ? <TrainingProgram /> : <NutritionProgram />}
      </div>
    </>
  )
}

function TrainingProgram() {
  const { data: plan, isLoading, isError, error } = useMyTrainingPlan()

  if (isLoading) {
    return <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
  }
  if (isError) {
    return (
      <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
        Programınız yüklenemedi: {error.message}
      </p>
    )
  }
  if (!plan) {
    return (
      <ComingSoonCard
        title="Antrenman Programınız Hazırlanıyor"
        description="Koçunuz programınızı yüklediğinde günler ve hareketler burada listelenecek."
      />
    )
  }

  return (
    <section>
      <h2 className="text-lg font-bold text-text-primary">{plan.name}</h2>
      {plan.note ? <p className="mt-1 text-sm text-text-secondary">{plan.note}</p> : null}
      <div className="mt-4 space-y-3">
        {plan.days.map((day) => (
          <Link
            key={day.id}
            to={`/uye/programlarim/antrenman/${day.id}`}
            className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-border bg-surface p-5 transition-colors hover:border-white/20"
          >
            <span className="min-w-0">
              <span className="block font-semibold text-text-primary">{day.name}</span>
              <span className="mt-0.5 block text-xs text-text-muted">{day.exercises.length} hareket</span>
            </span>
            <span className="text-xs font-semibold uppercase tracking-wide text-primary">Görüntüle →</span>
          </Link>
        ))}
        {plan.days.length === 0 ? (
          <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
            Programınıza henüz gün eklenmedi.
          </p>
        ) : null}
      </div>
    </section>
  )
}

function NutritionProgram() {
  const { data: plan, isLoading, isError, error } = useMyNutritionPlan()
  const [dayType, setDayType] = useState<NutritionDayType>('TRAINING')

  if (isLoading) {
    return <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
  }
  if (isError) {
    return (
      <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
        Programınız yüklenemedi: {error.message}
      </p>
    )
  }
  if (!plan) {
    return (
      <ComingSoonCard
        title="Beslenme Programınız Hazırlanıyor"
        description="Koçunuz programınızı yüklediğinde makro hedefleriniz ve öğünleriniz burada görünecek."
      />
    )
  }

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-bold text-text-primary">{plan.name}</h2>
        {plan.note ? <p className="mt-1 text-sm text-text-secondary">{plan.note}</p> : null}
      </div>
      <DayTypeTabs value={dayType} onChange={setDayType} />
      <section className="rounded-2xl border border-border bg-surface p-6">
        <h3 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">
          {dayTypeLabel(dayType)} Hedefleri
        </h3>
        <MacroSummary plan={plan} dayType={dayType} />
      </section>
      <NutritionTable plan={plan} dayType={dayType} />
    </div>
  )
}
