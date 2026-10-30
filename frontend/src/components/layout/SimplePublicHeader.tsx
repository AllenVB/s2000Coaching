import { Link } from 'react-router-dom'

/** The slim header of the public pages that are not the home page (calculators, legal texts). */
export function SimplePublicHeader() {
  return (
    <header className="w-full border-b border-border bg-canvas/90">
      <div className="mx-auto flex min-h-[72px] max-w-[1280px] items-center justify-between gap-4 px-4 py-3 sm:px-6 lg:px-8">
        <Link to="/" className="flex items-center gap-3" aria-label="Ana sayfaya dön">
          <span className="flex h-9 w-9 items-center justify-center rounded-lg border border-border bg-surface font-display text-sm font-black tracking-wider text-primary">
            FC
          </span>
          <span className="flex flex-col leading-none">
            <span className="font-display text-base font-bold uppercase tracking-wider text-text-primary">S2000</span>
            <span className="mt-0.5 text-[10px] font-semibold uppercase tracking-[0.22em] text-primary">Coaching</span>
          </span>
        </Link>
        <div className="flex items-center gap-4">
          <Link to="/" className="text-xs font-medium text-text-secondary transition-colors hover:text-text-primary">
            Ana sayfa
          </Link>
          <Link to="/giris" className="text-xs font-semibold text-text-primary transition-colors hover:text-primary">
            Giriş Yap
          </Link>
        </div>
      </div>
    </header>
  )
}
