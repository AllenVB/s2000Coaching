export type ExperienceCategory = 'ANTRENMAN' | 'BESLENME' | 'KARDIYO'

export interface ExperienceNote {
  category: ExperienceCategory
  content: string
  updatedAt: string
}
