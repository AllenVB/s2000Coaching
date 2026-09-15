import type { MouseEvent } from 'react'

export function isPlainLeftClick(event: MouseEvent): boolean {
  return event.button === 0 && !event.metaKey && !event.ctrlKey && !event.shiftKey && !event.altKey
}

function prefersReducedMotion(): boolean {
  return typeof window !== 'undefined' && window.matchMedia('(prefers-reduced-motion: reduce)').matches
}

export function scrollToElementId(id: string) {
  document.getElementById(id)?.scrollIntoView({
    behavior: prefersReducedMotion() ? 'auto' : 'smooth',
    block: 'start',
  })
}

export function setSectionHash(hash: string) {
  if (typeof history !== 'undefined' && history.pushState) {
    history.pushState(null, '', hash)
  }
}

/**
 * Navigates to an in-page section without relying on the browser's default
 * anchor-click scrolling, which silently no-ops when `hash` already equals
 * `location.hash` (e.g. switching between two nav links that share a target).
 *
 * Deferred one frame so a state update fired just before this call (e.g.
 * switching the Pricing tab) has a chance to flush before we scroll,
 * avoiding a flash of the previous tab's content mid-scroll.
 */
export function navigateToSection(hash: string, targetId: string) {
  setSectionHash(hash)
  requestAnimationFrame(() => scrollToElementId(targetId))
}
