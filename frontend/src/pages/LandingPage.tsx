import { CartDrawer } from '@/components/cart/CartDrawer'
import { CheckoutModal } from '@/components/cart/CheckoutModal'
import { Footer } from '@/components/layout/Footer'
import { Header } from '@/components/layout/Header'
import { AppEcosystem } from '@/components/sections/AppEcosystem'
import { Experience } from '@/components/sections/Experience'
import { Faq } from '@/components/sections/Faq'
import { FinalCta } from '@/components/sections/FinalCta'
import { Hero } from '@/components/sections/Hero'
import { HowItWorks } from '@/components/sections/HowItWorks'
import { Pricing } from '@/components/sections/Pricing'
import { SuccessStories } from '@/components/sections/SuccessStories'
import { TrustStats } from '@/components/sections/TrustStats'
import { CartProvider } from '@/context/CartContext'
import { PricingCategoryProvider } from '@/context/PricingCategoryContext'

/** Public marketing site and checkout flow. */
export function LandingPage() {
  return (
    <CartProvider>
      <PricingCategoryProvider>
        <Header />
        <main>
          <Hero />
          <TrustStats />
          <HowItWorks />
          <AppEcosystem />
          <Pricing />
          <SuccessStories />
          <Experience />
          <Faq />
          <FinalCta />
        </main>
        <Footer />
        <CartDrawer />
        <CheckoutModal />
      </PricingCategoryProvider>
    </CartProvider>
  )
}
