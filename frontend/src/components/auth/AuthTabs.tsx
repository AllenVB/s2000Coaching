export type AuthTab = 'login' | 'register'

const LABELS: Record<AuthTab, string> = {
  login: 'Giriş Yap',
  register: 'Üye Ol',
}

/**
 * The switch at the top of the sign-in page: "Giriş Yap" (members and the coach alike) or
 * "Üye Ol". There is no separate management entrance; the server tells the two apart.
 */
export function AuthTabs({
  active,
  onChange,
  tabs = ['login', 'register'],
}: {
  active: AuthTab
  onChange: (tab: AuthTab) => void
  tabs?: AuthTab[]
}) {
  return (
    <div
      className="mb-6 grid rounded-full border border-border bg-canvas p-1 text-[11px] font-semibold uppercase tracking-wide sm:text-xs"
      style={{ gridTemplateColumns: `repeat(${tabs.length}, minmax(0, 1fr))` }}
    >
      {tabs.map((tab) => (
        <button
          key={tab}
          type="button"
          onClick={() => onChange(tab)}
          aria-pressed={active === tab}
          className={`rounded-full px-2 py-2.5 transition-colors ${
            active === tab ? 'bg-primary text-canvas' : 'text-text-secondary hover:text-text-primary'
          }`}
        >
          {LABELS[tab]}
        </button>
      ))}
    </div>
  )
}
