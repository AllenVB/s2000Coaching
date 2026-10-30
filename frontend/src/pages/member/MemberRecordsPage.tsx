import { useMemberRecordSource } from '@/api/monthlyRecordApi'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { MonthlyRecordsPanel } from '@/components/records/MonthlyRecordsPanel'
import { PageHeading } from '@/components/ui/PageHeading'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'

/** "Verilerim": the member's month-by-month tables of height, weight and circumferences. */
export function MemberRecordsPage() {
  const { member } = useMemberProfileSync()
  const source = useMemberRecordSource()

  return (
    <>
      <PageHeading
        title="Verilerim"
        description="Her ay için bir tablo doldurun: boy, kilo ve çevre ölçüleriniz. Kaydettiğiniz ayları kalem simgesiyle düzenleyebilir, çöp kutusuyla silebilirsiniz."
      />
      {/* Body figures are health data: nothing is entered before the member has agreed to that. */}
      {member && !member.healthDataConsentAt ? <HealthDataConsent /> : <MonthlyRecordsPanel source={source} />}
    </>
  )
}
