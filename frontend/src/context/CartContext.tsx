import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

import type { CoachingPackage } from '@/types/package'

interface CartContextValue {
  selectedPackage: CoachingPackage | null
  isDrawerOpen: boolean
  isCheckoutOpen: boolean
  addToCart: (pkg: CoachingPackage) => void
  removeFromCart: () => void
  openDrawer: () => void
  closeDrawer: () => void
  openCheckout: () => void
  closeCheckout: () => void
}

const CartContext = createContext<CartContextValue | null>(null)

export function CartProvider({ children }: { children: ReactNode }) {
  const [selectedPackage, setSelectedPackage] = useState<CoachingPackage | null>(null)
  const [isDrawerOpen, setDrawerOpen] = useState(false)
  const [isCheckoutOpen, setCheckoutOpen] = useState(false)

  const value = useMemo<CartContextValue>(
    () => ({
      selectedPackage,
      isDrawerOpen,
      isCheckoutOpen,
      addToCart: (pkg) => {
        setSelectedPackage(pkg)
        setDrawerOpen(true)
      },
      removeFromCart: () => setSelectedPackage(null),
      openDrawer: () => setDrawerOpen(true),
      closeDrawer: () => setDrawerOpen(false),
      openCheckout: () => {
        setDrawerOpen(false)
        setCheckoutOpen(true)
      },
      closeCheckout: () => setCheckoutOpen(false),
    }),
    [selectedPackage, isDrawerOpen, isCheckoutOpen],
  )

  return <CartContext.Provider value={value}>{children}</CartContext.Provider>
}

export function useCart(): CartContextValue {
  const context = useContext(CartContext)
  if (!context) {
    throw new Error('useCart must be used within a CartProvider')
  }
  return context
}
