import { usePackages } from '@/api/packagesApi'
import { SectionHeading } from '@/components/ui/SectionHeading'
import { ClockIcon, CreditCardIcon, ShieldIcon } from '@/components/ui/icons'
import { trustBadges } from '@/data/content'
import { usePricingCategory } from '@/context/PricingCategoryContext'

import { PricingCard } from './PricingCard'

const badgeIcons = { credit_card: CreditCardIcon, schedule: ClockIcon, lock: ShieldIcon }

export function Pricing() {
  const { category, setCategory } = usePricingCategory()
  const { data: packages, isLoading, isError } = usePackages()

  const filtered = packages?.filter((pkg) => pkg.category === category) ?? []

  return (
    <section id="paketler" className="scroll-mt-24 border-b border-border py-20 sm:py-24">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        <div className="mb-12">
          <SectionHeading
            eyebrow="Planlar & Fiyatlandırma"
            title="Hedefine Uygun Dönüşüm Paketini Seç"
            description="Hem antrenman hem beslenmeyi kapsayan tam koçluk protokolleri veya bağımsız beslenme danışmanlığı."
          />

          <div className="mt-8 flex justify-center">
            <div className="inline-flex items-center gap-1 rounded-xl bg-surface p-1.5">
              <button
                type="button"
                onClick={() => setCategory('KOCLUK')}
                className={`rounded-lg px-6 py-2.5 text-xs font-semibold uppercase tracking-wider transition-all ${
                  category === 'KOCLUK' ? 'bg-primary text-canvas' : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                Online Koçluk
              </button>
              <button
                type="button"
                onClick={() => setCategory('BESLENME')}
                className={`rounded-lg px-6 py-2.5 text-xs font-semibold uppercase tracking-wider transition-all ${
                  category === 'BESLENME' ? 'bg-primary text-canvas' : 'text-text-secondary hover:text-text-primary'
                }`}
              >
                Beslenme
              </button>
            </div>
          </div>
        </div>

        {isLoading ? (
          <div className="grid grid-cols-1 gap-5 md:grid-cols-3">
            {[0, 1, 2].map((i) => (
              <div key={i} className="h-96 animate-pulse rounded-2xl border border-border bg-surface" />
            ))}
          </div>
        ) : null}

        {isError ? (
          <p className="text-center text-sm text-error">
            Paketler yüklenemedi. Backend servisinin çalıştığından emin olun.
          </p>
        ) : null}

        {!isLoading && !isError ? (
          <div className="grid grid-cols-1 items-stretch gap-5 sm:grid-cols-2 lg:grid-cols-3">
            {filtered.map((pkg) => (
              <PricingCard key={pkg.id} pkg={pkg} />
            ))}
          </div>
        ) : null}

        <div className="mt-12 flex flex-wrap items-center justify-around gap-6 rounded-2xl bg-surface/50 p-6">
          {trustBadges.map((badge) => {
            const Icon = badgeIcons[badge.icon as keyof typeof badgeIcons]
            return (
              <div key={badge.title} className="flex items-center gap-3">
                <div className="flex h-10 w-10 items-center justify-center rounded-full bg-primary/15 text-primary">
                  <Icon className="h-5 w-5" />
                </div>
                <div>
                  <p className="text-xs font-semibold uppercase text-text-primary">{badge.title}</p>
                  <p className="text-xs text-text-secondary">{badge.caption}</p>
                </div>
              </div>
            )
          })}
        </div>
      </div>
    </section>
  )
}
