import { useState } from 'react'

import { useSubmitLead } from '@/api/leadsApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { parseFigure } from '@/types/monthlyRecord'

const GOALS = ['Kilo vermek', 'Kas kazanmak', 'Forma girmek / sıkılaşmak', 'Performans / güç', 'Sağlıklı beslenmek', 'Diğer']
const LEVELS = ['Hiç spor yapmadım', 'Başlangıç (0–1 yıl)', 'Orta (1–3 yıl)', 'İleri (3+ yıl)']

/**
 * "Ücretsiz ön değerlendirme": for the visitor who is not ready to buy. A few details go to the coach, who
 * writes back. Nothing is a purchase and no account is created. `website` is a honeypot field people never see.
 */
export function FreeAssessment() {
  const submit = useSubmitLead()
  const [values, setValues] = useState({ fullName: '', phone: '', email: '', goal: GOALS[0], level: '', height: '', weight: '', note: '', website: '' })
  const [consent, setConsent] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const set = (key: keyof typeof values) => (event: React.ChangeEvent<HTMLInputElement | HTMLSelectElement | HTMLTextAreaElement>) =>
    setValues((current) => ({ ...current, [key]: event.target.value }))

  const send = (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    const height = parseFigure(values.height)
    const weight = parseFigure(values.weight)
    if (values.fullName.trim() === '' || values.phone.trim().length < 7) {
      setError('Adınızı ve ulaşılabilir bir telefon numarası yazın.')
      return
    }
    if (height === 'invalid' || (typeof height === 'number' && (height < 100 || height > 250))) {
      setError('Boy 100 ile 250 cm arasında olmalı.')
      return
    }
    if (weight === 'invalid' || (typeof weight === 'number' && (weight < 30 || weight > 300))) {
      setError('Kilo 30 ile 300 kg arasında olmalı.')
      return
    }
    if (!consent) {
      setError('Sizinle iletişime geçebilmemiz için onay kutusunu işaretleyin.')
      return
    }
    submit.mutate({
      fullName: values.fullName.trim(),
      phone: values.phone.trim(),
      email: values.email.trim(),
      goal: values.goal,
      level: values.level,
      heightCm: typeof height === 'number' ? Math.round(height) : null,
      weightKg: typeof weight === 'number' ? weight : null,
      note: values.note.trim(),
      consent,
      website: values.website,
    })
  }

  return (
    <section id="on-degerlendirme" className="scroll-mt-24 border-b border-border py-20 sm:py-24">
      <div className="mx-auto max-w-3xl px-4 sm:px-6">
        <span className="mb-3 flex items-center gap-3 text-[11px] font-bold uppercase tracking-[0.28em] text-primary">
          <span className="h-px w-8 bg-primary" aria-hidden />
          Karar vermeden önce
        </span>
        <h2 className="font-display text-[clamp(1.8rem,4vw,2.6rem)] font-extrabold uppercase leading-tight text-text-primary">
          Ücretsiz ön değerlendirme
        </h2>
        <p className="mt-3 max-w-2xl text-sm leading-relaxed text-text-secondary">
          Hedefini ve durumunu birkaç satırda yaz; koçun sana hangi yoldan gitmenin mantıklı olduğunu anlatmak için
          dönsün. Ödeme yok, taahhüt yok.
        </p>

        {submit.isSuccess ? (
          <p role="status" className="mt-8 rounded-2xl border border-success/40 bg-success/10 p-6 text-sm text-success">
            Başvurun alındı. En kısa sürede verdiğin telefon numarasından sana ulaşacağız.
          </p>
        ) : (
          <form onSubmit={send} noValidate className="mt-8 space-y-4 rounded-2xl border border-border bg-surface p-6">
            <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
              <Field label="Ad Soyad *">
                <input value={values.fullName} onChange={set('fullName')} type="text" autoComplete="name" className={inputClasses} />
              </Field>
              <Field label="Telefon *" hint="Seninle buradan iletişime geçeriz">
                <input value={values.phone} onChange={set('phone')} type="tel" autoComplete="tel" className={inputClasses} />
              </Field>
              <Field label="E-posta (isteğe bağlı)">
                <input value={values.email} onChange={set('email')} type="email" autoComplete="email" className={inputClasses} />
              </Field>
              <Field label="Hedefin *">
                <select value={values.goal} onChange={set('goal')} className={inputClasses}>
                  {GOALS.map((goal) => (
                    <option key={goal} value={goal}>{goal}</option>
                  ))}
                </select>
              </Field>
              <Field label="Spor geçmişin">
                <select value={values.level} onChange={set('level')} className={inputClasses}>
                  <option value="">Seçin</option>
                  {LEVELS.map((level) => (
                    <option key={level} value={level}>{level}</option>
                  ))}
                </select>
              </Field>
              <div className="grid grid-cols-2 gap-4">
                <Field label="Boy (cm)">
                  <input value={values.height} onChange={set('height')} type="text" inputMode="numeric" autoComplete="off" className={inputClasses} />
                </Field>
                <Field label="Kilo (kg)">
                  <input value={values.weight} onChange={set('weight')} type="text" inputMode="decimal" autoComplete="off" className={inputClasses} />
                </Field>
              </div>
            </div>
            <Field label="Eklemek istediğin">
              <textarea value={values.note} onChange={set('note')} maxLength={1000} className={textareaClasses} />
            </Field>

            {/* Honeypot: off-screen, not focusable, ignored by people and by assistive tech. */}
            <div aria-hidden className="absolute -left-[9999px] h-0 w-0 overflow-hidden">
              <label>
                Web sitesi
                <input value={values.website} onChange={set('website')} type="text" tabIndex={-1} autoComplete="off" />
              </label>
            </div>

            <label className="flex cursor-pointer items-start gap-2.5 text-xs leading-relaxed text-text-secondary">
              <input type="checkbox" checked={consent} onChange={(event) => setConsent(event.target.checked)} className="mt-0.5" />
              Verdiğim bilgilerin, benimle iletişime geçilmesi amacıyla işlenmesini kabul ediyorum.
            </label>

            {error || submit.isError ? (
              <p role="alert" className="text-xs text-error">
                {error ?? submit.error?.message}
              </p>
            ) : null}
            <Button type="submit" disabled={submit.isPending}>
              {submit.isPending ? 'Gönderiliyor…' : 'Ön Değerlendirme İste'}
            </Button>
          </form>
        )}
      </div>
    </section>
  )
}
