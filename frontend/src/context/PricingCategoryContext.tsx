import { createContext, useContext, useEffect, useState, type ReactNode } from 'react'

import type { PackageCategory } from '@/types/package'

interface PricingCategoryContextValue {
  category: PackageCategory
  setCategory: (category: PackageCategory) => void
}

const PricingCategoryContext = createContext<PricingCategoryContextValue | null>(null)

function categoryFromHash(hash: string): PackageCategory {
  return hash === '#paketler-beslenme' ? 'BESLENME' : 'KOCLUK'
}

export function PricingCategoryProvider({ children }: { children: ReactNode }) {
  const [category, setCategory] = useState<PackageCategory>(() =>
    typeof window === 'undefined' ? 'KOCLUK' : categoryFromHash(window.location.hash),
  )

  useEffect(() => {
    const onHashChange = () => setCategory(categoryFromHash(window.location.hash))
    window.addEventListener('hashchange', onHashChange)
    return () => window.removeEventListener('hashchange', onHashChange)
  }, [])

  return (
    <PricingCategoryContext.Provider value={{ category, setCategory }}>{children}</PricingCategoryContext.Provider>
  )
}

export function usePricingCategory(): PricingCategoryContextValue {
  const context = useContext(PricingCategoryContext)
  if (!context) {
    throw new Error('usePricingCategory must be used within a PricingCategoryProvider')
  }
  return context
}
