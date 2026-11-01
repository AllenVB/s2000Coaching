import { useState } from 'react'

import { useCoupons, useCreateCoupon, useSetCouponActive } from '@/api/couponsApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import { PageHeading } from '@/components/ui/PageHeading'
import { useSignOutOn401 } from '@/hooks/useAdminSession'
import { formatDate, todayIso } from '@/lib/date'

/** Discount codes: create one (percentage, optional use limit and last day), switch one on or off. */
export function CouponsPage() {
  const { data: coupons, isLoading, error } = useCoupons()
  useSignOutOn401(error)
  const create = useCreateCoupon()
  const setActive = useSetCouponActive()
  const [code, setCode] = useState('')
  const [percent, setPercent] = useState('10')
  const [maxUses, setMaxUses] = useState('')
  const [expires, setExpires] = useState('')
  const [formError, setFormError] = useState<string | null>(null)

  const submit = (event: React.FormEvent) => {
    event.preventDefault()
    setFormError(null)
    const percentOff = Number(percent)
    const limit = maxUses.trim() === '' ? null : Number(maxUses)
    if (!Number.isInteger(percentOff) || percentOff < 1 || percentOff > 90) {
      setFormError('İndirim yüzdesi 1 ile 90 arasında bir tam sayı olmalı.')
      return
    }
    if (limit !== null && (!Number.isInteger(limit) || limit < 1)) {
      setFormError('Kullanım sınırı 1 ya da daha büyük bir tam sayı olmalı; sınırsız için boş bırakın.')
      return
    }
    create.mutate(
      { code: code.trim(), percentOff, maxUses: limit, expiresOn: expires || null },
      { onSuccess: () => { setCode(''); setMaxUses(''); setExpires('') } },
    )
  }

  return (
    <>
      <PageHeading
        title="İndirim Kodları"
        description="Ödeme ekranında girilen yüzdelik indirim kodları. Kod büyük/küçük harf fark etmez; kullanım sınırı ve son gün isteğe bağlıdır."
      />
      <form onSubmit={submit} className="mb-6 rounded-2xl border border-border bg-surface p-5">
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Field label="Kod *"><input value={code} onChange={(e) => setCode(e.target.value)} maxLength={40} placeholder="örn. YAZ20" className={`${inputClasses} uppercase`} /></Field>
          <Field label="İndirim % *"><input value={percent} onChange={(e) => setPercent(e.target.value)} inputMode="numeric" className={inputClasses} /></Field>
          <Field label="Kullanım sınırı" hint="Boş = sınırsız"><input value={maxUses} onChange={(e) => setMaxUses(e.target.value)} inputMode="numeric" className={inputClasses} /></Field>
          <Field label="Son gün" hint="Boş = süresiz"><input type="date" value={expires} min={todayIso()} onChange={(e) => setExpires(e.target.value)} className={inputClasses} /></Field>
        </div>
        {formError || create.isError ? <p role="alert" className="mt-3 text-xs text-error">{formError ?? create.error?.message}</p> : null}
        <Button type="submit" className="mt-4" disabled={create.isPending || code.trim().length < 3}>Kod Oluştur</Button>
      </form>

      {isLoading ? (
        <div className="h-20 animate-pulse rounded-2xl border border-border bg-surface" />
      ) : !coupons || coupons.length === 0 ? (
        <p className="rounded-2xl border border-border bg-surface p-6 text-sm italic text-text-muted">Henüz kod yok.</p>
      ) : (
        <ul className="space-y-2">
          {coupons.map((coupon) => (
            <li key={coupon.id} className="flex flex-wrap items-center justify-between gap-3 rounded-xl border border-border bg-surface p-4 text-sm">
              <span>
                <strong className="font-mono text-text-primary">{coupon.code}</strong>
                <span className="ml-2 text-text-secondary">%{coupon.percentOff}</span>
                <span className="ml-3 text-xs text-text-muted">
                  {coupon.usedCount}{coupon.maxUses ? ` / ${coupon.maxUses}` : ''} kullanım
                  {coupon.expiresOn ? ` · ${formatDate(coupon.expiresOn)} son gün` : ''}
                </span>
              </span>
              <button
                type="button"
                disabled={setActive.isPending}
                onClick={() => setActive.mutate({ id: coupon.id, active: !coupon.active })}
                className={`text-xs font-semibold uppercase tracking-wide hover:underline ${coupon.active ? 'text-primary' : 'text-text-muted'}`}
              >
                {coupon.active ? 'Açık — kapat' : 'Kapalı — aç'}
              </button>
            </li>
          ))}
        </ul>
      )}
    </>
  )
}
