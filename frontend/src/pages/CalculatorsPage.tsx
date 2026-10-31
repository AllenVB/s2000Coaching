import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'

import { WhatsAppButton } from '@/components/layout/WhatsAppButton'
import { SimplePublicHeader } from '@/components/layout/SimplePublicHeader'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import {
  ACTIVITY_LEVELS,
  BODY_LIMITS,
  GOALS,
  MACRO_SPLITS,
  bodyMassIndex,
  estimateCalories,
  type Body,
  type Sex,
} from '@/lib/calculators'
import { formatAmount, macrosOf } from '@/lib/weeklyNutrition'
import { parseFigure } from '@/types/monthlyRecord'

const TITLE = 'Ücretsiz Kalori, Makro ve VKİ Hesaplayıcı — S2000 Coaching'

/**
 * Public calculators: daily calories, macro split and body-mass index. Nothing typed here leaves
 * the browser or is stored. The results are textbook estimates, and the page says so.
 */
export function CalculatorsPage() {
  useEffect(() => {
    const previous = document.title
    document.title = TITLE
    return () => {
      document.title = previous
    }
  }, [])

  const [sex, setSex] = useState<Sex>('MALE')
  const [age, setAge] = useState('')
  const [height, setHeight] = useState('')
  const [weight, setWeight] = useState('')
  const [activityValue, setActivityValue] = useState('moderate')
  const [goalValue, setGoalValue] = useState('maintain')

  const ageValue = readInRange(age, BODY_LIMITS.age.min, BODY_LIMITS.age.max)
  const heightValue = readInRange(height, BODY_LIMITS.height.min, BODY_LIMITS.height.max)
  const weightValue = readInRange(weight, BODY_LIMITS.weight.min, BODY_LIMITS.weight.max)

  const body: Body | null =
    typeof ageValue === 'number' && typeof heightValue === 'number' && typeof weightValue === 'number'
      ? { sex, ageYears: ageValue, heightCm: heightValue, weightKg: weightValue }
      : null

  const activity = ACTIVITY_LEVELS.find((level) => level.value === activityValue) ?? ACTIVITY_LEVELS[2]
  const goal = GOALS.find((entry) => entry.value === goalValue) ?? GOALS[1]
  const calories = body ? estimateCalories(body, activity, goal) : null
  const bmi = body ? bodyMassIndex(body.heightCm, body.weightKg) : null
  // The formulas and the BMI bands are for adults; a growing body needs age-and-sex percentiles.
  const isMinor = body !== null && body.ageYears < 18

  return (
    <div className="min-h-screen">
      <SimplePublicHeader />
      <WhatsAppButton />

      <main className="mx-auto w-full max-w-[920px] px-4 py-10 sm:px-6 sm:py-14">
        <h1 className="font-display text-3xl font-extrabold uppercase tracking-tight text-text-primary sm:text-4xl">
          Hesaplayıcılar
        </h1>
        <p className="mt-3 max-w-2xl text-sm leading-relaxed text-text-secondary">
          Günlük kalori ihtiyacını, makro dağılımını ve vücut kitle indeksini tahmin et. Yazdıkların
          tarayıcından çıkmaz, hiçbir yere kaydedilmez.
        </p>

        <section className="mt-8 rounded-2xl border border-border bg-surface p-6">
          <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">Bilgilerin</h2>
          <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
            <Field label="Cinsiyet">
              <select value={sex} onChange={(event) => setSex(event.target.value as Sex)} className={inputClasses}>
                <option value="MALE">Erkek</option>
                <option value="FEMALE">Kadın</option>
              </select>
            </Field>
            <Field label="Yaş" error={errorOf(ageValue, age, BODY_LIMITS.age.min, BODY_LIMITS.age.max)}>
              <input value={age} onChange={(event) => setAge(event.target.value)} type="text" inputMode="numeric" autoComplete="off" placeholder="örn. 28" className={inputClasses} />
            </Field>
            <Field label="Boy (cm)" error={errorOf(heightValue, height, BODY_LIMITS.height.min, BODY_LIMITS.height.max)}>
              <input value={height} onChange={(event) => setHeight(event.target.value)} type="text" inputMode="decimal" autoComplete="off" placeholder="örn. 178" className={inputClasses} />
            </Field>
            <Field label="Kilo (kg)" error={errorOf(weightValue, weight, BODY_LIMITS.weight.min, BODY_LIMITS.weight.max)}>
              <input value={weight} onChange={(event) => setWeight(event.target.value)} type="text" inputMode="decimal" autoComplete="off" placeholder="örn. 78,5" className={inputClasses} />
            </Field>
          </div>
        </section>

        <section className="mt-6 rounded-2xl border border-border bg-surface p-6" aria-labelledby="kalori-baslik">
          <h2 id="kalori-baslik" className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
            Günlük Kalori İhtiyacı
          </h2>
          <p className="mb-4 text-xs leading-relaxed text-text-secondary">
            Dinlenme metabolizman Mifflin–St Jeor formülüyle hesaplanır, aktivite çarpanıyla günlük harcamana
            çevrilir.
          </p>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
            <Field label="Aktivite düzeyi">
              <select value={activityValue} onChange={(event) => setActivityValue(event.target.value)} className={inputClasses}>
                {ACTIVITY_LEVELS.map((level) => (
                  <option key={level.value} value={level.value}>
                    {level.label}
                  </option>
                ))}
              </select>
            </Field>
            <Field label="Hedefin">
              <select value={goalValue} onChange={(event) => setGoalValue(event.target.value)} className={inputClasses}>
                {GOALS.map((entry) => (
                  <option key={entry.value} value={entry.value}>
                    {entry.label}
                  </option>
                ))}
              </select>
            </Field>
          </div>

          {calories ? (
            <div className="mt-5">
              <dl className="grid grid-cols-1 gap-3 sm:grid-cols-3">
                <Result label="Dinlenme metabolizması" value={`${formatAmount(calories.bmr)} kcal`} />
                <Result label="Günlük harcama" value={`${formatAmount(calories.maintenance)} kcal`} />
                <Result label="Hedef için günlük kalori" value={`${formatAmount(calories.target)} kcal`} highlight />
              </dl>
              {isMinor ? (
                <p className="mt-3 text-xs leading-relaxed text-text-secondary">
                  18 yaşın altında bu formül yalnızca kaba bir fikir verir; büyüme çağında ihtiyaç değişir. Kalori
                  hedefi için bir doktora ya da diyetisyene danış.
                </p>
              ) : null}
              {calories.heldAtBmr ? (
                <p className="mt-3 text-xs leading-relaxed text-text-secondary">
                  Bu hedef, dinlenme metabolizmanın altına inecekti; kalori ona sabitlendi. Daha düşüğü bir uzman
                  gözetimi gerektirir.
                </p>
              ) : null}
            </div>
          ) : (
            <Hint />
          )}
        </section>

        <MacroSection calories={calories?.target ?? null} weightKg={body?.weightKg ?? null} />

        <section className="mt-6 rounded-2xl border border-border bg-surface p-6" aria-labelledby="vki-baslik">
          <h2 id="vki-baslik" className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
            Vücut Kitle İndeksi (VKİ)
          </h2>
          {bmi ? (
            <>
              <dl className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-3">
                <Result label="VKİ" value={formatAmount(Math.round(bmi.value * 10) / 10)} highlight />
                {isMinor ? (
                  <Result label="Sınıf" value="18 yaş altı için geçerli değil" />
                ) : (
                  <>
                    <Result label="Sınıf (WHO)" value={bmi.category} />
                    <Result
                      label="Boyuna göre normal aralık"
                      value={`${formatAmount(Math.round(bmi.healthyRangeKg[0] * 10) / 10)} – ${formatAmount(Math.round(bmi.healthyRangeKg[1] * 10) / 10)} kg`}
                    />
                  </>
                )}
              </dl>
              <p className="mt-3 text-xs leading-relaxed text-text-secondary">
                {isMinor
                  ? 'Sınıflandırma yetişkinler içindir; 18 yaşın altında VKİ, yaş ve cinsiyete göre persentil eğrileriyle yorumlanır. Bunu doktorun yapar.'
                  : 'VKİ yalnızca boy ve kiloya bakar; kas ile yağı ayıramaz. Antrenmanlı biriysen "fazla kilolu" çıkabilirsin; bel çevresi ve vücut yağ oranıyla birlikte değerlendir.'}
              </p>
            </>
          ) : (
            <Hint />
          )}
        </section>

        <section className="mt-10 rounded-2xl border border-primary/40 bg-primary/5 p-6 text-center">
          <h2 className="font-display text-xl font-extrabold uppercase text-text-primary">Rakamlar bir başlangıç</h2>
          <p className="mx-auto mt-2 max-w-xl text-sm leading-relaxed text-text-secondary">
            Bunlar herkese uyan formüller. Sana özel plan, haftalık takip ve ilerlemene göre ayar için koçluk
            paketlerine bak.
          </p>
          <Link to="/#paketler" className="mt-5 inline-block">
            <Button type="button">Koçluğa Başla</Button>
          </Link>
        </section>

        <p className="mt-8 text-[11px] leading-relaxed text-text-muted">
          Bu sayfadaki sonuçlar genel formüllere dayanan tahminlerdir; tıbbi tavsiye değildir. Sağlık sorunun,
          hamileliğin ya da kronik bir hastalığın varsa beslenme ve antrenman değişikliklerinden önce doktoruna
          danış.
        </p>
      </main>
    </div>
  )
}

function MacroSection({ calories, weightKg }: { calories: number | null; weightKg: number | null }) {
  const [carbs, setCarbs] = useState(String(MACRO_SPLITS[0].carbsPercent))
  const [fat, setFat] = useState(String(MACRO_SPLITS[0].fatPercent))
  const [protein, setProtein] = useState(String(MACRO_SPLITS[0].proteinPercent))

  const carbsValue = readWhole(carbs)
  const fatValue = readWhole(fat)
  const proteinValue = readWhole(protein)
  const percents = [carbsValue, fatValue, proteinValue]
  const valid = percents.every((value) => value !== null && value <= 100)
  const total = percents.reduce<number>((sum, value) => sum + (value ?? 0), 0)
  const complete = valid && total === 100

  const macros =
    complete && calories !== null ? macrosOf(calories, carbsValue as number, fatValue as number, proteinValue as number) : null

  const note =
    total === 100
      ? { text: 'Toplam %100', tone: 'text-success' }
      : total < 100
        ? { text: `Toplam %${total} — ${100 - total} puan eksik`, tone: 'text-text-muted' }
        : { text: `Toplam %${total} — ${total - 100} puan fazla`, tone: 'text-error' }

  const apply = (split: (typeof MACRO_SPLITS)[number]) => {
    setCarbs(String(split.carbsPercent))
    setFat(String(split.fatPercent))
    setProtein(String(split.proteinPercent))
  }

  return (
    <section className="mt-6 rounded-2xl border border-border bg-surface p-6" aria-labelledby="makro-baslik">
      <h2 id="makro-baslik" className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
        Makro Dağılımı
      </h2>
      <p className="mb-4 text-xs leading-relaxed text-text-secondary">
        Hedef kalorini karbonhidrat, yağ ve protein yüzdelerine böl (toplam 100 olmalı); gramajı hesaplayalım.
        Karbonhidrat ve protein gramı 4, yağ gramı 9 kcal sayılır.
      </p>

      <div className="mb-4 flex flex-wrap gap-2">
        {MACRO_SPLITS.map((split) => (
          <button
            key={split.value}
            type="button"
            onClick={() => apply(split)}
            className="rounded-full border border-border-strong bg-surface-elevated px-3.5 py-1.5 text-[11px] font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-primary/60 hover:text-text-primary"
          >
            {split.label} ({split.carbsPercent}/{split.fatPercent}/{split.proteinPercent})
          </button>
        ))}
      </div>

      <div className="grid grid-cols-3 gap-3">
        <Field label="Karbonhidrat %">
          <input value={carbs} onChange={(event) => setCarbs(event.target.value)} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
        </Field>
        <Field label="Yağ %">
          <input value={fat} onChange={(event) => setFat(event.target.value)} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
        </Field>
        <Field label="Protein %">
          <input value={protein} onChange={(event) => setProtein(event.target.value)} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
        </Field>
      </div>
      <p role="status" className={`mt-2 text-xs font-semibold ${note.tone}`}>
        {note.text}
      </p>

      {macros && calories !== null ? (
        <div className="mt-4 overflow-x-auto">
          <table className="w-full min-w-[320px] border-collapse text-sm">
            <thead>
              <tr className="border-b border-border text-[11px] font-semibold uppercase tracking-wider text-text-muted">
                <th className="py-2 text-left font-semibold" />
                <th className="py-2 text-right font-semibold">Yüzde</th>
                <th className="py-2 text-right font-semibold">Gram / gün</th>
                {weightKg !== null ? <th className="py-2 text-right font-semibold">g / kg</th> : null}
              </tr>
            </thead>
            <tbody>
              {[
                { label: 'Karbonhidrat', percent: carbsValue as number, grams: macros.carbsG },
                { label: 'Yağ', percent: fatValue as number, grams: macros.fatG },
                { label: 'Protein', percent: proteinValue as number, grams: macros.proteinG },
              ].map((row) => (
                <tr key={row.label} className="border-b border-border/60">
                  <th scope="row" className="py-2 text-left font-medium text-text-secondary">
                    {row.label}
                  </th>
                  <td className="py-2 text-right text-text-secondary">%{row.percent}</td>
                  <td className="py-2 text-right font-semibold text-text-primary">{formatAmount(row.grams)} g</td>
                  {weightKg !== null ? (
                    <td className="py-2 text-right text-text-secondary">{formatAmount(Math.round((row.grams / weightKg) * 10) / 10)}</td>
                  ) : null}
                </tr>
              ))}
              <tr>
                <th scope="row" className="py-2 text-left font-medium text-text-secondary">
                  Kalori
                </th>
                <td className="py-2 text-right text-text-secondary">%100</td>
                <td className="py-2 text-right font-semibold text-primary">{formatAmount(calories)} kcal</td>
                {weightKg !== null ? <td /> : null}
              </tr>
            </tbody>
          </table>
        </div>
      ) : calories === null ? (
        <p className="mt-4 text-xs text-text-muted">Makroları görmek için yukarıdaki bilgilerini gir.</p>
      ) : null}
    </section>
  )
}

function Result({ label, value, highlight = false }: { label: string; value: string; highlight?: boolean }) {
  return (
    <div className="rounded-xl border border-border bg-surface-elevated p-4">
      <dt className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">{label}</dt>
      <dd className={`mt-1 font-display text-xl font-bold ${highlight ? 'text-primary' : 'text-text-primary'}`}>{value}</dd>
    </div>
  )
}

function Hint() {
  return <p className="mt-4 text-xs text-text-muted">Sonucu görmek için yukarıdaki bilgilerini gir.</p>
}

/** The typed number when it is a number inside the range; `null` when empty; `'invalid'` otherwise. */
function readInRange(text: string, min: number, max: number): number | null | 'invalid' {
  const parsed = parseFigure(text)
  if (parsed === null || parsed === 'invalid') {
    return parsed
  }
  return parsed >= min && parsed <= max ? parsed : 'invalid'
}

function errorOf(value: number | null | 'invalid', text: string, min: number, max: number): string | undefined {
  return value === 'invalid' && text.trim() !== '' ? `${min} ile ${max} arasında olmalı` : undefined
}

function readWhole(text: string): number | null {
  return /^\d+$/.test(text.trim()) ? Number(text.trim()) : null
}
