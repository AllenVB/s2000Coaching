import { useDeleteMyMeasurement, useMyMeasurements } from '@/api/measurementsApi'
import { MeasurementProgress } from '@/components/charts/MeasurementProgress'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { MeasurementForm } from '@/components/member/MeasurementForm'
import { MeasurementHistory } from '@/components/member/MeasurementHistory'
import { PageHeading } from '@/components/ui/PageHeading'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'

export function MemberMeasurementsPage() {
  const { member } = useMemberProfileSync()
  const { data: measurements, isLoading, isError, error } = useMyMeasurements()
  const deleteMeasurement = useDeleteMyMeasurement()

  if (!member) {
    return null
  }

  // Health data is special-category under KVKK: nothing is shown or written before consent.
  if (!member.healthDataConsentAt) {
    return (
      <>
        <PageHeading
          title="Ölçümler"
          description="Haftalık ölçümlerinizi girmeye başlamak için önce onay vermeniz gerekiyor."
        />
        <HealthDataConsent />
      </>
    )
  }

  return (
    <>
      <PageHeading
        title="Ölçümler"
        description="Haftada bir, tercihen aynı gün ve aynı saatte ölçün — eğilimi okunur kılan budur."
      />

      <div className="space-y-6">
        <MeasurementForm />

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
    </>
  )
}
