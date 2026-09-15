import { trustStats } from '@/data/content'

export function TrustStats() {
  return (
    <section className="border-b border-border bg-surface/40 py-16 sm:py-20">
      <div className="mx-auto grid max-w-[1280px] grid-cols-1 gap-6 px-4 sm:grid-cols-2 sm:px-6 lg:grid-cols-4 lg:px-8">
        {trustStats.map((stat) => (
          <div
            key={stat.label}
            className="flex flex-col justify-between rounded-2xl border border-border bg-surface p-6"
          >
            <div className="mb-4 text-xs font-semibold uppercase tracking-wider text-text-muted">{stat.label}</div>
            <div>
              <div
                className={`font-display text-3xl font-extrabold tracking-tight lg:text-4xl ${
                  stat.accent ? 'text-primary' : 'text-text-primary'
                }`}
              >
                {stat.value}
              </div>
              <div className="mt-1 text-sm font-medium text-text-secondary">{stat.caption}</div>
            </div>
            <p className="mt-4 border-t border-border pt-4 text-xs text-text-muted">{stat.footnote}</p>
          </div>
        ))}
      </div>
    </section>
  )
}
