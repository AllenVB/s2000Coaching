import { useSearchParams } from 'react-router-dom'

import type { Member } from '@/types/member'

import { useDeleteMyMeasurement, useMyMeasurements } from '@/api/measurementsApi'
import { useMemberRecordSource } from '@/api/monthlyRecordApi'
import { MeasurementProgress } from '@/components/charts/MeasurementProgress'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { MeasurementHistory } from '@/components/member/MeasurementHistory'
import { WeeklyCheckForm } from '@/components/member/WeeklyCheckForm'
import { MonthlyRecordsPanel } from '@/components/records/MonthlyRecordsPanel'
import { MemberPhotosPage } from '@/pages/member/MemberPhotosPage'
import { PageHeading } from '@/components/ui/PageHeading'
import { SectionTabs, tabFromParam } from '@/components/ui/SectionTabs'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'

const TABS = [
  { value: 'aylik', label: 'Aylık Ölçüler' },
  { value: 'haftalik', label: 'Haftalık Takip' },
  { value: 'fotograflar', label: 'Fotoğraflar' },
] as const

/**
 * "Ölçümlerim": the monthly body tables (final once saved, past months stay on the page) and the
 * weekly follow-up (weight, how the week went, wishes and complaints) with its chart and history.
 */
export function MemberMeasurementsPage() {
  const { member } = useMemberProfileSync()
  const [params, setParams] = useSearchParams()
  const tab = tabFromParam(params.get('sekme'), [...TABS])

  if (!member) {
    return null
  }

  return (
    <>
      <PageHeading
        title="Ölçümlerim"
        description="Aylık vücut ölçülerinizi, haftalık kilo ile değerlendirmenizi ve gelişim fotoğraflarınızı buradan girersiniz; geçmiş kayıtlarınız altında durur."
      />
      {/* Body figures and weight are health data: nothing is shown or written before the member agrees. */}
      {!member.healthDataConsentAt ? (
        <HealthDataConsent />
      ) : (
        <>
          <SectionTabs label="Ölçüm türü" tabs={[...TABS]} value={tab} onChange={(next) => setParams({ sekme: next }, { replace: true })} />
          {tab === 'aylik' ? <MonthlyTab gender={member.gender} heightCm={member.heightCm} /> : tab === 'haftalik' ? <WeeklyTab /> : <MemberPhotosPage embedded />}
        </>
      )}
    </>
  )
}

function MonthlyTab({ gender, heightCm }: { gender: Member['gender']; heightCm: number | null }) {
  const source = useMemberRecordSource()
  return (
    <div className="space-y-4">
      <p className="rounded-xl border border-border bg-surface p-4 text-xs leading-relaxed text-text-secondary">
        Her ay için bir tablo doldurun. <strong className="text-text-primary">Kaydettiğiniz veriler sonradan değiştirilemez</strong>;
        kaydetmeden önce doğruluğunu onaylamanız istenir. Geçmiş aylar için de giriş yapabilirsiniz.
      </p>
      <MonthlyRecordsPanel source={source} immutable gender={gender} profileHeightCm={heightCm} />
    </div>
  )
}

function WeeklyTab() {
  const { data: measurements, isLoading, isError, error } = useMyMeasurements()
  const deleteMeasurement = useDeleteMyMeasurement()

  return (
    <div className="space-y-6">
      <WeeklyCheckForm />
      {isLoading ? (
        <div className="h-48 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Ölçümler yüklenemedi: {error.message}
        </p>
      ) : measurements ? (
        <>
          <MeasurementProgress measurements={measurements} />
          <MeasurementHistory
            measurements={measurements}
            onDelete={(measurementId) => deleteMeasurement.mutate({ measurementId })}
            isDeleting={deleteMeasurement.isPending}
            deleteError={deleteMeasurement.isError ? deleteMeasurement.error.message : null}
          />
        </>
      ) : null}
    </div>
  )
}
