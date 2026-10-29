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
  /** Whether reminder e-mails (renewal, weekly check-in, measurement) are wanted. */
  remindersEnabled: boolean
}

/**
 * What happened to a mailed code. SENT means the mail server took it, not that it reached an
 * inbox. NOT_CONFIGURED is the development fallback, where the code is only in the server log.
 */
export type MailDelivery = 'SENT' | 'NOT_CONFIGURED' | 'FAILED'

/** The member exists whatever `inviteDelivery` says; a failed mail is fixed by resending. */
export interface CreateMemberResponse {
  member: Member
  inviteDelivery: MailDelivery
}

export interface ResendCodeResponse {
  delivery: MailDelivery
}

export interface MailTestResponse {
  delivery: MailDelivery
  to: string
  /** The mail server's own reason when delivery is FAILED. */
  detail: string | null
}

export interface CreateMemberInput {
  email: string
  fullName: string
  phone: string
  heightCm: number
  weightKg: number
  sportsBackground: string
}

/**
 * What a member may change about themselves — narrower than `CreateMemberInput`, which is
 * the coach's form. E-mail identifies the account, and the starting weight is the baseline
 * the progress charts are drawn against, so neither is editable here.
 */
export interface UpdateMyProfileBody {
  fullName: string
  phone: string
  heightCm: number
  sportsBackground: string
}

export interface MemberSession {
  token: string
  expiresAt: string
  member: Member
}

export type PhotoPose = 'ON' | 'YAN' | 'ARKA'

export const PHOTO_POSES: { value: PhotoPose; label: string }[] = [
  { value: 'ON', label: 'Ön' },
  { value: 'YAN', label: 'Yan' },
  { value: 'ARKA', label: 'Arka' },
]

export interface ProgressPhoto {
  id: string
  /** ISO date (yyyy-mm-dd) — the day the photo was taken, which is what comparisons order by. */
  takenOn: string
  pose: PhotoPose
  contentType: string
  sizeBytes: number
  note: string | null
  createdAt: string
}
