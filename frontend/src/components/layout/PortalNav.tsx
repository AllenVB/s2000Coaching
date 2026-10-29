import { NavLink } from 'react-router-dom'

import type { ComponentType } from 'react'

export interface PortalNavItem {
  to: string
  label: string
  icon: ComponentType<{ className?: string }>
  /** Only the index route should match on its exact path; the rest match their subtree. */
  end?: boolean
  /**
   * Kept out of the phone tab bar. The bar divides the full width between its items, so
   * past about six they stop being thumb-sized; a section reachable from a card on the
   * page it belongs to is set this way rather than squeezing the bar further.
   */
  desktopOnly?: boolean
  /** Unread count shown next to the label; 0 or undefined shows nothing. */
  badge?: number
}

/** Small count next to a nav label — unread messages, today. */
function NavBadge({ count, className = '' }: { count: number; className?: string }) {
  return (
    <span
      aria-label={`${count} okunmamış`}
      className={`inline-flex min-w-[18px] items-center justify-center rounded-full bg-primary px-1.5 text-[10px] font-bold leading-[18px] text-canvas ${className}`}
    >
      {count > 9 ? '9+' : count}
    </span>
  )
}

const baseClasses =
  'flex items-center gap-2.5 rounded-xl px-3.5 py-2.5 text-sm font-semibold transition-colors'

/** Vertical navigation used on desktop by both portals. */
export function PortalSidebarNav({ items }: { items: PortalNavItem[] }) {
  return (
    <nav className="flex flex-col gap-1">
      {items.map(({ to, label, icon: Icon, end, badge }) => (
        <NavLink
          key={to}
          to={to}
          end={end}
          className={({ isActive }) =>
            `${baseClasses} ${
              isActive
                ? 'bg-primary text-canvas shadow-[0_0_20px_rgba(199,240,0,0.18)]'
                : 'text-text-secondary hover:bg-white/5 hover:text-text-primary'
            }`
          }
        >
          <Icon className="h-4 w-4 shrink-0" />
          <span className="flex-1 truncate">{label}</span>
          {badge ? <NavBadge count={badge} /> : null}
        </NavLink>
      ))}
    </nav>
  )
}

/**
 * Bottom tab bar for phones. Members open this in the gym, so the targets are full-height
 * and the labels stay visible rather than collapsing to icons.
 */
export function PortalTabBar({ items }: { items: PortalNavItem[] }) {
  return (
    <nav className="fixed inset-x-0 bottom-0 z-40 border-t border-border bg-canvas/95 backdrop-blur-md lg:hidden print:hidden">
      <div className="mx-auto flex max-w-[640px]">
        {items.filter((item) => !item.desktopOnly).map(({ to, label, icon: Icon, end, badge }) => (
          <NavLink
            key={to}
            to={to}
            end={end}
            className={({ isActive }) =>
              `flex min-w-0 flex-1 flex-col items-center gap-1 px-1 py-2.5 text-[9px] font-semibold uppercase tracking-tight transition-colors sm:text-[10px] sm:tracking-wide ${
                isActive ? 'text-primary' : 'text-text-muted hover:text-text-secondary'
              }`
            }
          >
            <span className="relative">
              <Icon className="h-5 w-5 shrink-0" />
              {badge ? <NavBadge count={badge} className="absolute -right-2.5 -top-1.5" /> : null}
            </span>
            <span className="w-full truncate text-center">{label}</span>
          </NavLink>
        ))}
      </div>
    </nav>
  )
}
