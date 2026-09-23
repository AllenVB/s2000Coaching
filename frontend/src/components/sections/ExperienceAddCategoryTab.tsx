import { useState } from 'react'

import { useCreateCategory } from '@/api/experienceApi'
import { PlusIcon } from '@/components/ui/icons'
import { ensureEditToken } from '@/lib/editToken'

export function ExperienceAddCategoryTab({ onCreated }: { onCreated: (categoryId: string) => void }) {
  const [isOpen, setIsOpen] = useState(false)
  const [name, setName] = useState('')

  const createCategory = useCreateCategory()

  const close = () => {
    setIsOpen(false)
    setName('')
    createCategory.reset()
  }

  const save = () => {
    const trimmed = name.trim()
    if (!trimmed) return
    if (!ensureEditToken()) return
    createCategory.mutate(
      { name: trimmed },
      { onSuccess: (category) => { close(); onCreated(category.id) } },
    )
  }

  if (!isOpen) {
    return (
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        className="inline-flex items-center gap-1.5 rounded-lg border border-dashed border-border-strong px-4 py-2 text-xs font-semibold uppercase tracking-wider text-text-secondary transition-colors hover:border-primary/50 hover:text-primary"
      >
        <PlusIcon className="h-3.5 w-3.5" />
        Kategori Ekle
      </button>
    )
  }

  return (
    <div className="inline-flex items-center gap-1.5 rounded-lg bg-surface p-1">
      <input
        value={name}
        onChange={(event) => setName(event.target.value)}
        onKeyDown={(event) => {
          if (event.key === 'Enter') save()
          if (event.key === 'Escape') close()
        }}
        placeholder="Kategori adı"
        maxLength={60}
        autoFocus
        className="w-32 rounded-md border border-primary/50 bg-surface-elevated px-2 py-1.5 text-xs font-semibold uppercase tracking-wider text-text-primary placeholder:normal-case placeholder:font-normal placeholder:text-text-muted focus:outline-none"
      />
      <button
        type="button"
        onClick={save}
        disabled={createCategory.isPending}
        className="rounded-md bg-primary px-2 py-1.5 text-[11px] font-bold uppercase text-canvas"
      >
        Ekle
      </button>
      <button
        type="button"
        onClick={close}
        className="px-1.5 text-[11px] font-medium text-text-secondary hover:text-text-primary"
      >
        Vazgeç
      </button>
    </div>
  )
}
