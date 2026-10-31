/** Pill tabs for switching between two or three views of one page (Antrenman / Beslenme …). */
export function SectionTabs<T extends string>({
  tabs,
  value,
  onChange,
  label,
}: {
  tabs: { value: T; label: string }[]
  value: T
  onChange: (next: T) => void
  label: string
}) {
  return (
    <div
      role="tablist"
      aria-label={label}
      className="mb-6 grid rounded-full border border-border bg-canvas p-1 text-xs font-semibold uppercase tracking-wide"
      style={{ gridTemplateColumns: `repeat(${tabs.length}, minmax(0, 1fr))` }}
    >
      {tabs.map((tab) => (
        <button
          key={tab.value}
          type="button"
          role="tab"
          aria-selected={value === tab.value}
          onClick={() => onChange(tab.value)}
          className={`rounded-full px-3 py-2.5 transition-colors ${
            value === tab.value ? 'bg-primary text-canvas' : 'text-text-secondary hover:text-text-primary'
          }`}
        >
          {tab.label}
        </button>
      ))}
    </div>
  )
}

/** Which tab the address asks for (`?sekme=…`), falling back to the first. */
export function tabFromParam<T extends string>(param: string | null, tabs: { value: T }[]): T {
  return tabs.find((tab) => tab.value === param)?.value ?? tabs[0].value
}
