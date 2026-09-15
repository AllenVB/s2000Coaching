import { z } from 'zod'

export const checkoutSchema = z.object({
  customerName: z.string().trim().min(2, 'Ad soyad en az 2 karakter olmalı').max(160),
  email: z.string().trim().email('Geçerli bir e-posta adresi girin'),
  phone: z.string().trim().min(10, 'Geçerli bir telefon numarası girin').max(40),
  cityAge: z.string().trim().max(120).optional().or(z.literal('')),
  kvkkConsent: z
    .boolean()
    .refine((value) => value === true, { message: 'Devam etmek için KVKK onamını kabul etmelisiniz' }),
})

export type CheckoutFormValues = z.infer<typeof checkoutSchema>
