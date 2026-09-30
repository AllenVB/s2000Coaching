export interface ExperienceNote {
  id: string
  content: string
}

export interface ExperienceTopic {
  id: string
  name: string
  notes: ExperienceNote[]
}

export interface ExperienceCategory {
  id: string
  name: string
  topics: ExperienceTopic[]
}
