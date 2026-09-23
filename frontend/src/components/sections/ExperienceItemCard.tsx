import { useState } from 'react'

import { useDeleteItem, useUpdateItem } from '@/api/experienceApi'
import { Button } from '@/components/ui/Button'
import { PencilIcon, TrashIcon } from '@/components/ui/icons'
import type { ExperienceItem } from '@/types/experience'

const inputClasses =
  'w-full rounded-lg border border-border bg-surface-elevated px-3.5 py-2.5 text-sm text-text-primary focus:border-primary focus:outline-none'

export function ExperienceItemCard({ item, isEditMode }: { item: ExperienceItem; isEditMode: boolean }) {
  const [isEditing, setIsEditing] = useState(false)
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)
  const [titleDraft, setTitleDraft] = useState(item.title)
  const [descriptionDraft, setDescriptionDraft] = useState(item.description)

  const updateItem = useUpdateItem()
  const deleteItem = useDeleteItem()

  const startEditing = () => {
    setTitleDraft(item.title)
    setDescriptionDraft(item.description)
    updateItem.reset()
    setIsEditing(true)
  }

  const save = () => {
    updateItem.mutate(
      { itemId: item.id, title: titleDraft.trim(), description: descriptionDraft.trim() },
      { onSuccess: () => setIsEditing(false) },
    )
  }

  const confirmDelete = () => {
    deleteItem.mutate({ itemId: item.id })
  }

  if (isEditing) {
    return (
      <div className="flex flex-col gap-3 rounded-2xl border border-primary/40 bg-surface p-5">
        <input
          value={titleDraft}
          onChange={(event) => setTitleDraft(event.target.value)}
          placeholder="Tecrübe başlığı"
          maxLength={160}
          className={`${inputClasses} font-semibold`}
        />
        <textarea
          value={descriptionDraft}
          onChange={(event) => setDescriptionDraft(event.target.value)}
          placeholder="Tecrübe açıklaması"
          rows={4}
          maxLength={5000}
          className={`${inputClasses} resize-y leading-relaxed`}
        />
        {updateItem.isError ? <p className="text-xs text-error">Kaydedilemedi, tekrar deneyin.</p> : null}
        <div className="flex items-center gap-2">
          <Button
            size="md"
            className="h-9 px-4 text-xs"
            onClick={save}
            disabled={!titleDraft.trim() || !descriptionDraft.trim() || updateItem.isPending}
          >
            {updateItem.isPending ? 'Kaydediliyor…' : 'Kaydet'}
          </Button>
          <Button variant="secondary" className="h-9 px-4 text-xs" onClick={() => setIsEditing(false)}>
            İptal
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="group relative flex flex-col gap-2 rounded-2xl border border-border bg-surface p-5 transition-colors hover:border-white/16">
      <h3 className="pr-14 text-base font-bold text-accent-orange">{item.title}</h3>
      <p className="whitespace-pre-wrap text-sm leading-relaxed text-text-primary">{item.description}</p>

      {isEditMode ? (
        <div className="absolute right-4 top-4 flex items-center gap-1.5">
          {isConfirmingDelete ? (
            <div className="flex items-center gap-1.5 rounded-lg bg-surface-elevated p-1">
              <span className="px-1 text-[11px] font-medium text-text-secondary">Silinsin mi?</span>
              <button
                type="button"
                onClick={confirmDelete}
                disabled={deleteItem.isPending}
                className="rounded-md bg-error/15 px-2 py-1 text-[11px] font-semibold text-error hover:bg-error/25"
              >
                Evet
              </button>
              <button
                type="button"
                onClick={() => setIsConfirmingDelete(false)}
                className="rounded-md px-2 py-1 text-[11px] font-medium text-text-secondary hover:text-text-primary"
              >
                Vazgeç
              </button>
            </div>
          ) : (
            <>
              <button
                type="button"
                aria-label="Tecrübeyi düzenle"
                onClick={startEditing}
                className="flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface-elevated text-text-secondary hover:border-primary/40 hover:text-primary"
              >
                <PencilIcon className="h-3.5 w-3.5" />
              </button>
              <button
                type="button"
                aria-label="Tecrübeyi sil"
                onClick={() => setIsConfirmingDelete(true)}
                className="flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface-elevated text-text-secondary hover:border-error/40 hover:text-error"
              >
                <TrashIcon className="h-3.5 w-3.5" />
              </button>
            </>
          )}
        </div>
      ) : null}
    </div>
  )
}
