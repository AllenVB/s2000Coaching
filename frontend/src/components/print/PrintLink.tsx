import { Link } from 'react-router-dom'

/** The member's way to their printable plan, from the pages that show the plan. */
export function PrintLink() {
  return (
    <Link
      to="/uye/cikti"
      className="rounded-lg border border-border-strong px-3.5 py-2 text-xs font-semibold uppercase tracking-wide text-text-secondary transition-colors hover:border-primary/60 hover:text-text-primary"
    >
      Yazdır / PDF
    </Link>
  )
}
