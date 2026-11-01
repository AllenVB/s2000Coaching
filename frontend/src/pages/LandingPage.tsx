import { useEffect } from 'react'
import { useLocation } from 'react-router-dom'

import { CartDrawer } from '@/components/cart/CartDrawer'
import { FreeAssessment } from '@/components/sections/FreeAssessment'
import { CheckoutModal } from '@/components/cart/CheckoutModal'
import { WhatsAppButton } from '@/components/layout/WhatsAppButton'
import { Footer } from '@/components/layout/Footer'
import { Header } from '@/components/layout/Header'
import { PaymentResultBanner } from '@/components/layout/PaymentResultBanner'
import { AppEcosystem } from '@/components/sections/AppEcosystem'
import { Faq } from '@/components/sections/Faq'
import { FinalCta } from '@/components/sections/FinalCta'
import { Hero } from '@/components/sections/Hero'
import { HowItWorks } from '@/components/sections/HowItWorks'
import { Pricing } from '@/components/sections/Pricing'
import { SuccessStories } from '@/components/sections/SuccessStories'
import { TrustStats } from '@/components/sections/TrustStats'
import { CartProvider } from '@/context/CartContext'
import { PricingCategoryProvider } from '@/context/PricingCategoryContext'
import { navLinks } from '@/data/content'
import { scrollToElementId } from '@/lib/scroll'

/** Public marketing site and checkout flow. */
export function LandingPage() {
  const { hash } = useLocation()

  // Arriving from another page with a section in the address ("/#paketler", from a button on the
  // calculators page) must land on that section; the browser only does so for a full page load.
  useEffect(() => {
    if (hash === '') {
      return
    }
    const targetId = navLinks.find((link) => link.href === hash)?.targetId ?? hash.slice(1)
    const frame = requestAnimationFrame(() => scrollToElementId(targetId))
    return () => cancelAnimationFrame(frame)
  }, [hash])

  return (
    <CartProvider>
      <PricingCategoryProvider>
        <Header />
        <PaymentResultBanner />
        <main>
          <Hero />
          <TrustStats />
          <HowItWorks />
          <AppEcosystem />
          <Pricing />
          <FreeAssessment />
          <SuccessStories />
          <Faq />
          <FinalCta />
        </main>
        <Footer />
        <WhatsAppButton />
        <CartDrawer />
        <CheckoutModal />
      </PricingCategoryProvider>
    </CartProvider>
  )
}
