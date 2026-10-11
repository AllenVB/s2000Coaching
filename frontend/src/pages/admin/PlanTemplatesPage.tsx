import { useState } from 'react'

import { useCreateTemplate, useDeletePlan, usePlanTemplates } from '@/api/trainingApi'
import { PlanEditor } from '@/components/training/PlanEditor'
import { Button } from '@/components/ui/Button'
import { ConfirmDialog } from '@/components/ui/ConfirmDialog'
import { inputClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { TrashIcon } from '@/components/ui/icons'
import { useSignOutOn401 } from '@/hooks/useAdminSession'

/** Write a programme once here, then copy it onto each member who needs it. */
export function PlanTemplatesPage() {
  const { data: templates, isLoading, isError, error } = usePlanTemplates()
  useSignOutOn401(error)

  const createTemplate = useCreateTemplate()
  const deletePlan = useDeletePlan()

  const [name, setName] = useState('')
  const [openId, setOpenId] = useState<string | null>(null)
  const [deletingId, setDeletingId] = useState<string | null>(null)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    const trimmed = name.trim()
    if (trimmed.length === 0) {
      return
    }
    createTemplate.mutate(
      { name: trimmed },
      {
        onSuccess: (template) => {
          setName('')
          setOpenId(template.id)
        },
      },
    )
  }

  const deletingTemplate = templates?.find((template) => template.id === deletingId)

  return (
    <>
      <PageHeading
        title="Plan Şablonları"
        description="Şablonu bir kez yazın, üyeye atarken kopyalansın. Kopya bağımsızdır: üyenin planında yaptığınız değişiklik şablonu etkilemez."
      />

      <div className="space-y-6">
        <form className="flex flex-wrap items-center gap-2" onSubmit={submit}>
          <input
            value={name}
            onChange={(event) => setName(event.target.value)}
            type="text"
            placeholder="Örn: Push Pull Legs"
            aria-label="Yeni şablon adı"
            className={`${inputClasses} max-w-xs`}
          />
          <Button type="submit" disabled={createTemplate.isPending || name.trim().length === 0}>
            {createTemplate.isPending ? 'Oluşturuluyor…' : 'Şablon Oluştur'}
          </Button>
        </form>
        {createTemplate.isError ? (
          <p className="text-xs text-error">{createTemplate.error.message}</p>
        ) : null}

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
                    {template.days.length} gün ·{' '}
                    {template.days.reduce((total, day) => total + day.exercises.length, 0)} hareket
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
                <div className="mt-5 border-t border-border pt-5">
                  <PlanEditor plan={template} />
                </div>
              ) : null}
            </section>
          ))
        ) : (
          <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">
            Henüz şablon yok.
          </p>
        )}
      </div>

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
    </>
  )
}
