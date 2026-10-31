import { useCoachRecordSource } from '@/api/monthlyRecordApi'
import { MonthlyRecordsPanel } from '@/components/records/MonthlyRecordsPanel'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'

/** The member's monthly tables, which the coach can also fill in or correct on their behalf. */
export function MemberMonthlyRecordsTab() {
  const { member } = useMemberDetail()
  const source = useCoachRecordSource(member.id)

  return (
    <div className="space-y-4">
      <p className="text-xs leading-relaxed text-text-secondary">
        Üyenin aylık veri tabloları. Üye kendisi girer; isterseniz sizin de ekleyip düzeltmeniz mümkün. Üye sağlık
        verisi işleme onayını vermemişse kayıt eklenemez.
      </p>
      <MonthlyRecordsPanel source={source} gender={member.gender} profileHeightCm={member.heightCm} />
    </div>
  )
}
