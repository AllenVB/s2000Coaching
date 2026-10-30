/** Each link is a ready-to-use address, or null when the coach has not set it. */
export interface SiteSocial {
  instagram: string | null
  youtube: string | null
  whatsapp: string | null
}

export interface SiteStat {
  label: string
  value: string
  caption: string
  footnote: string
  accent: boolean
}

export interface LegalSummary {
  slug: string
  title: string
  /** Whether the coach has written the text yet. */
  published: boolean
}

/**
 * What the coach has set about the public site. `stats` is null when none were set (the site then
 * shows its built-in figures) and an empty list when the coach chose to show none.
 */
export interface SiteSettings {
  social: SiteSocial
  stats: SiteStat[] | null
  legal: LegalSummary[]
}

export interface LegalDocument {
  slug: string
  title: string
  /** Null until the coach has written it. */
  content: string | null
  updatedAt: string | null
}

export interface AdminSiteSettings {
  social: SiteSocial
  stats: SiteStat[] | null
  legal: LegalDocument[]
}
