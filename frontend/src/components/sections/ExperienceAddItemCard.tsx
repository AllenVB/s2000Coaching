import { useState } from 'react'

import { useCreateItem } from '@/api/experienceApi'
import { Button } from '@/components/ui/Button'
import { PlusIcon } from '@/components/ui/icons'

const inputClasses =
  'w-full rounded-lg border border-border bg-surface-elevated px-3.5 py-2.5 text-sm text-text-primary focus:border-primary focus:outline-none'

export function ExperienceAddItemCard({ categoryId }: { categoryId: string }) {
  const [isOpen, setIsOpen] = useState(false)
  const [title, setTitle] = useState('')
  const [description, setDescription] = useState('')

  const createItem = useCreateItem()

  const close = () => {
    setIsOpen(false)
    setTitle('')
    setDescription('')
    createItem.reset()
  }

  const save = () => {
    createItem.mutate(
      { categoryId, title: title.trim(), description: description.trim() },
      { onSuccess: close },
    )
  }

  if (!isOpen) {
    return (
      <button
        type="button"
        onClick={() => setIsOpen(true)}
        className="flex min-h-[120px] flex-col items-center justify-center gap-2 rounded-2xl border-2 border-dashed border-border-strong text-text-secondary transition-colors hover:border-primary/50 hover:text-primary"
      >
        <PlusIcon className="h-5 w-5" />
        <span className="text-xs font-semibold uppercase tracking-wider">Tecrübe Ekle</span>
      </button>
    )
  }

  return (
    <div className="flex flex-col gap-3 rounded-2xl border border-primary/40 bg-surface p-5">
      <input
        value={title}
        onChange={(event) => setTitle(event.target.value)}
        placeholder="Tecrübe başlığı"
        maxLength={160}
        autoFocus
        className={`${inputClasses} font-semibold`}
      />
      <textarea
        value={description}
        onChange={(event) => setDescription(event.target.value)}
        placeholder="Tecrübe açıklaması"
        rows={4}
        maxLength={5000}
        className={`${inputClasses} resize-y leading-relaxed`}
      />
      {createItem.isError ? <p className="text-xs text-error">Eklenemedi, tekrar deneyin.</p> : null}
      <div className="flex items-center gap-2">
        <Button
          size="md"
          className="h-9 px-4 text-xs"
          onClick={save}
          disabled={!title.trim() || !description.trim() || createItem.isPending}
        >
          {createItem.isPending ? 'Ekleniyor…' : 'Ekle'}
        </Button>
        <Button variant="secondary" className="h-9 px-4 text-xs" onClick={close}>
          İptal
        </Button>
      </div>
    </div>
  )
}
