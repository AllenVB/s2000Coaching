import { useState } from 'react'

import { useMyCheckIns, useSaveMyCheckIn } from '@/api/checkInApi'
import { useSaveMyMeasurement } from '@/api/measurementsApi'
import { CheckInSummary } from '@/components/member/CheckInSummary'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { formatDate, todayIso } from '@/lib/date'
import { CHECK_IN_SCALES } from '@/types/checkIn'
import { parseFigure } from '@/types/monthlyRecord'

/**
 * The weekly ritual in one form: this week's weight, how the week went (sleep, stress, energy,
 * hunger, how well the diet was followed) and what the member wants or is unhappy about. The weight
 * is stored as a measurement and the rest as the week's evaluation, so the chart, the history and the
 * coach's cards all keep working; the coach answers on the same card below.
 */
export function WeeklyCheckForm() {
  const { data: checkIns } = useMyCheckIns()
  const saveCheckIn = useSaveMyCheckIn()
  const saveMeasurement = useSaveMyMeasurement()

  const [weight, setWeight] = useState('')
  const [values, setValues] = useState<Record<string, number>>({ sleep: 3, stress: 3, energy: 3, hunger: 3 })
  const [diet, setDiet] = useState('')
  const [wishes, setWishes] = useState('')
  const [error, setError] = useState<string | null>(null)
  const [sent, setSent] = useState(false)
  const [isSending, setIsSending] = useState(false)

  const submit = async (event: React.FormEvent) => {
    event.preventDefault()
    setError(null)
    setSent(false)

    const weightValue = parseFigure(weight)
    if (weightValue === null || weightValue === 'invalid' || weightValue < 30 || weightValue > 300) {
      setError('Bu haftaki kilonuzu 30 ile 300 kg arasında girin.')
      return
    }
    const dietValue = diet.trim() === '' ? null : Number(diet)
    if (dietValue !== null && !(dietValue >= 0 && dietValue <= 100)) {
      setError('Beslenmeye uyum 0 ile 100 arasında olmalı.')
      return
    }

    setIsSending(true)
    try {
      await saveMeasurement.mutateAsync({ measuredOn: todayIso(), weightKg: weightValue })
      await saveCheckIn.mutateAsync({
        weekOf: todayIso(),
        sleep: values.sleep,
        stress: values.stress,
        energy: values.energy,
        hunger: values.hunger,
        dietAdherencePercent: dietValue,
        note: wishes.trim() || null,
      })
      setSent(true)
      setWeight('')
      setWishes('')
    } catch (failure) {
      setError(failure instanceof Error ? failure.message : 'Gönderilemedi.')
    } finally {
      setIsSending(false)
    }
  }

  return (
    <div className="space-y-6">
      <form className="space-y-5 rounded-2xl border border-border bg-surface p-6" onSubmit={submit} noValidate>
        <div>
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Bu Haftanın Takibi</h2>
          <p className="mt-1.5 text-xs leading-relaxed text-text-secondary">
            Haftada bir, tercihen aynı gün ve aynı saatte tartının. Aynı haftayı tekrar gönderirseniz güncellenir.
          </p>
        </div>

        <Field label="Kilo (kg) *">
          <input
            value={weight}
            onChange={(event) => setWeight(event.target.value)}
            type="text"
            inputMode="decimal"
            autoComplete="off"
            placeholder="örn. 78,4"
            className={`${inputClasses} max-w-[160px]`}
          />
        </Field>

        {CHECK_IN_SCALES.map((scale) => (
          <fieldset key={scale.key}>
            <legend className="mb-2 flex w-full items-baseline justify-between text-xs font-medium text-text-secondary">
              <span>{scale.label}</span>
              <span className="text-text-muted">
                1 = {scale.low} · 5 = {scale.high}
              </span>
            </legend>
            <div className="flex gap-2">
              {[1, 2, 3, 4, 5].map((n) => (
                <label
                  key={n}
                  className={`flex h-11 flex-1 cursor-pointer items-center justify-center rounded-lg border text-sm font-semibold ${
                    values[scale.key] === n
                      ? 'border-primary bg-primary/10 text-text-primary'
                      : 'border-border bg-surface-elevated text-text-secondary'
                  }`}
                >
                  <input
                    type="radio"
                    name={scale.key}
                    value={n}
                    checked={values[scale.key] === n}
                    onChange={() => setValues({ ...values, [scale.key]: n })}
                    className="sr-only"
                  />
                  {n}
                </label>
              ))}
            </div>
          </fieldset>
        ))}

        <Field label="Beslenmeye uyum (%)" hint="Programı ne kadar uyguladınız? Boş bırakabilirsiniz.">
          <input
            type="text"
            inputMode="numeric"
            value={diet}
            onChange={(event) => setDiet(event.target.value)}
            className={`${inputClasses} max-w-[160px]`}
          />
        </Field>

        <Field label="İstek ve şikayetleriniz" hint="Koçunuzun bilmesini istediğiniz her şey: ağrı, zorlandığınız yer, programda değişmesini istedikleriniz, sorular…">
          <textarea
            value={wishes}
            onChange={(event) => setWishes(event.target.value)}
            maxLength={3000}
            className={textareaClasses}
          />
        </Field>

        {error ? (
          <p role="alert" className="text-xs text-error">
            {error}
          </p>
        ) : null}
        {sent ? <p className="text-xs text-success">Bu haftanın takibi koçunuza iletildi.</p> : null}

        <Button type="submit" disabled={isSending}>
          {isSending ? 'Gönderiliyor…' : 'Bu Haftayı Gönder'}
        </Button>
      </form>

      {checkIns && checkIns.length > 0 ? (
        <section className="space-y-3">
          <h2 className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">Geçmiş Haftalar</h2>
          {checkIns.map((checkIn) => (
            <article key={checkIn.id} className="rounded-2xl border border-border bg-surface p-5">
              <p className="mb-2 text-sm font-semibold text-text-primary">{formatDate(checkIn.weekStart)} haftası</p>
              <CheckInSummary checkIn={checkIn} />
              {checkIn.note ? (
                <p className="mt-3 text-xs leading-relaxed text-text-secondary">
                  <span className="font-semibold text-text-muted">İstek / şikayet: </span>
                  {checkIn.note}
                </p>
              ) : null}
              {checkIn.coachReply ? (
                <div className="mt-3 rounded-xl border border-primary/30 bg-primary/5 p-3.5">
                  <p className="mb-1 text-[11px] font-semibold uppercase tracking-wider text-primary">Koçunuzun yanıtı</p>
                  <p className="whitespace-pre-line text-sm leading-relaxed text-text-primary">{checkIn.coachReply}</p>
                </div>
              ) : (
                <p className="mt-3 text-[11px] text-text-muted">Koçunuz henüz yanıtlamadı.</p>
              )}
            </article>
          ))}
        </section>
      ) : null}
    </div>
  )
}
