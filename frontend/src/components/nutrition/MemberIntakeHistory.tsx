import { useMemberIntake } from '@/api/intakeApi'
import { formatDate } from '@/lib/date'
import { targetsOf, type NutritionPlan } from '@/types/nutrition'

/** The coach's read of a member's food log. Read-only: only the member writes their own days. */
export function MemberIntakeHistory({ memberId, plan }: { memberId: string; plan: NutritionPlan | null }) {
  const { data: entries, isLoading, isError, error } = useMemberIntake(memberId)

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-4 text-sm font-bold uppercase tracking-wider text-text-primary">Yemek Kayıtları</h2>
      {isLoading ? (
        <div className="h-16 animate-pulse rounded-xl bg-surface-elevated" />
      ) : isError ? (
        <p className="text-xs text-error">Yüklenemedi: {error.message}</p>
      ) : !entries || entries.length === 0 ? (
        <p className="text-sm italic text-text-muted">Üye henüz yemek kaydı girmedi.</p>
      ) : (
        <>
          <p className="mb-3 text-xs text-text-muted">
            Son {Math.min(entries.length, 14)} gün
            {plan?.targetCalories ? ` · antrenman günü hedefi ${plan.targetCalories} kcal` : ''}
            {plan?.restTargetCalories ? ` · dinlenme günü hedefi ${plan.restTargetCalories} kcal` : ''}
          </p>
          <ul className="space-y-1.5">
            {entries.slice(0, 14).map((entry) => {
              // The target follows the day type the member chose; a day with no type has no target to compare to.
              const target = plan && entry.dayType ? targetsOf(plan, entry.dayType === 'DINLENME' ? 'REST' : 'TRAINING').calories : null
              // What was eaten counts by the macros when they are there (4/4/9 kcal per gram), else the typed total.
              const macros = (entry.proteinG ?? 0) * 4 + (entry.carbsG ?? 0) * 4 + (entry.fatG ?? 0) * 9
              const eaten = macros > 0 ? macros : entry.calories
              const remaining = target !== null ? target - eaten : null
              return (
              <li
                key={entry.id}
                className="flex flex-wrap items-baseline justify-between gap-3 rounded-lg border border-border bg-surface-elevated px-3 py-2 text-xs"
              >
                <span className="font-semibold text-text-primary">{formatDate(entry.consumedOn)}</span>
                <span className="text-text-muted">
                  {entry.proteinG !== null ? `P ${entry.proteinG} ` : ''}
                  {entry.carbsG !== null ? `K ${entry.carbsG} ` : ''}
                  {entry.fatG !== null ? `Y ${entry.fatG} ` : ''}
                  {entry.sodiumMg !== null ? `Na ${entry.sodiumMg}` : ''}
                </span>
                <span className="font-semibold text-text-primary">
                  {entry.dayType ? `${entry.dayType === 'DINLENME' ? 'Dinlenme' : 'Antrenman'} · ` : ''}
                  {entry.calories} kcal
                  {remaining !== null ? (
                    <span className={`ml-2 font-normal ${remaining >= 0 ? 'text-success' : 'text-warning'}`}>
                      {remaining >= 0 ? `hedefe ${remaining} kcal kaldı` : `hedefi ${-remaining} kcal aştı`}
                    </span>
                  ) : null}
                </span>
              </li>
              )
            })}
          </ul>
        </>
      )}
    </section>
  )
}
