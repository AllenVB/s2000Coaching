export type MemberStatus = 'PENDING_VERIFICATION' | 'ACTIVE'

export interface Member {
  id: string
  email: string
  fullName: string
  phone: string
  heightCm: number
  weightKg: number
  sportsBackground: string
  status: MemberStatus
  createdAt: string
  verifiedAt: string | null
  /** When the member accepted health-data processing; null until they do. */
  healthDataConsentAt: string | null
}

export interface CreateMemberInput {
  email: string
  fullName: string
  phone: string
  heightCm: number
  weightKg: number
  sportsBackground: string
}

export interface MemberSession {
  token: string
  expiresAt: string
  member: Member
}
