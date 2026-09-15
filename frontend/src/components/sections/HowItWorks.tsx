import { SectionHeading } from '@/components/ui/SectionHeading'
import { howItWorksSteps } from '@/data/content'

export function HowItWorks() {
  return (
    <section id="nasil-calisir" className="border-b border-border py-20 sm:py-24">
      <div className="mx-auto max-w-[1280px] px-4 sm:px-6 lg:px-8">
        <div className="mb-16">
          <SectionHeading
            align="left"
            eyebrow="Sistematik Süreç"
            title="Koçluk Süreci Nasıl İlerliyor?"
            description="Bilimsel metodoloji ve kesintisiz dijital takip ile 4 adımda hedefe odaklanın."
          />
        </div>

        <div className="grid grid-cols-1 gap-6 sm:grid-cols-2 lg:grid-cols-4">
          {howItWorksSteps.map((item) => (
            <div
              key={item.step}
              className="flex flex-col justify-between rounded-2xl border border-border bg-surface p-8"
            >
              <div>
                <div className="mb-6 font-display text-2xl font-black tracking-tight text-primary">{item.step}</div>
                <h3 className="mb-2 text-lg font-bold text-text-primary">{item.title}</h3>
                <p className="text-sm leading-relaxed text-text-secondary">{item.description}</p>
              </div>
              <div className="mt-6 flex items-center justify-between border-t border-border pt-4 text-xs font-medium text-text-muted">
                <span>{item.meta[0]}</span>
                <span className="text-text-secondary">{item.meta[1]}</span>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  )
}
