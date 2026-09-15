import { Button } from '@/components/ui/Button'

export function Hero() {
  return (
    <section id="ana-sayfa" className="relative scroll-mt-24 overflow-hidden border-b border-border py-20 sm:py-24">
      <div
        className="pointer-events-none absolute -top-40 left-1/2 h-[450px] w-[1000px] -translate-x-1/2 rounded-full bg-accent/10 blur-[140px]"
        aria-hidden
      />
      <div
        className="pointer-events-none absolute right-0 top-1/3 h-[500px] w-[600px] rounded-full bg-primary/5 blur-[160px]"
        aria-hidden
      />

      <div className="relative mx-auto grid max-w-[1280px] grid-cols-1 items-center gap-12 px-4 sm:px-6 lg:grid-cols-12 lg:gap-16 lg:px-8">
        <div className="flex flex-col items-start lg:col-span-7">
          <div className="mb-6 inline-flex items-center gap-2 rounded-full border border-border bg-surface px-3 py-1.5">
            <span className="h-1.5 w-1.5 animate-pulse-slow rounded-full bg-primary" />
            <span className="text-xs font-semibold uppercase tracking-wider text-primary">
              Yeni Dönem Kayıtları Aktif
            </span>
            <span className="text-text-muted">•</span>
            <span className="text-xs font-bold text-text-secondary">Son 4 Kontenjan</span>
          </div>

          <h1 className="mb-6 max-w-[650px] text-balance font-display text-4xl font-extrabold uppercase leading-[1.1] tracking-tight text-text-primary sm:text-5xl lg:text-[56px]">
            Vücudunu Değil, <br className="hidden sm:inline" />
            <span className="bg-gradient-to-r from-accent via-primary to-primary bg-clip-text text-transparent">
              Hayatını Dönüştür.
            </span>
          </h1>

          <p className="mb-8 max-w-[550px] text-base leading-relaxed text-text-secondary sm:text-lg">
            Biyomekanik temelli kişiselleştirilmiş antrenman tasarımı, milimetrik beslenme stratejileri ve 7/24
            kesintisiz koç desteğiyle sınırlarını yeniden tanımla.
          </p>

          <div className="mb-10 flex w-full flex-wrap items-center gap-3 sm:w-auto">
            <Button size="lg" className="w-full sm:w-auto" onClick={() => (window.location.hash = '#paketler')}>
              Hemen Koçluğa Başla
            </Button>
            <Button
              variant="secondary"
              size="lg"
              className="w-full sm:w-auto"
              onClick={() => (window.location.hash = '#paketler')}
            >
              Paketleri İncele
            </Button>
          </div>

          <div className="grid w-full max-w-[520px] grid-cols-2 gap-6 border-t border-border pt-6 sm:grid-cols-4">
            <MicroStat value="500+" label="Aktif Sporcu" />
            <MicroStat value="4.9★" label="Sporcu Puanı" accent />
            <MicroStat value="%100" label="Kişiye Özel" />
            <MicroStat value="7/24" label="Koç İletişimi" />
          </div>
        </div>

        <div className="relative lg:col-span-5">
          <div className="relative mx-auto max-w-[440px] rounded-2xl bg-gradient-to-b from-accent/30 via-transparent to-primary/30 p-1 shadow-[0_20px_50px_rgba(0,0,0,0.8)]">
            <div className="relative overflow-hidden rounded-2xl bg-surface">
              <img
                src="https://images.unsplash.com/photo-1517836357463-d25dfeac3438?auto=format&fit=crop&w=800&q=80"
                alt="S2000 Coaching baş antrenörü"
                className="h-[500px] w-full object-cover object-top"
                loading="eager"
              />
              <div className="absolute inset-0 bg-gradient-to-t from-surface via-transparent to-transparent" />

              <div className="absolute left-4 top-4 flex items-center gap-2 rounded-full bg-surface/80 px-3 py-1.5 backdrop-blur-md">
                <span className="h-2 w-2 animate-pulse-slow rounded-full bg-primary" />
                <span className="text-xs font-semibold uppercase tracking-wider text-text-primary">
                  Canlı Analiz Sistemi
                </span>
              </div>

              <div className="absolute inset-x-4 bottom-4 flex items-center justify-between rounded-xl bg-surface-elevated/90 p-4 backdrop-blur-xl">
                <div>
                  <span className="text-xs font-semibold uppercase tracking-wider text-primary">
                    Hazırlık Seviyesi
                  </span>
                  <p className="font-display text-sm font-bold uppercase text-text-primary">
                    Pro Hipertrofi & Yağ Yakımı
                  </p>
                </div>
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
    <div className="flex flex-col">
      <span className={`font-display text-xl font-extrabold ${accent ? 'text-primary' : 'text-text-primary'}`}>
        {value}
      </span>
      <span className="text-[11px] font-semibold uppercase tracking-wider text-text-secondary">{label}</span>
    </div>
  )
}
