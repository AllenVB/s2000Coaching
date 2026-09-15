import { Button } from '@/components/ui/Button'
import { CheckIcon } from '@/components/ui/icons'
import { ecosystemFeatures } from '@/data/content'

export function AppEcosystem() {
  return (
    <section className="border-b border-border bg-surface/40 py-20 sm:py-24">
      <div className="mx-auto grid max-w-[1280px] grid-cols-1 items-center gap-12 px-4 sm:px-6 lg:grid-cols-12 lg:gap-16 lg:px-8">
        <div className="lg:col-span-6">
          <div className="aspect-[4/3] overflow-hidden rounded-2xl border border-border bg-surface shadow-2xl">
            <img
              src="https://images.unsplash.com/photo-1552674605-db6ffd4facb5?auto=format&fit=crop&w=900&q=80"
              alt="S2000 Coaching dijital performans takip kokpiti"
              className="h-full w-full object-cover"
              loading="lazy"
              width={900}
              height={675}
            />
          </div>
        </div>

        <div className="flex flex-col lg:col-span-6">
          <span className="mb-3 text-xs font-semibold uppercase tracking-wider text-primary">Dijital Altyapı</span>
          <h2 className="mb-4 text-balance font-display text-3xl font-extrabold uppercase tracking-tight text-text-primary sm:text-4xl">
            Tek Bir Program Değil, <br className="hidden sm:inline" />
            Sana Özel Bir Sistem.
          </h2>
          <p className="mb-8 text-base leading-relaxed text-text-secondary">
            Akıllı telefon veya tabletinizde çalışan modern sporcu takip altyapısı. Setler, tekrarlar, ağırlık
            artışları ve gramajlı beslenme tek bir mimaride buluşuyor.
          </p>

          <div className="mb-8 space-y-4">
            {ecosystemFeatures.map((feature) => (
              <div key={feature.title} className="flex items-start gap-3">
                <span className="mt-0.5 flex h-5 w-5 shrink-0 items-center justify-center rounded-full border border-border bg-surface text-primary">
                  <CheckIcon className="h-3 w-3" />
                </span>
                <div>
                  <div className="text-sm font-semibold text-text-primary">{feature.title}</div>
                  <div className="mt-0.5 text-xs text-text-secondary">{feature.description}</div>
                </div>
              </div>
            ))}
          </div>

          <Button variant="secondary" className="w-fit" onClick={() => (window.location.hash = '#paketler')}>
            Paketleri İncele
          </Button>
        </div>
      </div>
    </section>
  )
}
