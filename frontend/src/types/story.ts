export type StoryStatus = 'DRAFT' | 'AWAITING_CONSENT' | 'CONSENTED' | 'PUBLISHED' | 'DECLINED' | 'REVOKED'

export const STORY_STATUS: Record<StoryStatus, { label: string; tone: 'success' | 'warning' | 'error' | 'muted' }> = {
  DRAFT: { label: 'Taslak', tone: 'muted' },
  AWAITING_CONSENT: { label: 'Üye onayı bekleniyor', tone: 'warning' },
  CONSENTED: { label: 'Üye onayladı — yayına hazır', tone: 'success' },
  PUBLISHED: { label: 'Yayında', tone: 'success' },
  DECLINED: { label: 'Üye reddetti', tone: 'error' },
  REVOKED: { label: 'Üye onayı geri çekti', tone: 'error' },
}

export interface StoryPhoto {
  id: string
  caption: string
  takenOn: string
}

/** A story as the coach and the member see it; `memberName` is only set for the coach. */
export interface Story {
  id: string
  memberId: string
  memberName: string | null
  title: string
  quote: string | null
  body: string
  displayName: string
  status: StoryStatus
  consentAt: string | null
  publishedAt: string | null
  updatedAt: string
  photos: StoryPhoto[]
  /** The wording the member agrees to; only present while they are being asked. */
  consentText: string | null
}

/** What the public site gets: nothing identifying beyond the display name the member agreed to. */
export interface PublicStory {
  id: string
  title: string
  quote: string | null
  body: string
  displayName: string
  publishedAt: string
  photos: StoryPhoto[]
}

export interface CreateStoryInput {
  memberId: string
  title: string
  quote?: string | null
  body: string
  displayName: string
  photos: { progressPhotoId: string; caption: string }[]
}

export interface UpdateStoryInput {
  title: string
  quote?: string | null
  body: string
  displayName: string
}
