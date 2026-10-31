import { Link } from 'react-router-dom'

import { useMyCheckIns } from '@/api/checkInApi'
import { useMemberRecordSource } from '@/api/monthlyRecordApi'
import { useMyNutritionPlan } from '@/api/nutritionApi'
import { useMyQuestionnaire } from '@/api/questionnaireApi'
import { useMyTrainingPlan } from '@/api/trainingApi'
import { CheckIcon } from '@/components/ui/icons'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'

/**
 * "İlk adımlar": what a new member does first, in order, with what is done ticked off. It is shown
 * until every step is done and then disappears for good, so it is only ever in the way of someone who
 * has not finished setting up. Each step is worked out from data the member already has; nothing extra
 * is stored.
 */
export function FirstSteps() {
  const { member } = useMemberProfileSync()
  const { data: questionnaire } = useMyQuestionnaire()
  const records = useMemberRecordSource()
  const { data: checkIns } = useMyCheckIns()
  const { data: training } = useMyTrainingPlan()
  const { data: nutrition } = useMyNutritionPlan()

  // Wait for everything: a step shown as open for the moment it takes data to arrive would flash.
  if (!member || !questionnaire || !records.records || !checkIns || training === undefined || nutrition === undefined) {
    return null
  }

  const consented = Boolean(member.healthDataConsentAt)
  const steps = [
    {
      label: 'Sağlık verisi onayını verin',
      hint: 'Ölçü, kilo ve fotoğraf girebilmek için gerekir.',
      done: consented,
      to: '/uye/olcumlerim',
    },
    {
      label: 'Başlangıç anketini doldurun',
      hint: 'Koçunuz size güvenli ve uygun bir program yazsın.',
      done: Boolean(questionnaire.answers),
      to: '/uye/anket',
    },
    {
      label: 'İlk aylık ölçü tablonuzu girin',
      hint: 'Boy, kilo ve çevre ölçüleriniz başlangıç noktanız olur.',
      done: records.records.length > 0,
      to: '/uye/olcumlerim?sekme=aylik',
    },
    {
      label: 'İlk haftalık takibinizi gönderin',
      hint: 'Kilonuz ve haftanın nasıl geçtiği.',
      done: checkIns.length > 0,
      to: '/uye/olcumlerim?sekme=haftalik',
    },
    {
      label: 'Programınızı inceleyin',
      hint: training || nutrition ? 'Koçunuz programınızı hazırladı.' : 'Koçunuz hazırladığında burada görünecek.',
      done: false,
      waiting: !training && !nutrition,
      to: training ? '/uye/programlarim?sekme=antrenman' : '/uye/programlarim?sekme=beslenme',
    },
  ]
  // The last step cannot be "done" from data (reading is not recorded); it counts once the plan exists.
  const finished = steps.slice(0, 4).every((step) => step.done) && Boolean(training || nutrition)
  if (finished) {
    return null
  }

  const doneCount = steps.slice(0, 4).filter((step) => step.done).length

  return (
    <section className="mb-6 rounded-2xl border border-primary/30 bg-primary/5 p-6">
      <div className="flex items-baseline justify-between gap-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">İlk Adımlar</h2>
        <span className="text-xs text-text-muted">{doneCount} / 4 tamamlandı</span>
      </div>
      <ol className="mt-4 space-y-3">
        {steps.map((step, index) => (
          <li key={step.label}>
            <Link to={step.to} className="flex items-start gap-3 rounded-xl p-2 transition-colors hover:bg-white/5">
              <span
                className={`mt-0.5 flex h-6 w-6 shrink-0 items-center justify-center rounded-full border text-xs font-bold ${
                  step.done ? 'border-primary bg-primary text-canvas' : 'border-border-strong text-text-muted'
                }`}
                aria-hidden="true"
              >
                {step.done ? <CheckIcon className="h-3.5 w-3.5" /> : index + 1}
              </span>
              <span className="min-w-0">
                <span className={`block text-sm font-semibold ${step.done ? 'text-text-muted line-through' : 'text-text-primary'}`}>
                  {step.label}
                  {step.done ? <span className="sr-only"> (tamamlandı)</span> : null}
                </span>
                <span className="block text-xs text-text-secondary">{step.hint}</span>
              </span>
            </Link>
          </li>
        ))}
      </ol>
    </section>
  )
}
