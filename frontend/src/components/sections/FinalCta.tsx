import { Button } from '@/components/ui/Button'
import { navigateToSection } from '@/lib/scroll'

export function FinalCta() {
  return (
    <section className="py-20 sm:py-24">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        <div className="relative mx-auto flex max-w-4xl flex-col items-center overflow-hidden rounded-2xl border border-border bg-surface p-10 text-center sm:p-16">
          <div
            className="pointer-events-none absolute -right-20 -bottom-20 h-[350px] w-[350px] rounded-full bg-accent/15 blur-[120px]"
            aria-hidden
          />
          <span className="mb-3 text-xs font-semibold uppercase tracking-wider text-primary">Hemen Başlayın</span>
          <h2 className="max-w-2xl text-balance font-display text-3xl font-extrabold uppercase leading-tight tracking-tight text-text-primary sm:text-4xl">
            Değişimin ilk adımını bugün at.
          </h2>
          <p className="mt-4 max-w-xl text-sm text-text-secondary sm:text-base">
            Daha fazla erteleme. Biyomekanik temelli antrenman protokolü ve kişisel beslenme koçluğu ile hedefine en
            kestirme yoldan ulaş.
          </p>
          <div className="mt-8 flex flex-wrap items-center justify-center gap-4">
            <Button size="lg" onClick={() => navigateToSection('#paketler', 'paketler')}>
              Koçluğa Başla
            </Button>
            <Button variant="secondary" size="lg" onClick={() => navigateToSection('#sss', 'sss')}>
              Sorularım Var
            </Button>
          </div>
          <div className="mt-8 flex items-center justify-center gap-6 text-xs text-text-muted">
            <span className="flex items-center gap-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-success" /> Test Modu — Simülasyon Ödeme
            </span>
            <span className="flex items-center gap-1.5">
              <span className="h-1.5 w-1.5 rounded-full bg-primary" /> 48 Saatte Program Teslimi
            </span>
          </div>
        </div>
      </div>
    </section>
  )
}
