import { Button } from '@/components/ui/Button'
import { navigateToSection } from '@/lib/scroll'

export function Hero() {
  return (
    <section
      id="ana-sayfa"
      className="ember-wash relative scroll-mt-24 overflow-hidden border-b border-border py-16 sm:py-24"
    >
      {/* A faint ruled grid under the wash. It gives the oversized type something to sit on,
          so the headline reads as set on a page rather than floating on black. */}
      <div
        className="pointer-events-none absolute inset-0 opacity-[0.07] [background-image:linear-gradient(to_right,var(--color-text-primary)_1px,transparent_1px),linear-gradient(to_bottom,var(--color-text-primary)_1px,transparent_1px)] [background-size:76px_76px] [mask-image:radial-gradient(70%_60%_at_30%_20%,#000,transparent)]"
        aria-hidden
      />

      <div className="relative mx-auto grid max-w-[1280px] grid-cols-1 items-center gap-12 px-4 sm:px-6 lg:grid-cols-12 lg:gap-16 lg:px-8">
        <div className="flex flex-col items-start lg:col-span-7">
          <div className="mb-7 flex items-center gap-3">
            <span className="h-px w-10 bg-primary" aria-hidden />
            <span className="text-[11px] font-bold uppercase tracking-[0.28em] text-primary">
              Yeni dönem kayıtları açık
            </span>
            <span className="rounded-full border border-border bg-surface px-2.5 py-1 text-[11px] font-bold text-text-secondary">
              Son 4 kontenjan
            </span>
          </div>

          {/* Two voices in one headline: the premise set quietly, the promise shouted. The
              size jump is the argument, so it is a scale step rather than a colour change. */}
          <h1 className="mb-7 max-w-[720px] font-display font-extrabold uppercase leading-[0.92] text-text-primary">
            <span className="block text-[clamp(1.6rem,4.4vw,2.6rem)] font-semibold tracking-[0.01em] text-text-secondary">
              Vücudunu değil,
            </span>
            <span className="block text-[clamp(2.9rem,9vw,5.4rem)]">
              Hayatını
              <span className="relative ml-3 inline-block text-primary">
                dönüştür.
                <span
                  className="absolute -bottom-1 left-0 h-[6px] w-full rounded-full bg-primary/35"
                  aria-hidden
                />
              </span>
            </span>
          </h1>

          <p className="mb-9 max-w-[560px] text-base leading-relaxed text-text-secondary sm:text-lg">
            Biyomekanik temelli kişiselleştirilmiş antrenman tasarımı, milimetrik beslenme stratejileri ve 7/24
            kesintisiz koç desteğiyle sınırlarını yeniden tanımla.
          </p>

          <div className="mb-10 flex w-full flex-wrap items-center gap-3 sm:w-auto">
            <Button
              size="lg"
              className="w-full sm:w-auto"
              onClick={() => navigateToSection('#paketler', 'paketler')}
            >
              Hemen Koçluğa Başla
            </Button>
            <Button
              variant="secondary"
              size="lg"
              className="w-full sm:w-auto"
              onClick={() => navigateToSection('#paketler', 'paketler')}
            >
              Paketleri İncele
            </Button>
          </div>

          <div className="grid w-full max-w-[560px] grid-cols-2 gap-px overflow-hidden rounded-2xl border border-border bg-border sm:grid-cols-4">
            <MicroStat value="500+" label="Aktif sporcu" />
            <MicroStat value="4.9★" label="Sporcu puanı" accent />
            <MicroStat value="%100" label="Kişiye özel" />
            <MicroStat value="7/24" label="Koç iletişimi" />
          </div>
        </div>

        <div className="relative lg:col-span-5">
          {/* The frame is offset rather than centred: a printed plate pinned to the page,
              with the ember rule behind it doing the work a drop shadow used to. */}
          <div className="relative mx-auto max-w-[460px]">
            <div
              className="absolute -left-3 -top-3 h-full w-full rounded-[28px] border border-primary/40"
              aria-hidden
            />
            <div className="relative overflow-hidden rounded-[28px] border border-border-strong bg-surface shadow-[0_30px_70px_-30px_rgba(0,0,0,0.95)]">
              <img
                src="https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=800&q=80"
                alt="S2000 Coaching baş antrenörü"
                className="h-[520px] w-full object-cover object-top"
                loading="eager"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-canvas via-canvas/20 to-transparent" />

              <div className="absolute left-4 top-4 flex items-center gap-2 rounded-full border border-border-strong bg-canvas/70 px-3 py-1.5 backdrop-blur-md">
                <span className="h-2 w-2 animate-pulse-slow rounded-full bg-primary" />
                <span className="text-[11px] font-semibold uppercase tracking-[0.18em] text-text-primary">
                  Canlı analiz sistemi
                </span>
              </div>

              <div className="edge-lit absolute inset-x-4 bottom-4 rounded-2xl bg-surface-elevated/90 p-4 backdrop-blur-xl">
                <span className="text-[11px] font-semibold uppercase tracking-[0.18em] text-primary">
                  Hazırlık seviyesi
                </span>
                <p className="font-display text-base font-bold uppercase text-text-primary">
                  Pro hipertrofi &amp; yağ yakımı
                </p>
              </div>
            </div>
          </div>
        </div>
      </div>
    </section>
  )
}

function MicroStat({ value, label, accent = false }: { value: string; label: string; accent?: boolean }) {
  return (
    <div className="flex flex-col bg-canvas px-4 py-4">
      <span className={`font-display text-2xl font-extrabold ${accent ? 'text-primary' : 'text-text-primary'}`}>
        {value}
      </span>
      <span className="text-[11px] font-semibold uppercase tracking-[0.14em] text-text-muted">{label}</span>
    </div>
  )
}
