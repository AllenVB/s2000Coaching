import { useParams } from 'react-router-dom'

import { useMember } from '@/api/adminMembersApi'
import { useMemberMeasurements } from '@/api/measurementsApi'
import { useMemberNutritionPlans } from '@/api/nutritionApi'
import { useMemberPlans } from '@/api/trainingApi'
import { PlanDocument } from '@/components/print/PlanDocument'
import { PrintToolbar } from '@/components/print/PrintToolbar'
import { todayIso } from '@/lib/date'

/** The coach's printable sheet for one member: the plans that are active for them right now. */
export function MemberPrintPage() {
  const { memberId = '' } = useParams<{ memberId: string }>()
  const { data: member, isLoading, isError, error } = useMember(memberId)
  const { data: nutritionPlans, isLoading: nutritionLoading } = useMemberNutritionPlans(memberId)
  const { data: trainingPlans, isLoading: trainingLoading } = useMemberPlans(memberId)
  const { data: measurements } = useMemberMeasurements(memberId)

  const back = `/yonetim/uyeler/${memberId}`

  if (isLoading || nutritionLoading || trainingLoading) {
    return <div className="h-64 animate-pulse rounded-2xl border border-border bg-surface" />
  }

  if (isError || !member) {
    return (
      <>
        <PrintToolbar backTo="/yonetim/uyeler" backLabel="Üyeler" />
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Üye yüklenemedi{error ? `: ${error.message}` : '.'}
        </p>
      </>
    )
  }

  return (
    <>
      <PrintToolbar backTo={back} backLabel={member.fullName} />
      <PlanDocument
        member={member}
        measurements={measurements ?? []}
        nutrition={nutritionPlans?.find((plan) => plan.active) ?? null}
        training={trainingPlans?.find((plan) => plan.active) ?? null}
        issuedOn={todayIso()}
      />
    </>
  )
}
