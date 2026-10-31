import { useMemo, useState } from 'react'
import { Link, useSearchParams } from 'react-router-dom'

import { useMember } from '@/api/adminMembersApi'
import { useMemberActivity } from '@/api/activityApi'
import { useMemberCheckIns } from '@/api/checkInApi'
import { useMemberIntake } from '@/api/intakeApi'
import { useMemberMeasurements } from '@/api/measurementsApi'
import { useCoachRecordSource } from '@/api/monthlyRecordApi'
import { useMemberNutritionPlans } from '@/api/nutritionApi'
import { useMemberPhotos } from '@/api/photosApi'
import { StudentPicker } from '@/components/admin/StudentPicker'
import { ProgressChart, type ProgressPoint } from '@/components/charts/ProgressChart'
import { PhotoComparison } from '@/components/member/PhotoComparison'
import { Button } from '@/components/ui/Button'
import { PageHeading } from '@/components/ui/PageHeading'
import { derive } from '@/lib/bodyMetrics'
import { CoachPhotoSourceProvider } from '@/context/PhotoSourceContext'
import { formatDate } from '@/lib/date'
import { formatAmount } from '@/lib/weeklyNutrition'
import { CHECK_IN_SCALES } from '@/types/checkIn'
import { targetsOf } from '@/types/nutrition'
import { FIGURES, formatPeriod } from '@/types/monthlyRecord'

interface Metric {
  key: string
  label: string
  unit: string
  group: 'Aylık' | 'Haftalık' | 'Günlük'
  /** Worked out from the tape figures rather than measured. */
  estimate?: boolean
  decimals: number
  points: ProgressPoint[]
}

/** The first of the month, so a month sits on the chart's time axis like any date. */
const monthStart = (period: string) => `${period}-01`

/**
 * "Üye Verileri": everything one student has entered, as tables, and a chart for any figure on demand.
 * Each row of the figure list has a "Grafik Oluştur" button; pressing it draws that figure over time
 * right under the row. Body fat, lean mass, FFMI and the shoulder/waist ratio are worked out from the
 * monthly tape measurements (see `lib/bodyMetrics`) and are labelled as estimates.
 */
export function MemberDataPage() {
  const [params, setParams] = useSearchParams()
  const selectedId = params.get('uye') ?? undefined
  const { data: member } = useMember(selectedId)

  return (
    <>
      <PageHeading
        title="Üye Verileri"
        description="Öğrencinin haftalık ve aylık girdiği verileri inceleyin; istediğiniz değerin yanındaki Grafik Oluştur düğmesiyle zaman içindeki değişimini çizin."
      />
      <div className="grid gap-6 lg:grid-cols-[300px_minmax(0,1fr)]">
        <StudentPicker selectedId={selectedId} onSelect={(id) => setParams({ uye: id }, { replace: true })} />
        <section className="min-w-0">
          {member && selectedId ? (
            <MemberData key={selectedId} memberId={selectedId} />
          ) : (
            <p className="rounded-2xl border border-dashed border-border-strong p-10 text-center text-sm text-text-muted">
              {selectedId ? 'Yükleniyor…' : 'Soldan bir öğrenci seçin.'}
            </p>
          )}
        </section>
      </div>
    </>
  )
}

function MemberData({ memberId }: { memberId: string }) {
  const { data: member } = useMember(memberId)
  const monthly = useCoachRecordSource(memberId)
  const { data: measurements } = useMemberMeasurements(memberId)
  const { data: checkIns } = useMemberCheckIns(memberId)
  const { data: activity } = useMemberActivity(memberId)
  const { data: intakes } = useMemberIntake(memberId)
  const { data: plans } = useMemberNutritionPlans(memberId)
  const { data: photos } = useMemberPhotos(memberId)
  const activePlan = plans?.find((plan) => plan.active) ?? null
  const [shown, setShown] = useState<Set<string>>(new Set())

  const records = useMemo(() => [...(monthly.records ?? [])].sort((a, b) => a.period.localeCompare(b.period)), [monthly.records])
  const derivedByPeriod = useMemo(
    () => new Map(records.map((record) => [record.period, derive(record, member?.gender ?? null, member?.heightCm ?? null)])),
    [records, member],
  )

  const metrics = useMemo<Metric[]>(() => {
    const list: Metric[] = []
    for (const figure of FIGURES) {
      list.push({
        key: `m-${figure.key}`,
        label: figure.label,
        unit: figure.unit,
        group: 'Aylık',
        decimals: 1,
        points: records.flatMap((record) => (record[figure.key] === null ? [] : [{ date: monthStart(record.period), value: Number(record[figure.key]) }])),
      })
    }
    const fromDerived = (key: string, label: string, unit: string, decimals: number, pick: (d: ReturnType<typeof derive>) => number | null): Metric => ({
      key,
      label,
      unit,
      group: 'Aylık',
      estimate: true,
      decimals,
      points: records.flatMap((record) => {
        const value = pick(derivedByPeriod.get(record.period)!)
        return value === null ? [] : [{ date: monthStart(record.period), value }]
      }),
    })
    list.push(
      fromDerived('d-fat', 'Yağ yüzdesi', '%', 1, (d) => d.bodyFatPercent),
      fromDerived('d-leanpct', 'Yağsız kütle yüzdesi', '%', 1, (d) => d.leanMassPercent),
      fromDerived('d-leankg', 'Yağsız kütle', 'kg', 1, (d) => d.leanMassKg),
      fromDerived('d-ratio', 'Omuz / bel oranı', '', 2, (d) => d.shoulderWaistRatio),
      fromDerived('d-ffmi', 'FFMI', 'kg/m²', 1, (d) => d.ffmi),
      fromDerived('d-nffmi', 'FFMI (1,80 m’ye göre)', 'kg/m²', 1, (d) => d.normalizedFfmi),
    )
    list.push({
      key: 'w-weight',
      label: 'Kilo (haftalık tartı)',
      unit: 'kg',
      group: 'Haftalık',
      decimals: 1,
      points: (measurements ?? []).map((row) => ({ date: row.measuredOn, value: Number(row.weightKg) })).sort((a, b) => a.date.localeCompare(b.date)),
    })
    for (const scale of CHECK_IN_SCALES) {
      list.push({
        key: `w-${scale.key}`,
        label: `${scale.label} (1–5)`,
        unit: '',
        group: 'Haftalık',
        decimals: 0,
        points: (checkIns ?? []).map((row) => ({ date: row.weekStart, value: row[scale.key] })).sort((a, b) => a.date.localeCompare(b.date)),
      })
    }
    list.push({
      key: 'w-diet',
      label: 'Beslenmeye uyum',
      unit: '%',
      group: 'Haftalık',
      decimals: 0,
      points: (checkIns ?? []).flatMap((row) => (row.dietAdherencePercent === null ? [] : [{ date: row.weekStart, value: row.dietAdherencePercent }])).sort((a, b) => a.date.localeCompare(b.date)),
    })
    const byDate = <T extends { date: string }>(rows: T[]) => [...rows].sort((a, b) => a.date.localeCompare(b.date))
    list.push(
      {
        key: 'd-steps',
        label: 'Günlük adım',
        unit: 'adım',
        group: 'Günlük',
        decimals: 0,
        points: byDate((activity ?? []).flatMap((row) => (row.steps === null ? [] : [{ date: row.performedOn, value: row.steps }]))),
      },
      {
        key: 'd-cardio',
        label: 'Kardiyo süresi',
        unit: 'dk',
        group: 'Günlük',
        decimals: 0,
        points: byDate((activity ?? []).flatMap((row) => (row.cardioMinutes === null ? [] : [{ date: row.performedOn, value: row.cardioMinutes }]))),
      },
      {
        key: 'd-kcal',
        label: 'Günlük kalori',
        unit: 'kcal',
        group: 'Günlük',
        decimals: 0,
        points: byDate((intakes ?? []).map((row) => ({ date: row.consumedOn, value: row.calories }))),
      },
      {
        // Positive = over the target of the day type the member chose; days without a type or a target are left out.
        key: 'd-kcal-gap',
        label: 'Kalori − hedef (gün türüne göre)',
        unit: 'kcal',
        group: 'Günlük',
        decimals: 0,
        points: byDate(
          (intakes ?? []).flatMap((row) => {
            const target = activePlan && row.dayType ? targetsOf(activePlan, row.dayType === 'DINLENME' ? 'REST' : 'TRAINING').calories : null
            return target === null ? [] : [{ date: row.consumedOn, value: row.calories - target }]
          }),
        ),
      },
      {
        key: 'd-water',
        label: 'Su',
        unit: 'L',
        group: 'Günlük',
        decimals: 1,
        points: byDate((intakes ?? []).flatMap((row) => (row.waterLiters === null ? [] : [{ date: row.consumedOn, value: Number(row.waterLiters) }]))),
      },
    )
    return list
  }, [records, derivedByPeriod, measurements, checkIns, activity, intakes, activePlan])

  const toggle = (key: string) =>
    setShown((current) => {
      const next = new Set(current)
      if (!next.delete(key)) {
        next.add(key)
      }
      return next
    })

  if (!member) {
    return <p className="text-sm text-text-muted">Yükleniyor…</p>
  }
  const canEstimate = member.gender === 'MALE' || member.gender === 'FEMALE'

  return (
    <div className="space-y-6">
      <header className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-lg font-bold text-text-primary">{member.fullName}</h2>
        <Link to={`/yonetim/uyeler/${member.id}`} className="text-xs text-text-muted hover:text-primary">
          Üye sayfası →
        </Link>
      </header>

      <section className="rounded-2xl border border-border bg-surface p-5 sm:p-6">
        <h3 className="mb-3 text-sm font-bold uppercase tracking-wider text-text-primary">Aylık Tablolar</h3>
        {records.length === 0 ? (
          <p className="text-sm italic text-text-muted">Öğrenci henüz aylık tablo girmedi.</p>
        ) : (
          <div className="-mx-2 overflow-x-auto">
            <table className="w-full min-w-[820px] border-collapse text-sm">
              <thead>
                <tr className="border-b border-border text-[11px] font-semibold uppercase tracking-wider text-text-muted">
                  <th className="px-2 py-2 text-left">Ay</th>
                  {FIGURES.map((figure) => (
                    <th key={figure.key} className="px-2 py-2 text-right">
                      {figure.label.replace(' Çevresi', '')}
                    </th>
                  ))}
                  <th className="px-2 py-2 text-right">Yağ %*</th>
                  <th className="px-2 py-2 text-right">FFMI*</th>
                </tr>
              </thead>
              <tbody>
                {[...records].reverse().map((record) => {
                  const d = derivedByPeriod.get(record.period)!
                  return (
                    <tr key={record.id} className="border-b border-border/60">
                      <th scope="row" className="px-2 py-2 text-left font-semibold text-text-primary">
                        {formatPeriod(record.period)}
                      </th>
                      {FIGURES.map((figure) => (
                        <td key={figure.key} className="px-2 py-2 text-right text-text-secondary">
                          {record[figure.key] ?? '—'}
                        </td>
                      ))}
                      <td className="px-2 py-2 text-right text-text-primary">{d.bodyFatPercent === null ? '—' : formatAmount(round1(d.bodyFatPercent))}</td>
                      <td className="px-2 py-2 text-right text-text-primary">{d.ffmi === null ? '—' : formatAmount(round1(d.ffmi))}</td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>
        )}
        <p className="mt-3 text-[11px] leading-relaxed text-text-muted">
          * Tahmindir: yağ yüzdesi boy, boyun ve bel (kadınlarda kalça) çevresinden US Navy formülüyle hesaplanır;
          ölçüm değildir. Yağsız kütle ve FFMI bundan türediği için aynı belirsizliği taşır.
          {canEstimate ? '' : ' Bu öğrencinin cinsiyeti kayıtlı olmadığı için tahmin hesaplanamıyor (üye sayfasından girin).'}
        </p>
      </section>

      {(['Aylık', 'Haftalık', 'Günlük'] as const).map((group) => (
        <section key={group} className="rounded-2xl border border-border bg-surface p-5 sm:p-6">
          <h3 className="mb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
            {group} Veriler ve Grafikler
          </h3>
          <ul className="divide-y divide-border/60">
            {metrics.filter((metric) => metric.group === group).map((metric) => {
              const last = metric.points[metric.points.length - 1]
              const open = shown.has(metric.key)
              return (
                <li key={metric.key} className="py-3">
                  <div className="flex flex-wrap items-center justify-between gap-3">
                    <div className="min-w-0">
                      <span className="text-sm font-semibold text-text-primary">
                        {metric.label}
                        {metric.estimate ? <span className="ml-1.5 text-[11px] font-normal text-text-muted">tahmin</span> : null}
                      </span>
                      <span className="block text-xs text-text-muted">
                        {last
                          ? `Son: ${formatAmount(round(last.value, metric.decimals))}${metric.unit ? ` ${metric.unit}` : ''} · ${formatDate(last.date)} · ${metric.points.length} kayıt`
                          : 'Veri yok'}
                      </span>
                    </div>
                    <Button
                      type="button"
                      size="sm"
                      variant={open ? 'ghost' : 'secondary'}
                      disabled={metric.points.length === 0}
                      aria-expanded={open}
                      aria-label={`${metric.label} ${open ? 'grafiğini gizle' : 'grafiği oluştur'}`}
                      onClick={() => toggle(metric.key)}
                    >
                      {open ? 'Grafiği Gizle' : 'Grafik Oluştur'}
                    </Button>
                  </div>
                  {open ? (
                    <div className="mt-3 rounded-xl border border-border bg-canvas p-3">
                      {metric.points.length < 2 ? (
                        <p className="p-3 text-xs text-text-muted">Çizgi için en az iki kayıt gerekir; şimdilik {metric.points.length} kayıt var.</p>
                      ) : (
                        <ProgressChart points={metric.points} unit={metric.unit} label={metric.label} />
                      )}
                    </div>
                  ) : null}
                </li>
              )
            })}
          </ul>
        </section>
      ))}

      <CoachPhotoSourceProvider>
        <PhotoComparison photos={photos ?? []} />
      </CoachPhotoSourceProvider>
    </div>
  )
}

function round(value: number, decimals: number): number {
  const factor = 10 ** decimals
  return Math.round(value * factor) / factor
}

const round1 = (value: number) => round(value, 1)
