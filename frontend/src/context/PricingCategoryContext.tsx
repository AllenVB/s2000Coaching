import { createContext, useContext, useMemo, useState, type ReactNode } from 'react'

import type { PackageCategory } from '@/types/package'

interface PricingCategoryContextValue {
  category: PackageCategory
  setCategory: (category: PackageCategory) => void
}

const PricingCategoryContext = createContext<PricingCategoryContextValue | null>(null)

export function PricingCategoryProvider({ children }: { children: ReactNode }) {
  const [category, setCategory] = useState<PackageCategory>('KOCLUK')

  const value = useMemo<PricingCategoryContextValue>(() => ({ category, setCategory }), [category])

  return <PricingCategoryContext.Provider value={value}>{children}</PricingCategoryContext.Provider>
}

export function usePricingCategory(): PricingCategoryContextValue {
  const context = useContext(PricingCategoryContext)
  if (!context) {
    throw new Error('usePricingCategory must be used within a PricingCategoryProvider')
  }
  return context
}
