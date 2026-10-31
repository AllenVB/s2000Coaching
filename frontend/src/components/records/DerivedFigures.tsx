import { derive } from '@/lib/bodyMetrics'
import type { MemberGender } from '@/types/member'
import type { MonthlyRecord } from '@/types/monthlyRecord'

/**
 * What follows from a month's figures, worked out on the spot: BMI, body fat, lean mass, FFMI and
 * shoulder-to-waist. Body fat is a tape-measure ESTIMATE (US Navy method) and so is everything built on
 * it; the block says so rather than presenting estimates as measurements.
 */
export function DerivedFigures({
  figures,
  gender,
  profileHeightCm,
}: {
  figures: Omit<MonthlyRecord, 'id' | 'period' | 'updatedAt'>
  gender: MemberGender | null
  profileHeightCm: number | null
}) {
  const d = derive({ id: '', period: '', updatedAt: '', ...figures }, gender, profileHeightCm)
  const rows: [string, string | null][] = [
    ['VKİ', d.bmi !== null ? d.bmi.toFixed(1) : null],
    ['Yağ oranı (tahmin)', d.bodyFatPercent !== null ? `%${d.bodyFatPercent.toFixed(1)}` : null],
    ['Yağsız kütle oranı', d.leanMassPercent !== null ? `%${d.leanMassPercent.toFixed(1)}` : null],
    ['Yağsız kütle', d.leanMassKg !== null ? `${d.leanMassKg.toFixed(1)} kg` : null],
    ['FFMI', d.ffmi !== null ? d.ffmi.toFixed(1) : null],
    ['Omuz / bel oranı', d.shoulderWaistRatio !== null ? d.shoulderWaistRatio.toFixed(2) : null],
  ]

  return (
    <div className="mt-4 rounded-xl border border-border bg-surface-elevated p-4" aria-live="polite">
      <h3 className="mb-2 text-[11px] font-bold uppercase tracking-wider text-text-secondary">
        Otomatik hesaplanan değerler
      </h3>
      <dl className="grid grid-cols-2 gap-x-4 gap-y-2 sm:grid-cols-3">
        {rows.map(([label, value]) => (
          <div key={label}>
            <dt className="text-[11px] text-text-muted">{label}</dt>
            <dd className="text-sm font-semibold text-text-primary">{value ?? '—'}</dd>
          </div>
        ))}
      </dl>
      <p className="mt-3 text-[11px] leading-relaxed text-text-muted">
        Yağ oranı boy, boyun ve bel (kadınlarda kalça) ölçünüzden tahmin edilir; yağsız kütle ve FFMI buna dayanır.
        Ölçüm cihazı değildir, birkaç puan sapabilir. Eksik ölçü varsa ilgili değer “—” görünür.
      </p>
    </div>
  )
}
