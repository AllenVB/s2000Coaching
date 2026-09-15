import { Button } from '@/components/ui/Button'
import { CheckIcon } from '@/components/ui/icons'
import { useCart } from '@/context/CartContext'
import { formatMinor } from '@/lib/money'
import type { CoachingPackage } from '@/types/package'

export function PricingCard({ pkg }: { pkg: CoachingPackage }) {
  const { addToCart } = useCart()
  const discountPercent = Math.round((1 - pkg.priceMinor / pkg.originalPriceMinor) * 100)

  if (pkg.featured) {
    return (
      <div className="relative flex flex-col justify-between rounded-2xl bg-gradient-to-b from-accent via-primary to-accent p-[2px] shadow-[0_0_45px_rgba(16,185,129,0.25)] md:-translate-y-3">
        <div className="flex h-full flex-col justify-between rounded-[14px] bg-canvas p-6 sm:p-8">
          <div>
            <div className="mb-2 flex items-center justify-between gap-2">
              <span className="inline-flex items-center gap-1.5 rounded-full bg-gradient-to-r from-accent to-primary px-3 py-1 text-[11px] font-bold uppercase tracking-wider text-canvas">
                {pkg.badge}
              </span>
            </div>
            <h3 className="font-display text-xl font-bold uppercase text-text-primary">{pkg.name}</h3>
            <p className="mt-1 text-xs leading-relaxed text-text-secondary">{pkg.tagline}</p>

            <div className="my-6 rounded-xl bg-surface p-3">
              <div className="flex items-baseline gap-2">
                <span className="text-sm text-text-muted line-through">{formatMinor(pkg.originalPriceMinor)}</span>
                <span className="font-display text-2xl font-black text-primary">{formatMinor(pkg.priceMinor)}</span>
                {discountPercent > 0 ? (
                  <span className="rounded bg-primary/20 px-2 py-0.5 text-[11px] font-bold uppercase text-primary">
                    -%{discountPercent}
                  </span>
                ) : null}
              </div>
            </div>

            <ul className="space-y-3">
              {pkg.features.map((feature) => (
                <li key={feature.label} className="flex items-start gap-2.5 text-xs text-text-primary">
                  <CheckIcon className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                  <span>{feature.label}</span>
                </li>
              ))}
            </ul>
          </div>

          <Button className="mt-8 w-full" onClick={() => addToCart(pkg)}>
            Hemen Koçluğa Başla
          </Button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-col justify-between rounded-2xl border border-border bg-surface p-6 transition-colors hover:border-white/16 sm:p-8">
      <div>
        <span className="mb-2 block text-[11px] font-semibold uppercase tracking-wider text-text-muted">
          {pkg.badge}
        </span>
        <h3 className="font-display text-xl font-bold uppercase text-text-primary">{pkg.name}</h3>
        <p className="mt-1 text-xs leading-relaxed text-text-secondary">{pkg.tagline}</p>

        <div className="my-6 border-t border-border pt-5">
          <div className="flex items-baseline gap-2">
            <span className="font-display text-2xl font-extrabold text-text-primary">
              {formatMinor(pkg.priceMinor)}
            </span>
            <span className="text-xs text-text-muted line-through">{formatMinor(pkg.originalPriceMinor)}</span>
          </div>
        </div>

        <ul className="space-y-3 border-t border-border pt-6 text-xs text-text-secondary">
          {pkg.features.map((feature) => (
            <li key={feature.label} className="flex items-center gap-2">
              <CheckIcon className="h-3.5 w-3.5 shrink-0 text-primary" />
              <span>{feature.label}</span>
            </li>
          ))}
        </ul>
      </div>

      <Button variant="secondary" className="mt-8 w-full" onClick={() => addToCart(pkg)}>
        Paketi Seç
      </Button>
    </div>
  )
}
