export interface Coach {
  id: string
  email: string
  fullName: string
  lastLoginAt: string | null
}

export interface CoachSession {
  token: string
  expiresAt: string
  coach: Coach
}
