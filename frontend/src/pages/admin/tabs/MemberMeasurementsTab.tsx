import { useDeleteMemberMeasurement, useMemberMeasurements } from '@/api/measurementsApi'
import { MeasurementProgress } from '@/components/charts/MeasurementProgress'
import { MeasurementHistory } from '@/components/member/MeasurementHistory'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'
import { formatDate } from '@/lib/date'

/** Coach-side read of the member's check-ins. The coach never writes them, only tidies. */
export function MemberMeasurementsTab() {
  const { member } = useMemberDetail()
  const { data: measurements, isLoading, isError, error } = useMemberMeasurements(member.id)
  const deleteMeasurement = useDeleteMemberMeasurement(member.id)

  if (isLoading) {
    return <div className="h-48 animate-pulse rounded-2xl border border-border bg-surface" />
  }

  if (isError) {
    return (
      <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
        Ölçümler yüklenemedi: {error.message}
      </p>
    )
  }

  const latest = measurements?.[0]

  return (
    <div className="space-y-6">
      {!member.healthDataConsentAt ? (
        <p className="rounded-xl border border-warning/40 bg-warning/5 p-3.5 text-xs leading-relaxed text-text-secondary">
          Üye henüz sağlık verisi işleme onayını vermedi, bu yüzden ölçüm giremiyor. Onayı üye alanındaki
          Ölçümler sayfasından kendisi vermelidir.
        </p>
      ) : latest ? (
        <p className="text-xs text-text-muted">
          Son ölçüm: <span className="text-text-primary">{formatDate(latest.measuredOn)}</span> ·{' '}
          {latest.weightKg} kg
        </p>
      ) : null}

      {measurements && measurements.length > 0 ? (
        <MeasurementProgress measurements={measurements} />
      ) : null}

      <MeasurementHistory
        measurements={measurements ?? []}
        onDelete={(measurementId) => deleteMeasurement.mutate({ measurementId })}
        isDeleting={deleteMeasurement.isPending}
        deleteError={deleteMeasurement.isError ? deleteMeasurement.error.message : null}
      />
    </div>
  )
}
