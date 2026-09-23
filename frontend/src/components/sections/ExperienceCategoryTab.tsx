import { useState } from 'react'

import { useDeleteCategory, useRenameCategory } from '@/api/experienceApi'
import { PencilIcon, TrashIcon } from '@/components/ui/icons'
import { ensureEditToken } from '@/lib/editToken'
import type { ExperienceCategory } from '@/types/experience'

export function ExperienceCategoryTab({
  category,
  isActive,
  isEditMode,
  onSelect,
}: {
  category: ExperienceCategory
  isActive: boolean
  isEditMode: boolean
  onSelect: () => void
}) {
  const [isRenaming, setIsRenaming] = useState(false)
  const [isConfirmingDelete, setIsConfirmingDelete] = useState(false)
  const [nameDraft, setNameDraft] = useState(category.name)

  const renameCategory = useRenameCategory()
  const deleteCategory = useDeleteCategory()

  const startRenaming = () => {
    setNameDraft(category.name)
    renameCategory.reset()
    setIsRenaming(true)
  }

  const saveRename = () => {
    const trimmed = nameDraft.trim()
    if (!trimmed) return
    if (!ensureEditToken()) return
    renameCategory.mutate(
      { categoryId: category.id, name: trimmed },
      { onSuccess: () => setIsRenaming(false) },
    )
  }

  if (isRenaming) {
    return (
      <div className="inline-flex items-center gap-1.5 rounded-lg bg-surface p-1">
        <input
          value={nameDraft}
          onChange={(event) => setNameDraft(event.target.value)}
          onKeyDown={(event) => {
            if (event.key === 'Enter') saveRename()
            if (event.key === 'Escape') setIsRenaming(false)
          }}
          maxLength={60}
          autoFocus
          className="w-32 rounded-md border border-primary/50 bg-surface-elevated px-2 py-1.5 text-xs font-semibold uppercase tracking-wider text-text-primary focus:outline-none"
        />
        <button
          type="button"
          onClick={saveRename}
          disabled={renameCategory.isPending}
          className="rounded-md bg-primary px-2 py-1.5 text-[11px] font-bold uppercase text-canvas"
        >
          Kaydet
        </button>
        <button
          type="button"
          onClick={() => setIsRenaming(false)}
          className="px-1.5 text-[11px] font-medium text-text-secondary hover:text-text-primary"
        >
          Vazgeç
        </button>
      </div>
    )
  }

  if (isConfirmingDelete) {
    return (
      <div className="inline-flex items-center gap-1.5 rounded-lg bg-surface p-1">
        <span className="px-1.5 text-[11px] font-medium text-text-secondary">"{category.name}" silinsin mi?</span>
        <button
          type="button"
          onClick={() => {
            if (!ensureEditToken()) return
            deleteCategory.mutate({ categoryId: category.id })
          }}
          disabled={deleteCategory.isPending}
          className="rounded-md bg-error/15 px-2 py-1.5 text-[11px] font-semibold text-error hover:bg-error/25"
        >
          Evet
        </button>
        <button
          type="button"
          onClick={() => setIsConfirmingDelete(false)}
          className="px-1.5 text-[11px] font-medium text-text-secondary hover:text-text-primary"
        >
          Vazgeç
        </button>
      </div>
    )
  }

  return (
    <div
      className={`inline-flex items-center gap-1 rounded-lg p-1 transition-all ${
        isActive ? 'bg-primary' : 'bg-transparent hover:bg-white/5'
      }`}
    >
      <button
        type="button"
        onClick={onSelect}
        className={`rounded-md px-4 py-1.5 text-xs font-semibold uppercase tracking-wider ${
          isActive ? 'text-canvas' : 'text-text-secondary hover:text-text-primary'
        }`}
      >
        {category.name}
      </button>
      {isEditMode ? (
        <>
          <button
            type="button"
            aria-label="Kategoriyi yeniden adlandır"
            onClick={startRenaming}
            className={`flex h-6 w-6 items-center justify-center rounded ${
              isActive ? 'text-canvas/70 hover:text-canvas' : 'text-text-muted hover:text-text-primary'
            }`}
          >
            <PencilIcon className="h-3 w-3" />
          </button>
          <button
            type="button"
            aria-label="Kategoriyi sil"
            onClick={() => setIsConfirmingDelete(true)}
            className={`flex h-6 w-6 items-center justify-center rounded ${
              isActive ? 'text-canvas/70 hover:text-canvas' : 'text-text-muted hover:text-error'
            }`}
          >
            <TrashIcon className="h-3 w-3" />
          </button>
        </>
      ) : null}
    </div>
  )
}
