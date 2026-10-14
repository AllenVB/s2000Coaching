import { ProgressChart } from '@/components/charts/ProgressChart'
import { ScatterChart } from '@/components/charts/ScatterChart'
import { TrendWeightChart } from '@/components/charts/TrendWeightChart'
import { PERIOD_LABELS, type BodyAnalysis, type WeeklyRow } from '@/types/caseStudy'

/**
 * The whole analysis, as both the coach preview and the public page render it.
 *
 * <p>The running rule: where the engine returned no number it returned a reason, and the
 * reason is shown in the number's place. An empty panel would read as "nothing happened"
 * rather than "there is not enough data to say", and those are very different claims. The
 * coverage box comes first for the same reason — a maintenance-calorie figure from four
 * logged months sitting next to a two-year weight chart is the easiest misreading here, and
 * the only defence is saying plainly what each part covers.
 */
export function AnalysisReport({ analysis }: { analysis: BodyAnalysis }) {
  const { coverage } = analysis

  return (
    <div className="space-y-10">
      <CoverageBox analysis={analysis} />

      {analysis.caveats.length > 0 ? (
        <Block title="Veriyle ilgili uyarılar">
          <ul className="space-y-2 text-sm text-text-secondary">
            {analysis.caveats.map((caveat, index) => (
              <li key={index} className="flex gap-2">
                <span className="text-accent-orange">•</span>
                <span>{caveat}</span>
              </li>
            ))}
          </ul>
        </Block>
      ) : null}

      {analysis.dailySeries.length > 0 ? (
        <Block
          title="Kilo ve trend"
          note="Gri noktalar her tartı, çizgi yumuşatılmış trend. Yumuşatma ölçümler arası gün farkına göre ölçeklenir; haftada bir tartılan dönemle her gün tartılan dönem zamana göre aynı ağırlıkta yumuşatılır. Bunun doğal sonucu: haftada bir tartılan dönemde çizgi noktalara yakın seyreder, çünkü o haftadan ortalaması alınacak tek bir ölçüm vardır."
        >
          <TrendWeightChart points={analysis.dailySeries} />
        </Block>
      ) : null}

      {analysis.longTermPeriods.length > 0 ? (
        <Block
          title="Dönemler"
          note="Uzun kaydın tek yöne giden parçaları. Kalori verisi gerekmediği için tüm geçmişi kapsar."
        >
          <Table
            head={['Başlangıç', 'Bitiş', 'Yön', 'Kilo değişimi', 'Bel değişimi', 'Bel / kilo']}
            rows={analysis.longTermPeriods.map((period) => [
              formatDate(period.start),
              formatDate(period.end),
              PERIOD_LABELS[period.label],
              `${signed(period.weightChangeKg, 1)} kg`,
              period.waistChangeCm === null ? '—' : `${signed(period.waistChangeCm, 1)} cm`,
              period.waistPerKg === null ? '—' : `${signed(period.waistPerKg, 2)} cm/kg`,
            ])}
          />
          <p className="mt-3 text-xs text-text-muted">
            Bel / kilo oranı, alınan ya da verilen kilonun neyden oluştuğuna dair kaba bir
            göstergedir; ölçüm hatası payı yüksektir.
          </p>
        </Block>
      ) : null}

      <TdeeBlock analysis={analysis} />
      <CalorieRelationBlock analysis={analysis} />
      <BucketBlock analysis={analysis} />

      {coverage.hasWaist ? (
        <Block title="Bel ölçüsü" note="Kilonun neyden oluştuğunu okumanın en basit yolu.">
          <ProgressChart
            points={analysis.dailySeries
              .filter((point) => point.waistCm !== null)
              .map((point) => ({ date: point.date, value: point.waistCm! }))}
            unit="cm"
            label="Bel"
          />
        </Block>
      ) : null}

      {analysis.weeklyRows.length > 0 ? <WeeklyTableBlock rows={analysis.weeklyRows} /> : null}

      <SodiumBlock analysis={analysis} />
      <CompositionBlock analysis={analysis} />
    </div>
  )
}

function CoverageBox({ analysis }: { analysis: BodyAnalysis }) {
  const { coverage } = analysis

  return (
    <section className="rounded-2xl border border-border-strong bg-surface-high p-5 sm:p-6">
      <h3 className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
        Bu analiz neyi kapsıyor
      </h3>
      <p className="mb-4 text-xs text-text-muted">
        Aşağıdaki her sayı yalnızca kendi penceresini anlatır. Kalori ile ilgili bulgular
        beslenme kaydının olduğu dönemle sınırlıdır; kilo ve çevre ölçümü bulguları tüm kaydı
        kapsar.
      </p>
      <dl className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Stat
          term="Ölçüm (kilo / çevre)"
          value={
            coverage.measurementFirst
              ? `${formatDate(coverage.measurementFirst)} – ${formatDate(coverage.measurementLast)}`
              : 'Veri yok'
          }
          hint={`${coverage.measurementDays} gün kayıt`}
        />
        <Stat
          term="Beslenme kaydı"
          value={
            coverage.intakeFirst
              ? `${formatDate(coverage.intakeFirst)} – ${formatDate(coverage.intakeLast)}`
              : 'Veri yok'
          }
          hint={
            coverage.intakeDays === 0
              ? 'Kalori analizi yapılamaz'
              : `${coverage.intakeDays} gün kayıt${
                  coverage.intakeMissingDays > 0
                    ? `, aralıkta ${coverage.intakeMissingDays} gün boş`
                    : ''
                }`
          }
        />
        <Stat
          term="Sodyum"
          value={coverage.sodiumDays === 0 ? 'Veri yok' : `${coverage.sodiumDays} gün`}
          hint={coverage.sodiumDays > 0 ? 'Yalnızca ön bulgu için yeterli' : undefined}
        />
        <Stat
          term="Çevre ölçümü"
          value={[coverage.hasNeck ? 'boyun' : null, coverage.hasWaist ? 'bel' : null]
            .filter(Boolean)
            .join(', ') || 'Yok'}
          hint={coverage.hasNeck ? 'Navy formülü kullanılabilir' : 'Navy formülü için boyun gerekir'}
        />
      </dl>
    </section>
  )
}

function TdeeBlock({ analysis }: { analysis: BodyAnalysis }) {
  const windows = analysis.tdeeWindows
  if (windows.length === 0) {
    return (
      <Block title="Bakım kalorisi (TDEE)">
        <Absent reason="Bakım kalorisi hesaplanamadı: aynı döneme denk gelen kalori kaydı ve kilo ölçümü yeterli değil." />
      </Block>
    )
  }

  const latest = windows[windows.length - 1]
  const values = windows.map((window) => window.estimatedTdee)
  const low = Math.min(...values)
  const high = Math.max(...values)

  return (
    <Block
      title="Bakım kalorisi (TDEE)"
      note="Her pencere için: ortalama alım − (trend kilo değişimi × 7700 / gün sayısı). Tek bir sayı yerine kayan pencereler, çünkü bakım kalorisi kütle ve aktivite değiştikçe kayar."
    >
      <div className="mb-5 grid grid-cols-1 gap-4 sm:grid-cols-3">
        <Stat
          term="Son pencere"
          value={`${Math.round(latest.estimatedTdee)} kcal`}
          hint={`${formatDate(latest.windowStart)} – ${formatDate(latest.windowEnd)} (${latest.days} gün)`}
        />
        <Stat
          term="Pencereler arası aralık"
          value={`${Math.round(low)} – ${Math.round(high)} kcal`}
          hint={`${windows.length} pencere`}
        />
        <Stat
          term="Son pencerede alım"
          value={`${Math.round(latest.avgCalories)} kcal`}
          hint={`trend kilo ${signed(latest.trendWeightChangeKg, 2)} kg`}
        />
      </div>
      <ProgressChart
        points={windows.map((window) => ({
          date: window.windowEnd,
          value: window.estimatedTdee,
        }))}
        unit="kcal"
        label="Tahmini bakım kalorisi"
      />
      <p className="mt-3 text-xs text-text-muted">
        Pencereler arasındaki yayılım, tahminin kendi hata payıdır: tek bir rakam olarak
        okunmamalı. Rakam yalnızca yukarıdaki beslenme kaydı dönemini anlatır.
      </p>
    </Block>
  )
}

function CalorieRelationBlock({ analysis }: { analysis: BodyAnalysis }) {
  const lag = analysis.lagFinding
  const points = analysis.weeklyRows
    .filter((row) => row.avgCalories !== null && row.trendWeightDelta !== null)
    .map((row) => ({
      x: row.avgCalories!,
      y: row.trendWeightDelta!,
      label: `${formatDate(row.weekStart)} haftası`,
    }))

  return (
    <Block
      title="Kalori ve kilo değişimi"
      note="Haftalık ortalama alım, o haftanın trend kilo değişimine karşı."
    >
      {points.length === 0 ? (
        <Absent reason="Eşleşen haftalık veri yok: kalori kaydı ile kilo ölçümü aynı haftalara denk gelmiyor." />
      ) : (
        <>
          <ScatterChart
            points={points}
            xLabel="Haftalık ortalama alım"
            yLabel="Trend kilo değişimi"
            xUnit="kcal"
            yUnit="kg"
          />
          <div className="mt-5 grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Stat
              term="Aynı hafta"
              value={formatCorrelation(lag.correlationSameWeek)}
              hint={lag.sampleSize ? `${lag.sampleSize} hafta` : undefined}
            />
            <Stat
              term="Sonraki hafta"
              value={formatCorrelation(lag.correlationNextWeek)}
              hint="Alımın tartıya gecikmeli yansıması"
            />
            <Stat
              term="Daha iyi uyan"
              value={
                lag.betterFit === null
                  ? '—'
                  : lag.betterFit === 'AYNI_HAFTA'
                    ? 'Aynı hafta'
                    : 'Sonraki hafta'
              }
            />
          </div>
          <p className="mt-3 text-xs text-text-muted">{lag.note}</p>
        </>
      )}
    </Block>
  )
}

function BucketBlock({ analysis }: { analysis: BodyAnalysis }) {
  const buckets = analysis.calorieBuckets
  const flagged = buckets.filter((bucket) => bucket.gainedWithoutWaist)

  return (
    <Block
      title="Kalori aralıkları"
      note="Aralıklar bu kaydın kendi kalori dağılımından kesilir, hazır bir tablodan değil."
    >
      {buckets.length === 0 ? (
        <Absent reason="Kalori aralığı tablosu için yeterli hafta ya da yeterli kalori farkı yok (en az 4 hafta ve 100 kcal fark gerekir)." />
      ) : (
        <>
          <Table
            head={['Aralık', 'Hafta', 'Ortalama haftalık kilo', 'Ortalama haftalık bel']}
            rows={buckets.map((bucket) => [
              bucket.label,
              String(bucket.weekCount),
              bucket.avgWeeklyWeightDelta === null
                ? '—'
                : `${signed(bucket.avgWeeklyWeightDelta, 2)} kg`,
              bucket.avgWeeklyWaistDelta === null
                ? '—'
                : `${signed(bucket.avgWeeklyWaistDelta, 2)} cm`,
            ])}
            highlightRows={buckets.map((bucket) => bucket.gainedWithoutWaist)}
          />
          {flagged.length > 0 ? (
            <p className="mt-3 text-sm text-primary">
              {flagged.map((bucket) => bucket.label).join(', ')} aralığında kilo arttı, bel ölçüsü
              artmadı. Bu aralık, kilonun daha büyük bölümünün yağ dışı kütleden gelmiş olabileceğine
              işaret eder — {flagged.map((bucket) => bucket.weekCount).join(' / ')} haftaya dayanıyor,
              yani bir eğilim değil bir ipucu.
            </p>
          ) : (
            <p className="mt-3 text-xs text-text-muted">
              Kilonun arttığı hâlde bel ölçüsünün artmadığı bir aralık bu veride görünmüyor.
            </p>
          )}
        </>
      )}
    </Block>
  )
}

function WeeklyTableBlock({ rows }: { rows: WeeklyRow[] }) {
  return (
    <Block
      title="Haftalık tablo"
      note="En az 4 gün kayıt içeren haftalar. Daha az kayıtlı haftalar tablodan çıkarıldı; iki günden alınan ortalama yedi günden alınanla karşılaştırılamaz."
    >
      <div className="max-h-[32rem] overflow-auto rounded-xl border border-border">
        <table className="w-full text-left text-xs">
          <thead className="sticky top-0 bg-surface-high text-text-secondary">
            <tr>
              {['Hafta', 'Gün', 'Kcal', 'P', 'K', 'Y', 'Na', 'Trend kilo', 'Δ kilo', 'Δ bel'].map(
                (head) => (
                  <th key={head} className="whitespace-nowrap px-3 py-2 font-semibold">
                    {head}
                  </th>
                ),
              )}
            </tr>
          </thead>
          <tbody className="text-text-primary">
            {rows.map((row) => (
              <tr key={row.weekStart} className="border-t border-border">
                <td className="whitespace-nowrap px-3 py-2">{formatDate(row.weekStart)}</td>
                <td className="px-3 py-2 text-text-muted">{row.intakeDaysLogged}</td>
                <td className="px-3 py-2">{round(row.avgCalories, 0)}</td>
                <td className="px-3 py-2 text-text-muted">{round(row.avgProteinG, 0)}</td>
                <td className="px-3 py-2 text-text-muted">{round(row.avgCarbsG, 0)}</td>
                <td className="px-3 py-2 text-text-muted">{round(row.avgFatG, 0)}</td>
                <td className="px-3 py-2 text-text-muted">{round(row.avgSodiumMg, 0)}</td>
                <td className="px-3 py-2">{round(row.trendWeightEnd, 2)}</td>
                <td className="px-3 py-2">
                  {row.trendWeightDelta === null ? '—' : signed(row.trendWeightDelta, 2)}
                </td>
                <td className="px-3 py-2">
                  {row.waistDelta === null ? '—' : signed(row.waistDelta, 1)}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
      <p className="mt-3 text-xs text-text-muted">
        Δ sütunları yalnızca bir önceki haftayla karşılaştırılabildiğinde doldurulur. Araya giren
        boş bir hafta varsa tire gösterilir — yoksa iki haftalık değişim "haftalık" başlığı altında
        görünürdü.
      </p>
    </Block>
  )
}

function SodiumBlock({ analysis }: { analysis: BodyAnalysis }) {
  const sodium = analysis.sodiumFinding
  if (!sodium.sampleSize) {
    return null
  }

  return (
    <Block title="Sodyum (ön bulgu)">
      <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
        <Stat
          term="Ertesi gün kilo ile ilişki"
          value={formatCorrelation(sodium.correlationWithNextDayWeight)}
          hint={`${sodium.sampleSize} eşleşen gün`}
        />
        <Stat term="Kullanılan kilo" value="Ham tartı" hint="Aranan etki su; trend onu siler" />
      </div>
      <p className="mt-3 text-xs text-text-muted">{sodium.note}</p>
    </Block>
  )
}

function CompositionBlock({ analysis }: { analysis: BodyAnalysis }) {
  const composition = analysis.bodyComposition

  return (
    <Block title="Yağ oranı ve kas kütlesi tahmini">
      {composition.bodyFatPercent === null ? (
        <Absent reason={composition.note} />
      ) : (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
            <Stat
              term="Yağ oranı"
              value={`%${composition.bodyFatPercent.toFixed(1)} ± ${composition.uncertaintyPoints.toFixed(1)}`}
              hint={composition.method ?? undefined}
            />
            <Stat
              term="Yağsız kütle"
              value={`${composition.leanMassKg?.toFixed(1)} kg`}
              hint="Yağ olmayan her şey: kas, kemik, su"
            />
            <Stat
              term="Yağ kütlesi"
              value={`${composition.fatMassKg?.toFixed(1)} kg`}
              hint={composition.asOf ? formatDate(composition.asOf) : undefined}
            />
          </div>
          <p className="mt-3 text-xs text-text-muted">{composition.note}</p>
          <p className="mt-2 text-xs text-text-muted">
            Kas kütlesi ayrıca hesaplanmaz. Yağsız kütle "yağ olmayan her şey" olduğu için aynı
            hata payını taşır; onu ölçülmüş bir kas değeri gibi sunmak bu payı gizlemek olurdu.
            Mezura ile yağ oranı ölçülemez, yalnızca tahmin edilir.
          </p>
        </>
      )}
    </Block>
  )
}

// --- small pieces ----------------------------------------------------------------------

function Block({
  title,
  note,
  children,
}: {
  title: string
  note?: string
  children: React.ReactNode
}) {
  return (
    <section>
      <h3 className="text-sm font-bold uppercase tracking-wider text-text-primary">{title}</h3>
      {note ? <p className="mt-1 mb-4 text-xs text-text-muted">{note}</p> : <div className="mb-4" />}
      {children}
    </section>
  )
}

/** A stated absence, never an empty space: the two read as very different claims. */
function Absent({ reason }: { reason: string }) {
  return (
    <p className="rounded-xl border border-dashed border-border-strong bg-surface-high/50 p-4 text-sm text-text-secondary">
      {reason}
    </p>
  )
}

function Stat({ term, value, hint }: { term: string; value: string; hint?: string }) {
  return (
    <div>
      <dt className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">{term}</dt>
      <dd className="mt-0.5 text-sm font-semibold text-text-primary">{value}</dd>
      {hint ? <dd className="text-[11px] text-text-muted">{hint}</dd> : null}
    </div>
  )
}

function Table({
  head,
  rows,
  highlightRows,
}: {
  head: string[]
  rows: string[][]
  highlightRows?: boolean[]
}) {
  return (
    <div className="overflow-x-auto rounded-xl border border-border">
      <table className="w-full text-left text-xs">
        <thead className="bg-surface-high text-text-secondary">
          <tr>
            {head.map((cell) => (
              <th key={cell} className="whitespace-nowrap px-3 py-2 font-semibold">
                {cell}
              </th>
            ))}
          </tr>
        </thead>
        <tbody className="text-text-primary">
          {rows.map((row, rowIndex) => (
            <tr
              key={rowIndex}
              className={`border-t border-border ${highlightRows?.[rowIndex] ? 'bg-primary/5' : ''}`}
            >
              {row.map((cell, cellIndex) => (
                <td key={cellIndex} className="whitespace-nowrap px-3 py-2">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  )
}

function formatDate(date: string | null): string {
  return date
    ? new Date(date).toLocaleDateString('tr-TR', { day: '2-digit', month: 'short', year: 'numeric' })
    : '—'
}

function signed(value: number, decimals: number): string {
  return `${value > 0 ? '+' : ''}${value.toFixed(decimals)}`
}

function round(value: number | null, decimals: number): string {
  return value === null ? '—' : value.toFixed(decimals)
}

/** A correlation with no sample size behind it is not reported as a number at all. */
function formatCorrelation(value: number | null): string {
  return value === null ? 'Yeterli veri yok' : value.toFixed(2)
}
