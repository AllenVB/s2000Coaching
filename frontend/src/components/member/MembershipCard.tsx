import { Link } from 'react-router-dom'

import { useMyMemberships } from '@/api/membershipApi'
import { formatDate } from '@/lib/date'
import type { Membership } from '@/types/membership'

/** Days before the end at which the member is told to renew. */
export const RENEWAL_NOTICE_DAYS = 14

/** The period the member has paid for and when it ends. Nothing is shown until they have one. */
export function MembershipCard() {
  const { data: memberships } = useMyMemberships()
  const current = memberships?.[0]

  if (!current) {
    return null
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="mb-3 flex flex-wrap items-baseline justify-between gap-2 border-b border-border pb-3">
        <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Paketim</h2>
        <span className="text-xs text-text-muted">{current.label}</span>
      </div>
      <p className="text-sm text-text-primary">
        {formatDate(current.startsOn)} – <strong>{formatDate(current.endsOn)}</strong>
      </p>
      <p className={`mt-1 text-xs ${tone(current)}`}>{status(current)}</p>
      {current.daysLeft <= RENEWAL_NOTICE_DAYS ? <RenewLink /> : null}
    </section>
  )
}

function tone(membership: Membership): string {
  if (membership.daysLeft < 0) return 'text-error'
  return membership.daysLeft <= RENEWAL_NOTICE_DAYS ? 'text-warning' : 'text-text-secondary'
}

export function status(membership: Membership): string {
  if (membership.daysLeft < 0) return `Süreniz ${-membership.daysLeft} gün önce doldu.`
  if (membership.daysLeft === 0) return 'Süreniz bugün doluyor.'
  return `${membership.daysLeft} gün kaldı.`
}

/** A single line on the home screen, only when the end is near or has passed. */
export function RenewalNotice() {
  const { data: memberships } = useMyMemberships()
  const current = memberships?.[0]

  if (!current || current.daysLeft > RENEWAL_NOTICE_DAYS) {
    return null
  }

  return (
    <p
      className={`rounded-xl border p-3.5 text-xs ${
        current.daysLeft < 0
          ? 'border-error/40 bg-error/10 text-error'
          : 'border-warning/40 bg-warning/10 text-warning'
      }`}
    >
      {status(current)} Kesintisiz devam etmek için paketinizi yenileyin; kalan günleriniz kaybolmaz, yeni süre bitişin üstüne eklenir.{' '}
      <RenewLink />
    </p>
  )
}

/** Takes a signed-in member to the packages with their details already filled in at checkout. */
function RenewLink() {
  return (
    <Link
      to="/#paketler"
      className="mt-2 inline-block rounded-lg bg-primary px-3.5 py-2 text-[11px] font-bold uppercase tracking-wide text-canvas hover:bg-primary-hover"
    >
      Paketi Yenile
    </Link>
  )
}
