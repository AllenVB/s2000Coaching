import { useState, type MouseEvent } from 'react'

import { useCart } from '@/context/CartContext'
import { usePricingCategory } from '@/context/PricingCategoryContext'
import { navLinks, type NavLink } from '@/data/content'
import { Button } from '@/components/ui/Button'
import { CartIcon, CloseIcon, MenuIcon } from '@/components/ui/icons'
import { isPlainLeftClick, navigateToSection } from '@/lib/scroll'

export function Header() {
  const [isMobileMenuOpen, setMobileMenuOpen] = useState(false)
  const { selectedPackage, openDrawer } = useCart()
  const { setCategory } = usePricingCategory()
  const cartCount = selectedPackage ? 1 : 0

  const handleNavLinkClick = (event: MouseEvent<HTMLAnchorElement>, link: NavLink) => {
    if (!isPlainLeftClick(event)) {
      return
    }
    event.preventDefault()
    if (link.category) {
      setCategory(link.category)
    }
    navigateToSection(link.href, link.targetId)
  }

  const handleCtaClick = (event: MouseEvent<HTMLAnchorElement>) => {
    if (!isPlainLeftClick(event)) {
      return
    }
    event.preventDefault()
    navigateToSection('#paketler', 'paketler')
  }

  return (
    <header className="sticky top-0 z-40 w-full border-b border-border bg-canvas/90 backdrop-blur-md">
      <div className="mx-auto flex h-[72px] max-w-[1280px] items-center justify-between gap-4 px-4 sm:px-6 lg:px-8">
        <a
          href="#ana-sayfa"
          onClick={(event) => {
            if (!isPlainLeftClick(event)) return
            event.preventDefault()
            navigateToSection('#ana-sayfa', 'ana-sayfa')
          }}
          className="flex shrink-0 items-center gap-3 rounded-md py-1"
        >
          <div className="flex h-9 w-9 items-center justify-center rounded-lg border border-border bg-surface font-display text-sm font-black tracking-wider text-primary">
            FC
          </div>
          <div className="flex flex-col leading-none">
            <span className="font-display text-base font-bold uppercase tracking-wider text-text-primary">
              S2000
            </span>
            <span className="mt-0.5 text-[10px] font-semibold uppercase tracking-[0.22em] text-primary">
              Coaching
            </span>
          </div>
        </a>

        <nav className="hidden items-center gap-7 md:flex">
          {navLinks.map((link) => (
            <a
              key={link.label}
              href={link.href}
              onClick={(event) => handleNavLinkClick(event, link)}
              className="text-sm font-medium text-text-secondary transition-colors hover:text-text-primary"
            >
              {link.label}
            </a>
          ))}
        </nav>

        <div className="flex items-center gap-3">
          <button
            aria-label="Sepeti Görüntüle"
            onClick={openDrawer}
            className="relative flex h-11 w-11 items-center justify-center rounded-lg border border-border bg-surface text-text-secondary transition-colors hover:border-white/25 hover:text-text-primary"
          >
            <CartIcon className="h-5 w-5" />
            {cartCount > 0 ? (
              <span className="absolute -right-1.5 -top-1.5 flex h-5 min-w-[20px] items-center justify-center rounded-full bg-primary px-1 text-[11px] font-bold leading-none text-canvas">
                {cartCount}
              </span>
            ) : null}
          </button>

          <Button
            size="md"
            className="hidden sm:inline-flex"
            onClick={() => navigateToSection('#paketler', 'paketler')}
          >
            Koçluğa Başla
          </Button>

          <button
            aria-label="Menü"
            onClick={() => setMobileMenuOpen((open) => !open)}
            className="flex h-11 w-11 items-center justify-center rounded-lg border border-border bg-surface text-text-secondary hover:text-text-primary md:hidden"
          >
            {isMobileMenuOpen ? <CloseIcon className="h-5 w-5" /> : <MenuIcon className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {isMobileMenuOpen ? (
        <div className="space-y-4 border-b border-border bg-surface px-4 py-6 md:hidden">
          <div className="flex flex-col space-y-3">
            {navLinks.map((link) => (
              <a
                key={link.label}
                href={link.href}
                onClick={(event) => {
                  handleNavLinkClick(event, link)
                  setMobileMenuOpen(false)
                }}
                className="py-1 text-sm font-medium text-text-secondary hover:text-text-primary"
              >
                {link.label}
              </a>
            ))}
          </div>
          <a
            href="#paketler"
            onClick={(event) => {
              handleCtaClick(event)
              setMobileMenuOpen(false)
            }}
            className="flex h-11 w-full items-center justify-center rounded-xl bg-primary text-sm font-semibold uppercase tracking-wide text-canvas transition-colors hover:bg-primary-hover"
          >
            Koçluğa Başla
          </a>
        </div>
      ) : null}
    </header>
  )
}
