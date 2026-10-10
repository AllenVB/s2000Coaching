import { useEffect, useRef, type ReactNode } from 'react'

import { Button } from '@/components/ui/Button'
import { CloseIcon } from '@/components/ui/icons'

interface ConfirmDialogProps {
  title: string
  description?: ReactNode
  confirmLabel: string
  cancelLabel?: string
  tone?: 'primary' | 'danger'
  isPending?: boolean
  error?: string | null
  onConfirm: () => void
  onCancel: () => void
}

/** Blocking yes/no prompt used before creating or deleting a member. */
export function ConfirmDialog({
  title,
  description,
  confirmLabel,
  cancelLabel = 'Vazgeç',
  tone = 'primary',
  isPending = false,
  error,
  onConfirm,
  onCancel,
}: ConfirmDialogProps) {
  const confirmRef = useRef<HTMLButtonElement>(null)

  useEffect(() => {
    confirmRef.current?.focus()
  }, [])

  useEffect(() => {
    const handleKeyDown = (event: KeyboardEvent) => {
      if (event.key === 'Escape' && !isPending) {
        onCancel()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isPending, onCancel])

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm">
      <div
        role="dialog"
        aria-modal="true"
        aria-labelledby="confirm-dialog-title"
        className="relative w-full max-w-md overflow-hidden rounded-2xl border border-border bg-surface shadow-2xl"
      >
        <div className="flex items-center justify-between border-b border-border bg-surface-elevated px-5 py-4">
          <h3
            id="confirm-dialog-title"
            className="text-sm font-bold uppercase tracking-wider text-text-primary"
          >
            {title}
          </h3>
          <button
            type="button"
            aria-label="Kapat"
            onClick={onCancel}
            disabled={isPending}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-text-secondary hover:text-text-primary disabled:opacity-50"
          >
            <CloseIcon className="h-4 w-4" />
          </button>
        </div>

        <div className="space-y-4 p-5">
          {description ? <div className="text-sm leading-relaxed text-text-secondary">{description}</div> : null}
          {error ? <p className="text-xs text-error">{error}</p> : null}

          <div className="flex flex-col-reverse gap-2 sm:flex-row sm:justify-end">
            <Button type="button" variant="secondary" onClick={onCancel} disabled={isPending}>
              {cancelLabel}
            </Button>
            <Button
              ref={confirmRef}
              type="button"
              variant={tone === 'danger' ? 'danger' : 'primary'}
              onClick={onConfirm}
              disabled={isPending}
            >
              {isPending ? 'İşleniyor…' : confirmLabel}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )
}
