import type { ReactNode } from 'react'

/** Title row shared by the member and management pages. */
export function PageHeading({
  title,
  description,
  action,
}: {
  title: string
  description?: string
  action?: ReactNode
}) {
  return (
    <div className="mb-6 flex flex-wrap items-start justify-between gap-4">
      <div>
        <h1 className="font-display text-2xl font-extrabold uppercase tracking-tight text-text-primary sm:text-3xl">
          {title}
        </h1>
        {description ? (
          <p className="mt-2 max-w-2xl text-sm leading-relaxed text-text-secondary">{description}</p>
        ) : null}
      </div>
      {action}
    </div>
  )
}

/** Card standing in for a feature that lands in a later phase. */
export function ComingSoonCard({ title, description }: { title: string; description: string }) {
  return (
    <section className="rounded-2xl border border-dashed border-border-strong bg-surface/40 p-6">
      <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">{title}</h2>
      <p className="mt-2 text-sm leading-relaxed text-text-secondary">{description}</p>
      <p className="mt-3 text-[11px] font-semibold uppercase tracking-wider text-text-muted">Yakında</p>
    </section>
  )
}
