export type MemberStatus = 'PENDING_VERIFICATION' | 'ACTIVE'

/** As the member stated it at sign-up; only the coach can change it afterwards. */
export type MemberGender = 'MALE' | 'FEMALE' | 'OTHER'

export const GENDERS: { value: MemberGender; label: string }[] = [
  { value: 'FEMALE', label: 'Kadın' },
  { value: 'MALE', label: 'Erkek' },
  { value: 'OTHER', label: 'Diğer' },
]

export function genderLabel(gender: MemberGender | null): string {
  return GENDERS.find((entry) => entry.value === gender)?.label ?? '—'
}

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
  /** Null for members the coach created before gender and age were asked. */
  gender: MemberGender | null
  age: number | null
  /** False means the member still signs in with an e-mailed code and can set a password. */
  hasPassword: boolean
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
  age: number
  phone: string
  heightCm: number
  weightKg: number
  sportsBackground: string
}

export interface RegisterInput {
  email: string
  password: string
  firstName: string
  lastName: string
  age: number
  gender: MemberGender
  heightCm: number
  weightKg: number
  sportsBackground: string
  acceptedTerms: boolean
}

/** With e-mail confirmation on, `session` is null and the page asks for the mailed code. */
export interface RegistrationResponse {
  verificationRequired: boolean
  session: MemberSession | null
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
