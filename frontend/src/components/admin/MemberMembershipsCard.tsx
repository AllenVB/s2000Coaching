import { useState } from 'react'

import { useDeleteMembership, useGrantMembership, useMemberMemberships } from '@/api/membershipApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import { TrashIcon } from '@/components/ui/icons'
import { formatDate } from '@/lib/date'

/**
 * The periods a member has paid for. A confirmed order adds one by itself; the form is for
 * money that arrived some other way (cash, a transfer made before the shop existed).
 */
export function MemberMembershipsCard({ memberId }: { memberId: string }) {
  const { data: memberships } = useMemberMemberships(memberId)
  const grant = useGrantMembership(memberId)
  const remove = useDeleteMembership(memberId)
  const [label, setLabel] = useState('')
  const [months, setMonths] = useState('1')

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    if (label.trim() === '') {
      return
    }
    grant.mutate({ label: label.trim(), months: Number(months) }, { onSuccess: () => setLabel('') })
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-4 border-b border-border pb-3 text-sm font-bold uppercase tracking-wider text-text-primary">
        Üyelik Süresi
      </h2>

      {memberships && memberships.length > 0 ? (
        <ul className="mb-5 space-y-2">
          {memberships.map((membership) => (
            <li
              key={membership.id}
              className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface-elevated px-4 py-3 text-sm"
            >
              <span className="min-w-0">
                <span className="block font-semibold text-text-primary">{membership.label}</span>
                <span className="block text-xs text-text-muted">
                  {formatDate(membership.startsOn)} – {formatDate(membership.endsOn)}
                  {membership.orderNumber ? ` · ${membership.orderNumber}` : ' · elle eklendi'}
                </span>
              </span>
              <span className="flex items-center gap-3">
                <span
                  className={`text-xs font-semibold ${
                    membership.daysLeft < 0 ? 'text-error' : membership.daysLeft <= 14 ? 'text-warning' : 'text-success'
                  }`}
                >
                  {membership.daysLeft < 0 ? 'Bitti' : `${membership.daysLeft} gün kaldı`}
                </span>
                <button
                  type="button"
                  onClick={() => remove.mutate(membership.id)}
                  aria-label={`${membership.label} kaydını sil`}
                  className="text-text-muted transition-colors hover:text-error"
                >
                  <TrashIcon className="h-4 w-4" />
                </button>
              </span>
            </li>
          ))}
        </ul>
      ) : (
        <p className="mb-5 text-sm italic text-text-muted">
          Henüz ödenmiş bir üyelik yok. Onaylanan bir sipariş otomatik ekler.
        </p>
      )}

      <form className="flex flex-wrap items-end gap-3" onSubmit={submit}>
        <div className="min-w-[200px] flex-1">
          <Field label="Elle Ekle (nakit vb.)">
            <input
              value={label}
              onChange={(event) => setLabel(event.target.value)}
              type="text"
              placeholder="Örn: 3 Aylık Koçluk — nakit"
              className={inputClasses}
            />
          </Field>
        </div>
        <div className="w-28">
          <Field label="Süre (ay)">
            <input
              value={months}
              onChange={(event) => setMonths(event.target.value)}
              type="number"
              min={1}
              max={36}
              className={inputClasses}
            />
          </Field>
        </div>
        <Button type="submit" variant="secondary" disabled={grant.isPending || label.trim() === ''}>
          Ekle
        </Button>
      </form>
      {grant.isError ? <p className="mt-2 text-xs text-error">{grant.error.message}</p> : null}
      <p className="mt-2 text-[11px] text-text-muted">
        Yeni dönem, önceki bitmemişse onun bitiminden başlar; erken yenileme gün kaybettirmez.
      </p>
    </section>
  )
}
