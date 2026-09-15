interface SectionHeadingProps {
  eyebrow: string
  title: React.ReactNode
  description?: string
  align?: 'left' | 'center'
}

export function SectionHeading({ eyebrow, title, description, align = 'center' }: SectionHeadingProps) {
  const alignClasses = align === 'center' ? 'items-center text-center mx-auto' : 'items-start text-left'

  return (
    <div className={`flex max-w-2xl flex-col gap-3 ${alignClasses}`}>
      <span className="font-display text-xs font-bold uppercase tracking-[0.2em] text-primary">{eyebrow}</span>
      <h2 className="text-balance font-display text-3xl font-extrabold uppercase tracking-tight text-text-primary sm:text-4xl">
        {title}
      </h2>
      {description ? <p className="text-base text-text-secondary">{description}</p> : null}
    </div>
  )
}
