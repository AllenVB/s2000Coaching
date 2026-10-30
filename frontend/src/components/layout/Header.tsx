import { useState, type MouseEvent } from 'react'
import { Link } from 'react-router-dom'

import { useCart } from '@/context/CartContext'
import { usePricingCategory } from '@/context/PricingCategoryContext'
import { navLinks, type NavLink } from '@/data/content'
import { Button } from '@/components/ui/Button'
import { CartIcon, CloseIcon, MenuIcon } from '@/components/ui/icons'
import { readCoachSession } from '@/lib/adminSession'
import { readMemberSession } from '@/lib/memberSession'
import { isPlainLeftClick, navigateToSection } from '@/lib/scroll'

/**
 * The one way into the member and management areas. Whoever is signed in already sees where
 * their session leads instead of a sign-in button.
 */
function useAccountLink(): { label: string; to: string } {
  if (readCoachSession()) {
    return { label: 'Yönetim Paneli', to: '/yonetim' }
  }
  if (readMemberSession()) {
    return { label: 'Hesabım', to: '/uye' }
  }
  return { label: 'Giriş Yap', to: '/giris' }
}

export function Header() {
  const [isMobileMenuOpen, setMobileMenuOpen] = useState(false)
  const { selectedPackage, openDrawer } = useCart()
  const { setCategory } = usePricingCategory()
  const cartCount = selectedPackage ? 1 : 0
  const accountLink = useAccountLink()

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

        {/* The full bar needs xl: nine destinations beside the logo, the cart and the
            call to action overflow 1024px, and at md they wrapped onto two lines. Below
            that the same links live in the menu. */}
        <nav className="hidden items-center gap-6 xl:flex">
          {navLinks.map((link) => (
            <a
              key={link.label}
              href={link.href}
              onClick={(event) => handleNavLinkClick(event, link)}
              className="relative whitespace-nowrap py-2 text-sm font-medium text-text-secondary transition-colors after:absolute after:inset-x-0 after:bottom-0 after:h-px after:origin-left after:scale-x-0 after:bg-primary after:transition-transform hover:text-text-primary hover:after:scale-x-100"
            >
              {link.label}
            </a>
          ))}
          {/* Pages of their own (the calculators, the sign-in) are different from the anchors on
              this page, so they sit behind a divider instead of among them. */}
          <span className="h-5 w-px bg-border-strong" aria-hidden />
          <Link
            to="/hesaplayicilar"
            className="whitespace-nowrap text-sm font-medium text-text-secondary transition-colors hover:text-text-primary"
          >
            Hesaplayıcılar
          </Link>
          <Link
            to={accountLink.to}
            className="whitespace-nowrap text-sm font-semibold text-text-primary transition-colors hover:text-primary"
          >
            {accountLink.label}
          </Link>
        </nav>

        <div className="flex items-center gap-3">
          <button
            aria-label="Sepeti Görüntüle"
            onClick={openDrawer}
            className="relative flex h-11 w-11 items-center justify-center rounded-xl border border-border bg-surface text-text-secondary transition-colors hover:border-primary/50 hover:text-text-primary"
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
            className="hidden whitespace-nowrap sm:inline-flex"
            onClick={() => navigateToSection('#paketler', 'paketler')}
          >
            Koçluğa Başla
          </Button>

          <button
            aria-label="Menü"
            onClick={() => setMobileMenuOpen((open) => !open)}
            className="flex h-11 w-11 items-center justify-center rounded-xl border border-border bg-surface text-text-secondary hover:text-text-primary xl:hidden"
          >
            {isMobileMenuOpen ? <CloseIcon className="h-5 w-5" /> : <MenuIcon className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {isMobileMenuOpen ? (
        <div className="space-y-4 border-b border-border bg-surface px-4 py-6 xl:hidden">
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
            <Link
              to="/hesaplayicilar"
              onClick={() => setMobileMenuOpen(false)}
              className="py-1 text-sm font-medium text-text-secondary hover:text-text-primary"
            >
              Hesaplayıcılar
            </Link>
            <Link
              to={accountLink.to}
              onClick={() => setMobileMenuOpen(false)}
              className="py-1 text-sm font-medium text-primary hover:text-primary-hover"
            >
              {accountLink.label}
            </Link>
          </div>
          <a
            href="#paketler"
            onClick={(event) => {
              handleCtaClick(event)
              setMobileMenuOpen(false)
            }}
            className="flex h-11 w-full items-center justify-center rounded-full bg-primary text-sm font-semibold uppercase tracking-wide text-canvas transition-colors hover:bg-primary-hover"
          >
            Koçluğa Başla
          </a>
        </div>
      ) : null}
    </header>
  )
}
