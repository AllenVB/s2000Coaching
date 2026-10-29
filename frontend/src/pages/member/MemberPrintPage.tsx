import { useMyMeasurements } from '@/api/measurementsApi'
import { useMyNutritionPlan } from '@/api/nutritionApi'
import { useMyTrainingPlan } from '@/api/trainingApi'
import { PlanDocument } from '@/components/print/PlanDocument'
import { PrintToolbar } from '@/components/print/PrintToolbar'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { todayIso } from '@/lib/date'

/** The member's own printable plan: the same sheet the coach can print for them. */
export function MemberPrintPage() {
  const { member } = useMemberProfileSync()
  const { data: nutrition, isLoading: nutritionLoading } = useMyNutritionPlan()
  const { data: training, isLoading: trainingLoading } = useMyTrainingPlan()
  // Measurements are health data, shown only once the member has agreed to that; without
  // consent the request is refused and the sheet simply has no measurements.
  const { data: measurements } = useMyMeasurements()

  if (!member || nutritionLoading || trainingLoading) {
    return <div className="h-64 animate-pulse rounded-2xl border border-border bg-surface" />
  }

  return (
    <>
      <PrintToolbar backTo="/uye" backLabel="Bugün" />
      <PlanDocument
        member={member}
        measurements={measurements ?? []}
        nutrition={nutrition ?? null}
        training={training ?? null}
        issuedOn={todayIso()}
      />
    </>
  )
}
