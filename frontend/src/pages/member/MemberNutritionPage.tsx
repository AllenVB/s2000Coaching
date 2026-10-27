import { useMyNutritionPlan } from '@/api/nutritionApi'
import { HealthDataConsent } from '@/components/member/HealthDataConsent'
import { IntakeLog } from '@/components/member/IntakeLog'
import { MacroSummary } from '@/components/nutrition/MacroSummary'
import { useMemberProfileSync } from '@/hooks/useMemberProfileSync'
import { ComingSoonCard, PageHeading } from '@/components/ui/PageHeading'
import { mealCalories } from '@/types/nutrition'

export function MemberNutritionPage() {
  const { data: plan, isLoading, isError, error } = useMyNutritionPlan()
  const { member } = useMemberProfileSync()
  // Food intake is health data like measurements: nothing is shown or written before consent.
  const intakeLog = member?.healthDataConsentAt ? <IntakeLog plan={plan ?? null} /> : <HealthDataConsent />

  if (isLoading) {
    return (
      <>
        <PageHeading title="Beslenme" />
        <div className="h-40 animate-pulse rounded-2xl border border-border bg-surface" />
      </>
    )
  }

  if (isError) {
    return (
      <>
        <PageHeading title="Beslenme" />
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Planınız yüklenemedi: {error.message}
        </p>
      </>
    )
  }

  if (!plan) {
    return (
      <>
        <PageHeading title="Beslenme" />
        <ComingSoonCard
          title="Beslenme Planınız Hazırlanıyor"
          description="Koçunuz planınızı yüklediğinde makro hedefleriniz ve öğünleriniz burada görünecek."
        />
        <div className="mt-6">{intakeLog}</div>
      </>
    )
  }

  return (
    <>
      <PageHeading title={plan.name} description={plan.note ?? 'Günlük hedefleriniz ve öğünleriniz.'} />

      <div className="space-y-6">
        {intakeLog}

        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">
            Günlük Hedefler
          </h2>
          <MacroSummary plan={plan} />
        </section>

        {plan.meals.map((meal) => (
          <section key={meal.id} className="rounded-2xl border border-border bg-surface p-6">
            <div className="mb-4 flex flex-wrap items-baseline justify-between gap-2 border-b border-border pb-3">
              <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">{meal.name}</h2>
              <span className="text-xs text-text-muted">
                {meal.timeHint ? `${meal.timeHint} · ` : ''}
                {mealCalories(meal)} kcal
              </span>
            </div>

            {meal.note ? (
              <p className="mb-3 text-xs leading-relaxed text-text-secondary">{meal.note}</p>
            ) : null}

            {meal.items.length === 0 ? (
              <p className="text-sm italic text-text-muted">Bu öğüne henüz besin eklenmedi.</p>
            ) : (
              <ul className="space-y-2">
                {meal.items.map((item) => (
                  <li
                    key={item.id}
                    className="flex flex-wrap items-baseline justify-between gap-3 rounded-xl border border-border bg-surface-elevated px-4 py-3"
                  >
                    <span className="min-w-0">
                      <span className="block font-semibold text-text-primary">{item.name}</span>
                      {item.note ? (
                        <span className="block text-xs text-text-secondary">{item.note}</span>
                      ) : null}
                    </span>
                    <span className="text-right text-xs text-text-muted">
                      {item.amount ? <span className="block text-text-secondary">{item.amount}</span> : null}
                      {item.calories !== null ? <span className="block">{item.calories} kcal</span> : null}
                    </span>
                  </li>
                ))}
              </ul>
            )}
          </section>
        ))}

        {plan.meals.length === 0 ? (
          <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
            Planınıza henüz öğün eklenmedi.
          </p>
        ) : null}
      </div>
    </>
  )
}
