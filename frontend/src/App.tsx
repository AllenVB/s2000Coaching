import { CartDrawer } from '@/components/cart/CartDrawer'
import { CheckoutModal } from '@/components/cart/CheckoutModal'
import { Footer } from '@/components/layout/Footer'
import { Header } from '@/components/layout/Header'
import { AppEcosystem } from '@/components/sections/AppEcosystem'
import { Faq } from '@/components/sections/Faq'
import { FinalCta } from '@/components/sections/FinalCta'
import { Hero } from '@/components/sections/Hero'
import { HowItWorks } from '@/components/sections/HowItWorks'
import { Pricing } from '@/components/sections/Pricing'
import { Testimonials } from '@/components/sections/Testimonials'
import { Transformations } from '@/components/sections/Transformations'
import { TrustStats } from '@/components/sections/TrustStats'
import { CartProvider } from '@/context/CartContext'

export default function App() {
  return (
    <CartProvider>
      <Header />
      <main>
        <Hero />
        <TrustStats />
        <HowItWorks />
        <AppEcosystem />
        <Pricing />
        <Transformations />
        <Testimonials />
        <Faq />
        <FinalCta />
      </main>
      <Footer />
      <CartDrawer />
      <CheckoutModal />
    </CartProvider>
  )
}
