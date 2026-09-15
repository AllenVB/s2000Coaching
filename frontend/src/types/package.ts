export type PackageCategory = 'KOCLUK' | 'BESLENME'

export interface PackageFeature {
  label: string
}

export interface CoachingPackage {
  id: string
  slug: string
  category: PackageCategory
  name: string
  tagline: string | null
  durationMonths: number
  priceMinor: number
  originalPriceMinor: number
  badge: string | null
  featured: boolean
  features: PackageFeature[]
}
