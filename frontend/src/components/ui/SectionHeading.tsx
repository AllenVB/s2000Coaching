interface SectionHeadingProps {
  eyebrow: string
  title: React.ReactNode
  description?: string
  align?: 'left' | 'center'
}

export function SectionHeading({ eyebrow, title, description, align = 'center' }: SectionHeadingProps) {
  const alignClasses = align === 'center' ? 'items-center text-center mx-auto' : 'items-start text-left'

  return (
    <div className={`flex max-w-2xl flex-col gap-4 ${alignClasses}`}>
      {/* The rule beside the eyebrow is the hero's mark repeated: every section opens the
          same way, so the page has a rhythm a reader can feel without naming it. */}
      <span className="flex items-center gap-3 text-[11px] font-bold uppercase tracking-[0.28em] text-primary">
        <span className="h-px w-8 bg-primary" aria-hidden />
        {eyebrow}
      </span>
      <h2 className="text-balance font-display text-[clamp(1.9rem,4vw,2.9rem)] font-extrabold uppercase leading-[0.98] text-text-primary">
        {title}
      </h2>
      {description ? <p className="max-w-xl text-base leading-relaxed text-text-secondary">{description}</p> : null}
    </div>
  )
}
