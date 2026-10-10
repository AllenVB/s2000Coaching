import { z } from 'zod'

export const createMemberSchema = z.object({
  email: z.string().trim().email('Geçerli bir e-posta adresi girin').max(255),
  fullName: z.string().trim().min(2, 'Ad soyad en az 2 karakter olmalı').max(160),
  phone: z.string().trim().min(10, 'Geçerli bir telefon numarası girin').max(40),
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

/** Editable subset of a member; e-mail is intentionally not editable (see UpdateMemberRequest). */
export const updateMemberSchema = createMemberSchema.omit({ email: true })

export type UpdateMemberFormValues = z.infer<typeof updateMemberSchema>
