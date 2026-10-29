import { Link } from 'react-router-dom'

import type { ReactNode } from 'react'

/** Minimal header for the member and management pages — no landing-page section nav or cart. */
export function PortalHeader({
  eyebrow,
  title,
  action,
}: {
  eyebrow: string
  title: string
  action?: ReactNode
}) {
  return (
    <header className="sticky top-0 z-40 w-full print:hidden border-b border-border bg-canvas/90 backdrop-blur-md">
      <div className="mx-auto flex min-h-[72px] max-w-[1280px] flex-wrap items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
        <div className="flex items-center gap-3">
          <Link
            to="/"
            aria-label="Ana sayfaya dön"
            className="flex h-9 w-9 shrink-0 items-center justify-center rounded-lg border border-border bg-surface font-display text-sm font-black tracking-wider text-primary"
          >
            FC
          </Link>
          <div className="flex flex-col leading-none">
            <span className="text-[10px] font-semibold uppercase tracking-[0.22em] text-primary">{eyebrow}</span>
            <span className="mt-1 font-display text-base font-bold uppercase tracking-wider text-text-primary">
              {title}
            </span>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/"
            className="text-xs font-medium text-text-secondary transition-colors hover:text-text-primary"
          >
            Siteye dön
          </Link>
          {action}
        </div>
      </div>
    </header>
  )
}
