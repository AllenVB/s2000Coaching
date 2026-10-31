import { useState } from 'react'

import { Field, inputClasses } from '@/components/ui/FormField'
import { navyBodyFat } from '@/lib/bodyMetrics'
import { parseFigure } from '@/types/monthlyRecord'

type Reading = number | null

function read(text: string, min: number, max: number): Reading | 'invalid' {
  const parsed = parseFigure(text)
  if (parsed === null || parsed === 'invalid') {
    return parsed
  }
  return parsed >= min && parsed <= max ? parsed : 'invalid'
}

const num = (value: Reading | 'invalid'): Reading => (typeof value === 'number' ? value : null)

/**
 * Body fat (US Navy tape method), lean mass and shoulder-to-waist ratio from tape measurements.
 * Sex, height and weight come from the page's shared inputs; nothing leaves the browser.
 */
export function BodyCompositionCalculator({
  sex,
  heightCm,
  weightKg,
}: {
  sex: 'MALE' | 'FEMALE'
  heightCm: Reading
  weightKg: Reading
}) {
  const [neck, setNeck] = useState('')
  const [waist, setWaist] = useState('')
  const [hip, setHip] = useState('')
  const [shoulder, setShoulder] = useState('')

  const neckValue = read(neck, 20, 70)
  const waistValue = read(waist, 40, 200)
  const hipValue = read(hip, 50, 200)
  const shoulderValue = read(shoulder, 70, 220)
  const err = (value: Reading | 'invalid', text: string, range: string) =>
    value === 'invalid' && text.trim() !== '' ? `${range} cm arasında olmalı` : undefined

  const fat = navyBodyFat(sex, heightCm, num(neckValue), num(waistValue), num(hipValue))
  const lean = fat !== null && weightKg !== null ? weightKg * (1 - fat / 100) : null
  const ratio =
    num(shoulderValue) !== null && num(waistValue) !== null ? (num(shoulderValue) as number) / (num(waistValue) as number) : null

  return (
    <section className="mt-6 rounded-2xl border border-border bg-surface p-6" aria-labelledby="yag-baslik">
      <h2 id="yag-baslik" className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
        Yağ Oranı, Yağsız Kütle ve Omuz/Bel Oranı
      </h2>
      <p className="mb-4 text-xs leading-relaxed text-text-secondary">
        Yağ oranı mezura ölçülerinden ABD Donanması yöntemiyle <strong>tahmin</strong> edilir; DEXA gibi bir ölçüm
        değildir, birkaç puan sapabilir. Boy ve kiloyu yukarıdaki alanlara yaz. Ölçüleri sabah, aç karnına, gevşek
        dururken al.
      </p>
      <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
        <Field label="Boyun (cm)" error={err(neckValue, neck, '20–70')}>
          <input value={neck} onChange={(e) => setNeck(e.target.value)} type="text" inputMode="decimal" autoComplete="off" placeholder="örn. 38" className={inputClasses} />
        </Field>
        <Field label="Bel (cm)" hint="göbek deliği hizası" error={err(waistValue, waist, '40–200')}>
          <input value={waist} onChange={(e) => setWaist(e.target.value)} type="text" inputMode="decimal" autoComplete="off" placeholder="örn. 84" className={inputClasses} />
        </Field>
        {sex === 'FEMALE' ? (
          <Field label="Kalça (cm)" error={err(hipValue, hip, '50–200')}>
            <input value={hip} onChange={(e) => setHip(e.target.value)} type="text" inputMode="decimal" autoComplete="off" placeholder="örn. 98" className={inputClasses} />
          </Field>
        ) : null}
        <Field label="Omuz çevresi (cm)" hint="omuzların en geniş yeri" error={err(shoulderValue, shoulder, '70–220')}>
          <input value={shoulder} onChange={(e) => setShoulder(e.target.value)} type="text" inputMode="decimal" autoComplete="off" placeholder="örn. 118" className={inputClasses} />
        </Field>
      </div>

      <dl className="mt-5 grid grid-cols-2 gap-3 sm:grid-cols-4" aria-live="polite">
        <Out label="Yağ oranı (tahmin)" value={fat !== null ? `%${fat.toFixed(1)}` : '—'} />
        <Out label="Yağsız kütle oranı" value={fat !== null ? `%${(100 - fat).toFixed(1)}` : '—'} />
        <Out label="Yağsız kütle" value={lean !== null ? `${lean.toFixed(1)} kg` : '—'} />
        <Out label="Omuz / bel oranı" value={ratio !== null ? ratio.toFixed(2) : '—'} />
      </dl>
      {ratio !== null ? (
        <p className="mt-3 text-xs text-text-muted">
          Omuz/bel oranı için sık kullanılan hedef 1,4–1,6 aralığıdır (1,618 "altın oran" olarak anılır).
        </p>
      ) : null}
      {fat === null ? (
        <p className="mt-3 text-xs text-text-muted">
          Yağ oranı için boy, boyun ve bel (kadınlarda kalça da) gerekir; kilo girersen yağsız kütle de hesaplanır.
        </p>
      ) : null}
    </section>
  )
}

function Out({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-xl border border-border bg-surface-elevated p-4">
      <dt className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">{label}</dt>
      <dd className="mt-1 font-display text-xl font-bold text-text-primary">{value}</dd>
    </div>
  )
}
