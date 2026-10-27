import { useState } from 'react'

import {
  useActivateNutritionPlan,
  useCreateMemberNutritionPlan,
  useDeleteNutritionPlan,
  useMemberNutritionPlans,
  useNutritionTemplates,
} from '@/api/nutritionApi'
import { MemberIntakeHistory } from '@/components/nutrition/MemberIntakeHistory'
import { MacroSummary } from '@/components/nutrition/MacroSummary'
import { NutritionPlanEditor } from '@/components/nutrition/NutritionPlanEditor'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { inputClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'
import { useMemberDetail } from '@/pages/admin/MemberDetailPage'

export function MemberNutritionTab() {
  const { member } = useMemberDetail()
  const { data: plans, isLoading, isError, error } = useMemberNutritionPlans(member.id)
  const { data: templates } = useNutritionTemplates()

  const createPlan = useCreateMemberNutritionPlan(member.id)
  const activatePlan = useActivateNutritionPlan()
  const deletePlan = useDeleteNutritionPlan()

  const [name, setName] = useState('')
  const [templateId, setTemplateId] = useState('')
  const [openId, setOpenId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = name.trim()
    if (trimmed.length === 0) {
      return
    }
    createPlan.mutate(
      { input: { name: trimmed }, templateId: templateId || undefined },
      {
        onSuccess: (plan) => {
          setName('')
          setTemplateId('')
          setOpenId(plan.id)
        },
      },
    )
  }

  const deletingPlan = plans?.find((plan) => plan.id === deletingId)

  if (isLoading) {
    return <div className="h-32 animate-pulse rounded-2xl border border-border bg-surface" />
  }

  if (isError) {
    return (
      <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
        Beslenme planları yüklenemedi: {error.message}
      </p>
    )
  }

  return (
    <div className="space-y-6">
      <section className="rounded-2xl border border-border bg-surface p-6">
        <h2 className="mb-2 text-sm font-bold uppercase tracking-wider text-text-primary">
          Beslenme Planı Ata
        </h2>
        <p className="mb-4 text-xs leading-relaxed text-text-secondary">
          Boş bir plan oluşturun ya da bir şablonu kopyalayın. Yeni plan otomatik olarak aktif olur.
        </p>
        <form className="flex flex-wrap items-end gap-3" onSubmit={submit}>
          <label className="min-w-[200px] flex-1">
            <span className="mb-1.5 block text-xs font-medium text-text-secondary">Plan Adı</span>
            <input
              value={name}
              onChange={(event) => setName(event.target.value)}
              type="text"
              placeholder="Örn: Ekim Beslenmesi"
              className={inputClasses}
            />
          </label>
          <label className="min-w-[200px] flex-1">
            <span className="mb-1.5 block text-xs font-medium text-text-secondary">Şablondan Kopyala</span>
            <select
              value={templateId}
              onChange={(event) => setTemplateId(event.target.value)}
              className={inputClasses}
            >
              <option value="">Boş plan</option>
              {templates?.map((template) => (
                <option key={template.id} value={template.id}>
                  {template.name} ({template.meals.length} öğün)
                </option>
              ))}
            </select>
          </label>
          <Button type="submit" disabled={createPlan.isPending || name.trim().length === 0}>
            {createPlan.isPending ? 'Oluşturuluyor…' : 'Planı Oluştur'}
          </Button>
        </form>
        {createPlan.isError ? <p className="mt-3 text-xs text-error">{createPlan.error.message}</p> : null}
      </section>

      {plans && plans.length > 0 ? (
        plans.map((plan) => (
          <section key={plan.id} className="rounded-2xl border border-border bg-surface p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <div className="flex flex-wrap items-center gap-2.5">
                  <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">{plan.name}</h2>
                  {plan.active ? (
                    <span className="inline-flex items-center rounded-full border border-success/40 bg-success/10 px-2.5 py-1 text-[11px] font-semibold uppercase tracking-wide text-success">
                      Aktif
                    </span>
                  ) : null}
                </div>
                <p className="mt-1 text-xs text-text-muted">
                  {plan.meals.length} öğün ·{' '}
                  {plan.meals.reduce((total, meal) => total + meal.items.length, 0)} besin · güncelleme:{' '}
                  {formatDate(plan.updatedAt)}
                </p>
              </div>
              <div className="flex items-center gap-2">
                {!plan.active ? (
                  <Button
                    type="button"
                    size="sm"
                    variant="secondary"
                    onClick={() => activatePlan.mutate({ planId: plan.id })}
                    disabled={activatePlan.isPending}
                  >
                    Aktif Yap
                  </Button>
                ) : null}
                <Button
                  type="button"
                  size="sm"
                  variant="secondary"
                  onClick={() => setOpenId(openId === plan.id ? null : plan.id)}
                >
                  {openId === plan.id ? 'Kapat' : 'Düzenle'}
                </Button>
                <button
                  type="button"
                  onClick={() => setDeletingId(plan.id)}
                  aria-label={`${plan.name} planını sil`}
                  className="text-text-muted transition-colors hover:text-error"
                >
                  <TrashIcon className="h-4 w-4" />
                </button>
              </div>
            </div>

            {openId === plan.id ? (
              <div className="mt-5 space-y-5 border-t border-border pt-5">
                <MacroSummary plan={plan} />
                <NutritionPlanEditor plan={plan} />
              </div>
            ) : null}
          </section>
        ))
      ) : (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          Bu üyeye henüz beslenme planı atanmadı.
        </p>
      )}

      <MemberIntakeHistory
        memberId={member.id}
        targetCalories={plans?.find((plan) => plan.active)?.targetCalories ?? null}
      />

      {deletingPlan ? (
        <ConfirmDialog
          title="Planı Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deletePlan.isPending}
          error={deletePlan.isError ? deletePlan.error.message : null}
          description={`"${deletingPlan.name}" beslenme planını silmek istediğinize emin misiniz?`}
          onCancel={() => setDeletingId(null)}
          onConfirm={() =>
            deletePlan.mutate({ planId: deletingPlan.id }, { onSuccess: () => setDeletingId(null) })
          }
        />
      ) : null}
    </div>
  )
}
