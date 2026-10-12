import type { ReactNode } from 'react'

export const inputClasses =
  'h-11 w-full rounded-lg border border-border bg-surface px-3.5 text-xs text-text-primary focus:border-primary focus:outline-none'

export const textareaClasses =
  'min-h-[96px] w-full rounded-lg border border-border bg-surface px-3.5 py-2.5 text-xs leading-relaxed text-text-primary focus:border-primary focus:outline-none'

/**
 * Label + control + error message. The control is nested inside the `<label>`, which gives
 * it an implicit accessible name without threading ids through every call site.
 */
export function Field({
  label,
  error,
  hint,
  children,
}: {
  label: string
  error?: string
  hint?: string
  children: ReactNode
}) {
  return (
    <div>
      <label>
        <span className="mb-1.5 block text-xs font-medium text-text-secondary">{label}</span>
        {children}
      </label>
      {hint && !error ? <p className="mt-1 text-[11px] text-text-muted">{hint}</p> : null}
      {error ? <p className="mt-1 text-[11px] text-error">{error}</p> : null}
    </div>
  )
}
