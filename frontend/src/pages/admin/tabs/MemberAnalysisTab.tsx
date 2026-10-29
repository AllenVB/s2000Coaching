import { useMemberAnalysis } from '@/api/adminMembersApi'
import { AnalysisReport } from '@/components/caseStudy/AnalysisReport'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'

/**
 * Trend weight, weekly change, an energy-expenditure estimate and how food relates to weight —
 * the same engine as the case studies, run on this member's own check-ins and food log.
 *
 * <p>Where the data cannot answer, the report says why instead of leaving a gap; that is what
 * tells "not enough logged yet" apart from "nothing happened". Body-composition estimates need
 * a sex and a neck measurement a member record does not carry, so that block will say so.
 */
export function MemberAnalysisTab() {
  const { member } = useMemberDetail()
  const { data, isLoading, isError, error } = useMemberAnalysis(member.id)

  if (isLoading) {
    return <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
  }
  if (isError || !data) {
    return <p className="text-xs text-error">Analiz yüklenemedi{error ? `: ${error.message}` : '.'}</p>
  }

  return (
    <div className="space-y-4">
      <p className="rounded-xl border border-border bg-surface p-4 text-xs leading-relaxed text-text-secondary">
        Bu analiz üyenin girdiği ölçüm ve yemek kayıtlarından hesaplanır; üye bunu görmez. Her sayı yalnızca
        kapsadığı tarih aralığı için geçerlidir ve veri yetmediğinde sayı yerine nedeni yazılır. Kalori
        kaydı eksik günler olabileceğinden harcama tahmini yaklaşıktır.
      </p>
      <AnalysisReport analysis={data} />
    </div>
  )
}
