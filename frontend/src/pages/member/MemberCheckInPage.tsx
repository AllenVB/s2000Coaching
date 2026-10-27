import { useState } from 'react'

import { useMyCheckIns, useSaveMyCheckIn } from '@/api/checkInApi'
import { CheckInSummary } from '@/components/member/CheckInSummary'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { formatDate } from '@/lib/date'
import { CHECK_IN_SCALES } from '@/types/checkIn'

const today = () => new Date().toISOString().slice(0, 10)

/** The weekly ritual: how the week went, in a minute; the coach answers on the same card. */
export function MemberCheckInPage() {
  const { member } = useMemberProfileSync()
  const { data: checkIns } = useMyCheckIns()
  const save = useSaveMyCheckIn()

  const [values, setValues] = useState<Record<string, number>>({ sleep: 3, stress: 3, energy: 3, hunger: 3 })
  const [diet, setDiet] = useState('')
  const [note, setNote] = useState('')

  if (!member) {
    return null
  }

  if (!member.healthDataConsentAt) {
    return (
      <>
        <PageHeading
          title="Haftalık Değerlendirme"
          description="Değerlendirme girmeye başlamak için önce onay vermeniz gerekiyor."
        />
        <HealthDataConsent />
      </>
    )
  }

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    save.mutate(
      {
        weekOf: today(),
        sleep: values.sleep,
        stress: values.stress,
        energy: values.energy,
        hunger: values.hunger,
        dietAdherencePercent: diet.trim() === '' ? null : Number(diet),
        note: note.trim() || null,
      },
      { onSuccess: () => setNote('') },
    )
  }

  return (
    <>
      <PageHeading
        title="Haftalık Değerlendirme"
        description="Haftanın nasıl geçtiğini bir dakikada anlatın. Aynı haftayı tekrar gönderirseniz güncellenir."
      />

      <div className="space-y-6">
        <form className="space-y-5 rounded-2xl border border-border bg-surface p-6" onSubmit={submit}>
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

          <Field label="Beslenmeye uyum (%)" hint="Planı ne kadar uyguladınız? Boş bırakabilirsiniz.">
            <input
              type="number"
              inputMode="numeric"
              min={0}
              max={100}
              value={diet}
              onChange={(event) => setDiet(event.target.value)}
              className={`${inputClasses} max-w-[160px]`}
            />
          </Field>

          <Field label="Not">
            <textarea
              value={note}
              onChange={(event) => setNote(event.target.value)}
              placeholder="Koçunuza söylemek istediğiniz bir şey: ağrı, zorlandığınız yer, sorular…"
              className={textareaClasses}
            />
          </Field>

          {save.isError ? <p className="text-xs text-error">{save.error.message}</p> : null}
          {save.isSuccess ? <p className="text-xs text-success">Değerlendirmeniz koçunuza iletildi.</p> : null}

          <Button type="submit" disabled={save.isPending}>
            {save.isPending ? 'Gönderiliyor…' : 'Bu Haftayı Gönder'}
          </Button>
        </form>

        {checkIns && checkIns.length > 0 ? (
          <section className="space-y-3">
            <h2 className="text-[11px] font-semibold uppercase tracking-wider text-text-muted">Geçmiş Haftalar</h2>
            {checkIns.map((checkIn) => (
              <article key={checkIn.id} className="rounded-2xl border border-border bg-surface p-5">
                <p className="mb-2 text-sm font-semibold text-text-primary">
                  {formatDate(checkIn.weekStart)} haftası
                </p>
                <CheckInSummary checkIn={checkIn} />
                {checkIn.note ? (
                  <p className="mt-3 text-xs leading-relaxed text-text-secondary">{checkIn.note}</p>
                ) : null}
                {checkIn.coachReply ? (
                  <div className="mt-3 rounded-xl border border-primary/30 bg-primary/5 p-3.5">
                    <p className="mb-1 text-[11px] font-semibold uppercase tracking-wider text-primary">
                      Koçunuzun yanıtı
                    </p>
                    <p className="whitespace-pre-line text-sm leading-relaxed text-text-primary">
                      {checkIn.coachReply}
                    </p>
                  </div>
                ) : (
                  <p className="mt-3 text-[11px] text-text-muted">Koçunuz henüz yanıtlamadı.</p>
                )}
              </article>
            ))}
          </section>
        ) : null}
      </div>
    </>
  )
}
