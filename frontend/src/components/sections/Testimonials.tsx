import { SectionHeading } from '@/components/ui/SectionHeading'
import { StarIcon } from '@/components/ui/icons'
import { testimonials } from '@/data/content'

export function Testimonials() {
  return (
    <section className="py-20 sm:py-24">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        <div className="mb-16 mx-auto">
          <SectionHeading eyebrow="Gerçek Deneyimler" title="Öğrencilerimiz Ne Diyor?" />
        </div>

        <div className="grid grid-cols-1 gap-6 md:grid-cols-3">
          {testimonials.map((item) => (
            <div key={item.name} className="flex flex-col justify-between rounded-2xl border border-border bg-surface p-6">
              <div>
                <div className="mb-3 flex gap-0.5 text-primary">
                  {Array.from({ length: 5 }).map((_, i) => (
                    <StarIcon key={i} className="h-3.5 w-3.5" />
                  ))}
                </div>
                <p className="mb-6 text-xs leading-relaxed text-text-secondary">&ldquo;{item.quote}&rdquo;</p>
              </div>
              <div className="flex items-center justify-between border-t border-border pt-4">
                <div>
                  <div className="text-sm font-bold text-text-primary">{item.name}</div>
                  <div className="text-[11px] text-text-muted">{item.tag}</div>
                </div>
                <span className="rounded bg-success/10 px-2 py-0.5 text-[11px] font-semibold text-success">
                  Doğrulandı
                </span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
