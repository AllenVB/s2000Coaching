export interface ExperienceItem {
  id: string
  title: string
  description: string
  updatedAt: string
}

export interface ExperienceCategory {
  id: string
  name: string
  items: ExperienceItem[]
}
