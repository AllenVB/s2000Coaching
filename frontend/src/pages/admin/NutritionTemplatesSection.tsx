import { useState } from 'react'

import { useCreateNutritionTemplate, useDeleteNutritionPlan, useNutritionTemplates } from '@/api/nutritionApi'
import { NutritionPlanEditor } from '@/components/nutrition/NutritionPlanEditor'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { inputClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'

export function NutritionTemplatesSection() {
  const { data: templates, isLoading, isError, error } = useNutritionTemplates()
  const createTemplate = useCreateNutritionTemplate()
  const deletePlan = useDeleteNutritionPlan()

  const [name, setName] = useState('')
  const [calories, setCalories] = useState('')
  const [openId, setOpenId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = name.trim()
    if (trimmed.length === 0) {
      return
    }
    createTemplate.mutate(
      { input: { name: trimmed, targetCalories: calories.trim() === '' ? null : Number(calories) } },
      {
        onSuccess: (template) => {
          setName('')
          setCalories('')
          setOpenId(template.id)
        },
      },
    )
  }

  const deletingTemplate = templates?.find((template) => template.id === deletingId)

  return (
    <div className="space-y-6">
      <form className="flex flex-wrap items-end gap-3" onSubmit={submit}>
        <label className="min-w-[200px] flex-1">
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Şablon Adı</span>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            type="text"
            placeholder="Örn: 2400 kcal Kütle"
            className={inputClasses}
          />
        </label>
        <label className="w-32">
          <span className="mb-1.5 block text-xs font-medium text-text-secondary">Hedef kcal</span>
          <input
            value={calories}
            onChange={(event) => setCalories(event.target.value)}
            type="number"
            min={500}
            max={10000}
            placeholder="—"
            className={inputClasses}
          />
        </label>
        <Button type="submit" disabled={createTemplate.isPending || name.trim().length === 0}>
          {createTemplate.isPending ? 'Oluşturuluyor…' : 'Şablon Oluştur'}
        </Button>
      </form>
      {createTemplate.isError ? <p className="text-xs text-error">{createTemplate.error.message}</p> : null}

      {isLoading ? (
        <div className="h-32 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : isError ? (
        <p className="rounded-xl border border-error/40 bg-error/10 p-3.5 text-xs text-error">
          Şablonlar yüklenemedi: {error.message}
        </p>
      ) : templates && templates.length > 0 ? (
        templates.map((template) => (
          <section key={template.id} className="rounded-2xl border border-border bg-surface p-6">
            <div className="flex flex-wrap items-center justify-between gap-3">
              <div>
                <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
                  {template.name}
                </h2>
                <p className="mt-1 text-xs text-text-muted">
                  {template.meals.length} öğün ·{' '}
                  {template.meals.reduce((total, meal) => total + meal.items.length, 0)} besin
                </p>
              </div>
              <div className="flex items-center gap-2">
                <Button
                  type="button"
                  size="sm"
                  variant="secondary"
                  onClick={() => setOpenId(openId === template.id ? null : template.id)}
                >
                  {openId === template.id ? 'Kapat' : 'Düzenle'}
                </Button>
                <button
                  type="button"
                  onClick={() => setDeletingId(template.id)}
                  aria-label={`${template.name} şablonunu sil`}
                  className="text-text-muted transition-colors hover:text-error"
                >
                  <TrashIcon className="h-4 w-4" />
                </button>
              </div>
            </div>

            {openId === template.id ? (
              <div className="mt-5 space-y-5 border-t border-border pt-5">
                <NutritionPlanEditor plan={template} />
              </div>
            ) : null}
          </section>
        ))
      ) : (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
          Henüz beslenme şablonu yok.
        </p>
      )}

      {deletingTemplate ? (
        <ConfirmDialog
          title="Şablonu Sil"
          confirmLabel="Evet, Sil"
          tone="danger"
          isPending={deletePlan.isPending}
          error={deletePlan.isError ? deletePlan.error.message : null}
          description={`"${deletingTemplate.name}" şablonunu silmek istediğinize emin misiniz? Üyelere atanmış kopyalar etkilenmez.`}
          onCancel={() => setDeletingId(null)}
          onConfirm={() =>
            deletePlan.mutate({ planId: deletingTemplate.id }, { onSuccess: () => setDeletingId(null) })
          }
        />
      ) : null}
    </div>
  )
}
