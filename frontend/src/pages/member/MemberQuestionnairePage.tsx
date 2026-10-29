import { useState } from 'react'

import { useMyQuestionnaire, useSaveMyQuestionnaire } from '@/api/questionnaireApi'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { formatDate } from '@/lib/date'
import {
  EQUIPMENT,
  GOALS,
  type QuestionnaireGoal,
  type TrainingEquipment,
} from '@/types/questionnaire'

/** The starting questionnaire: health readiness (PAR-Q), goal, availability and equipment. */
export function MemberQuestionnairePage() {
  const { member } = useMemberProfileSync()
  const { data } = useMyQuestionnaire()
  const save = useSaveMyQuestionnaire()

  const [answers, setAnswers] = useState<(boolean | null)[] | null>(null)
  const [goal, setGoal] = useState<QuestionnaireGoal>('KAS_KAZANMA')
  const [days, setDays] = useState('4')
  const [equipment, setEquipment] = useState<TrainingEquipment>('SALON')
  const [injuries, setInjuries] = useState('')

  if (!member || !data) {
    return null
  }

  if (!member.healthDataConsentAt) {
    return (
      <>
        <PageHeading title="Başlangıç Anketi" description="Anketi doldurmak için önce onay vermeniz gerekiyor." />
        <HealthDataConsent />
      </>
    )
  }

  // Seeded from the saved answers once, so editing starts from what was said before.
  const current = answers ?? data.answers?.parq ?? data.questions.map(() => null)
  const complete = current.every((value) => value !== null)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (!complete) {
      return
    }
    save.mutate({
      parq: current as boolean[],
      goal,
      trainingDaysPerWeek: Number(days),
      equipment,
      injuries: injuries.trim() || null,
    })
  }

  return (
    <>
      <PageHeading
        title="Başlangıç Anketi"
        description="Koçunuzun size uygun ve güvenli bir program yazabilmesi için. Yanıtlarınızı yalnızca siz ve koçunuz görür."
      />

      {data.answers ? (
        <p className="mb-4 rounded-xl border border-border bg-surface p-3.5 text-xs text-text-secondary">
          Anketinizi {formatDate(data.answers.completedAt)} tarihinde doldurdunuz. Değiştirip tekrar
          gönderebilirsiniz.
          {data.answers.needsMedicalClearance
            ? ' Bazı sorulara "evet" yanıtı verdiğiniz için programa başlamadan önce doktorunuza danışmanız önerilir.'
            : ''}
        </p>
      ) : null}

      <form className="space-y-6 rounded-2xl border border-border bg-surface p-6" onSubmit={submit}>
        <fieldset className="space-y-4">
          <legend className="mb-1 text-sm font-bold uppercase tracking-wider text-text-primary">
            Sağlık Soruları
          </legend>
          {data.questions.map((question, index) => (
            <div key={question} className="rounded-xl border border-border bg-surface-elevated p-4">
              <p className="mb-3 text-sm leading-relaxed text-text-primary">
                {index + 1}. {question}
              </p>
              <div className="flex gap-2">
                {[
                  { label: 'Hayır', value: false },
                  { label: 'Evet', value: true },
                ].map((option) => (
                  <label
                    key={option.label}
                    className={`flex h-10 flex-1 cursor-pointer items-center justify-center rounded-lg border text-sm font-semibold ${
                      current[index] === option.value
                        ? 'border-primary bg-primary/10 text-text-primary'
                        : 'border-border text-text-secondary'
                    }`}
                  >
                    <input
                      type="radio"
                      name={`parq-${index}`}
                      checked={current[index] === option.value}
                      onChange={() => {
                        const next = [...current]
                        next[index] = option.value
                        setAnswers(next)
                      }}
                      className="sr-only"
                    />
                    {option.label}
                  </label>
                ))}
              </div>
            </div>
          ))}
        </fieldset>

        <div className="grid grid-cols-1 gap-4 sm:grid-cols-3">
          <Field label="Hedefiniz">
            <select value={goal} onChange={(e) => setGoal(e.target.value as QuestionnaireGoal)} className={inputClasses}>
              {GOALS.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </Field>
          <Field label="Haftada kaç gün?">
            <select value={days} onChange={(e) => setDays(e.target.value)} className={inputClasses}>
              {[1, 2, 3, 4, 5, 6, 7].map((n) => (
                <option key={n} value={n}>{n} gün</option>
              ))}
            </select>
          </Field>
          <Field label="Nerede antrenman yapacaksınız?">
            <select
              value={equipment}
              onChange={(e) => setEquipment(e.target.value as TrainingEquipment)}
              className={inputClasses}
            >
              {EQUIPMENT.map((option) => (
                <option key={option.value} value={option.value}>{option.label}</option>
              ))}
            </select>
          </Field>
        </div>

        <Field label="Sakatlık / ağrı / bilmemiz gerekenler">
          <textarea
            value={injuries}
            onChange={(event) => setInjuries(event.target.value)}
            placeholder="Varsa yazın: eski sakatlıklar, ameliyatlar, düzenli ağrılar…"
            className={textareaClasses}
          />
        </Field>

        {save.isError ? <p className="text-xs text-error">{save.error.message}</p> : null}
        {save.isSuccess ? <p className="text-xs text-success">Anketiniz kaydedildi.</p> : null}

        <Button type="submit" disabled={!complete || save.isPending}>
          {save.isPending ? 'Kaydediliyor…' : 'Anketi Kaydet'}
        </Button>
        {!complete ? <p className="text-[11px] text-text-muted">Göndermek için tüm soruları yanıtlayın.</p> : null}
      </form>
    </>
  )
}
