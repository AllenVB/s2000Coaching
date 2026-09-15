import { SectionHeading } from '@/components/ui/SectionHeading'
import { transformations } from '@/data/content'

export function Transformations() {
  return (
    <section id="basari-hikayeleri" className="scroll-mt-24 border-b border-border bg-surface/40 py-20 sm:py-24">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        <div className="mb-16 flex flex-col gap-4 md:flex-row md:items-end md:justify-between">
          <SectionHeading
            align="left"
            eyebrow="Doğrulanmış Sonuçlar"
            title="Değişimi Onlar Yaşadı."
          />
          <p className="max-w-md text-sm text-text-secondary">
            Sadece tartıdaki rakamlar değil; duruş, özgüven ve yaşam tarzının yeniden inşası.
          </p>
        </div>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
          {transformations.map((item) => (
            <div key={item.name} className="flex flex-col overflow-hidden rounded-2xl border border-border bg-surface">
              <div className="relative aspect-[4/5] overflow-hidden">
                <img src={item.image} alt={`${item.name} dönüşümü`} className="h-full w-full object-cover" loading="lazy" />
                <div className="absolute bottom-3 left-3 rounded-md bg-canvas/90 px-2.5 py-1 text-[11px] font-semibold text-text-primary">
                  {item.duration}
                </div>
              </div>
              <div className="flex flex-1 flex-col justify-between p-6">
                <div>
                  <div className="mb-2 flex items-center justify-between">
                    <h3 className="text-base font-bold text-text-primary">{item.name}</h3>
                    <span className="text-xs text-text-muted">{item.role}</span>
                  </div>
                  <p className="mb-6 text-xs leading-relaxed text-text-secondary">&ldquo;{item.quote}&rdquo;</p>
                </div>
                <div className="grid grid-cols-2 gap-3 border-t border-border pt-3 text-center">
                  {item.stats.map((stat) => (
                    <div key={stat.label} className="rounded-lg border border-border bg-surface-elevated p-2.5">
                      <div className="text-sm font-bold text-primary">{stat.value}</div>
                      <div className="mt-0.5 text-[11px] text-text-muted">{stat.label}</div>
                    </div>
                  ))}
                </div>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
