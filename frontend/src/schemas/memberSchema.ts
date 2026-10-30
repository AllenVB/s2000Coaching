import { isStrongPassword, MAX_PASSWORD_LENGTH, PASSWORD_RULES_MESSAGE } from '@/lib/password'
import { z } from 'zod'

export const createMemberSchema = z.object({
  email: z.string().trim().email('Geçerli bir e-posta adresi girin').max(255),
  fullName: z.string().trim().min(2, 'Ad soyad en az 2 karakter olmalı').max(160),
  // Optional: a member who signed up on their own was never asked for one.
  phone: z
    .string()
    .trim()
    .max(40)
    .refine((value) => value === '' || value.length >= 10, { message: 'Geçerli bir telefon numarası girin' }),
  gender: z.enum(['', 'FEMALE', 'MALE', 'OTHER']),
  age: z
    .string()
    .trim()
    .refine((value) => value === '' || (/^\d+$/.test(value) && Number(value) >= 14 && Number(value) <= 100), {
      message: 'Yaş 14 ile 100 arasında olmalı',
    }),
  // Registered with `valueAsNumber`, so an empty input arrives as NaN and fails the
  // number check with the message below.
  heightCm: z
    .number({ message: 'Boyu santimetre olarak girin' })
    .int('Boy tam sayı olmalı')
    .min(100, 'Boy en az 100 cm olmalı')
    .max(250, 'Boy en fazla 250 cm olabilir'),
  weightKg: z
    .number({ message: 'Kiloyu kilogram olarak girin' })
    .min(30, 'Kilo en az 30 kg olmalı')
    .max(300, 'Kilo en fazla 300 kg olabilir'),
  sportsBackground: z
    .string()
    .trim()
    .min(3, 'Spor geçmişi en az 3 karakter olmalı')
    .max(5000, 'Spor geçmişi en fazla 5000 karakter olabilir'),
})

export type CreateMemberFormValues = z.infer<typeof createMemberSchema>

export const memberEmailSchema = z.object({
  email: z.string().trim().email('Geçerli bir e-posta adresi girin').max(255),
})

export type MemberEmailFormValues = z.infer<typeof memberEmailSchema>

export const memberCodeSchema = z.object({
  code: z
    .string()
    .trim()
    .regex(/^\d{6}$/, 'Onay kodu 6 haneli olmalı'),
})

export type MemberCodeFormValues = z.infer<typeof memberCodeSchema>

const ageField = z
  .number({ message: 'Yaşınızı girin' })
  .int('Yaş tam sayı olmalı')
  .min(14, 'Yaş en az 14 olmalı')
  .max(100, 'Yaş en fazla 100 olabilir')

const passwordField = z
  .string()
  .max(MAX_PASSWORD_LENGTH, 'Şifre en fazla 64 karakter olabilir')
  .refine((value) => isStrongPassword(value), PASSWORD_RULES_MESSAGE)

export const loginSchema = z.object({
  email: z.string().trim().email('Geçerli bir e-posta adresi girin').max(255),
  password: z.string().min(1, 'Şifrenizi girin'),
})

export type LoginFormValues = z.infer<typeof loginSchema>

export const registerSchema = z
  .object({
    firstName: z.string().trim().min(2, 'Adınızı girin').max(80),
    lastName: z.string().trim().min(2, 'Soyadınızı girin').max(80),
    email: z.string().trim().email('Geçerli bir e-posta adresi girin').max(255),
    age: ageField,
    gender: z.enum(['FEMALE', 'MALE', 'OTHER'], { message: 'Cinsiyetinizi seçin' }),
    heightCm: createMemberSchema.shape.heightCm,
    weightKg: createMemberSchema.shape.weightKg,
    sportsBackground: z
      .string()
      .trim()
      .min(2, 'Spor geçmişinizi yazın (yoksa "Yok" yazın)')
      .max(5000, 'Spor geçmişi en fazla 5000 karakter olabilir'),
    password: passwordField,
    passwordRepeat: z.string(),
    acceptedTerms: z.literal(true, { message: 'Devam etmek için aydınlatma metnini kabul etmelisiniz' }),
  })
  .refine((values) => values.password === values.passwordRepeat, {
    message: 'Şifreler aynı değil',
    path: ['passwordRepeat'],
  })

export type RegisterFormValues = z.infer<typeof registerSchema>

/** The mailed code plus the password to choose with it. */
export const resetPasswordSchema = z
  .object({
    code: memberCodeSchema.shape.code,
    newPassword: passwordField,
    newPasswordRepeat: z.string(),
  })
  .refine((values) => values.newPassword === values.newPasswordRepeat, {
    message: 'Şifreler aynı değil',
    path: ['newPasswordRepeat'],
  })

export type ResetPasswordFormValues = z.infer<typeof resetPasswordSchema>

/** `currentPassword` is only asked for when a password already exists; the server enforces that. */
export const changePasswordSchema = z
  .object({
    currentPassword: z.string().optional(),
    newPassword: passwordField,
    newPasswordRepeat: z.string(),
  })
  .refine((values) => values.newPassword === values.newPasswordRepeat, {
    message: 'Şifreler aynı değil',
    path: ['newPasswordRepeat'],
  })

export type ChangePasswordFormValues = z.infer<typeof changePasswordSchema>

/**
 * The coach's form: every detail of a member. Gender and age may stay empty (members the coach
 * created before they were asked); both arrive from the inputs as strings and are turned into
 * null or a number on submit.
 */
export const updateMemberSchema = z.object({
  email: z.string().trim().email('Geçerli bir e-posta adresi girin').max(255),
  fullName: createMemberSchema.shape.fullName,
  gender: z.enum(['', 'FEMALE', 'MALE', 'OTHER']),
  age: z
    .string()
    .trim()
    .refine((value) => value === '' || (/^\d+$/.test(value) && Number(value) >= 14 && Number(value) <= 100), {
      message: 'Yaş 14 ile 100 arasında olmalı',
    }),
  phone: z.string().trim().max(40),
  heightCm: createMemberSchema.shape.heightCm,
  weightKg: createMemberSchema.shape.weightKg,
  sportsBackground: createMemberSchema.shape.sportsBackground,
})

export type UpdateMemberFormValues = z.infer<typeof updateMemberSchema>

/**
 * What a member may change about themselves. Name, gender and e-mail are the coach's: the
 * e-mail identifies the account and receives its codes.
 */
export const updateMyProfileSchema = z.object({
  age: ageField,
  phone: z.string().trim().max(40),
  heightCm: createMemberSchema.shape.heightCm,
  weightKg: createMemberSchema.shape.weightKg,
  sportsBackground: createMemberSchema.shape.sportsBackground,
})

export type UpdateMyProfileFormValues = z.infer<typeof updateMyProfileSchema>
