import { Button } from '@/components/ui/Button'
import { navigateToSection } from '@/lib/scroll'

export function FinalCta() {
  return (
    <section className="py-20 sm:py-24">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        {/* The page's last word gets the same ember edge as the featured package: the two
            places where the reader is asked to act look like each other. */}
        <div className="mx-auto max-w-4xl rounded-[32px] bg-gradient-to-br from-primary via-primary-deep to-accent-orange p-[2px] shadow-[0_30px_70px_-40px_rgba(255,107,44,0.7)]">
          <div className="ember-wash relative flex flex-col items-center overflow-hidden rounded-[30px] bg-canvas p-10 text-center sm:p-16">
            <span className="mb-4 flex items-center gap-3 text-[11px] font-bold uppercase tracking-[0.28em] text-primary">
              <span className="h-px w-8 bg-primary" aria-hidden />
              Hemen başlayın
            </span>
            <h2 className="max-w-2xl text-balance font-display text-[clamp(1.9rem,4.5vw,3rem)] font-extrabold uppercase leading-[0.98] text-text-primary">
              Değişimin ilk adımını bugün at.
            </h2>
            <p className="mt-5 max-w-xl text-sm leading-relaxed text-text-secondary sm:text-base">
              Daha fazla erteleme. Biyomekanik temelli antrenman protokolü ve kişisel beslenme koçluğu ile
              hedefine en kestirme yoldan ulaş.
            </p>
            <div className="mt-9 flex flex-wrap items-center justify-center gap-4">
              <Button size="lg" onClick={() => navigateToSection('#paketler', 'paketler')}>
                Koçluğa Başla
              </Button>
              <Button variant="secondary" size="lg" onClick={() => navigateToSection('#sss', 'sss')}>
                Sorularım Var
              </Button>
            </div>
            <div className="mt-9 flex flex-wrap items-center justify-center gap-x-6 gap-y-2 text-xs text-text-muted">
              <span className="flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 rounded-full bg-success" /> Test Modu — Simülasyon Ödeme
              </span>
              <span className="flex items-center gap-1.5">
                <span className="h-1.5 w-1.5 rounded-full bg-primary" /> 48 Saatte Program Teslimi
              </span>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}
