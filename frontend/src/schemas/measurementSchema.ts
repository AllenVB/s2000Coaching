import { z } from 'zod'

/** Blank optional number inputs arrive as NaN via `valueAsNumber`; treat those as "not measured". */
const optionalCm = (min: number, max: number, label: string) =>
  z
    .union([z.nan(), z.number()])
    .optional()
    .transform((value) => (value === undefined || Number.isNaN(value) ? null : value))
    .refine((value) => value === null || (value >= min && value <= max), {
      message: `${label} ${min}-${max} cm arasında olmalı`,
    })

export const measurementSchema = z.object({
  measuredOn: z
    .string()
    .min(1, 'Tarih zorunlu')
    .refine((value) => value <= new Date().toISOString().slice(0, 10), {
      message: 'İleri bir tarih seçilemez',
    }),
  weightKg: z
    .number({ message: 'Kilonuzu girin' })
    .min(30, 'Kilo en az 30 kg olmalı')
    .max(300, 'Kilo en fazla 300 kg olabilir'),
  waistCm: optionalCm(30, 250, 'Bel'),
  chestCm: optionalCm(30, 250, 'Göğüs'),
  hipCm: optionalCm(30, 250, 'Kalça'),
  armCm: optionalCm(15, 100, 'Kol'),
  thighCm: optionalCm(20, 150, 'Bacak'),
  note: z.string().trim().max(2000, 'Not en fazla 2000 karakter olabilir'),
})

/**
 * The optional-circumference transform makes the parsed shape differ from what the inputs
 * produce, so the form is typed with both: `Input` is what `register` writes (NaN for a blank
 * number field), `Values` is what the resolver hands to the submit handler (null for blank).
 */
export type MeasurementFormInput = z.input<typeof measurementSchema>
export type MeasurementFormValues = z.output<typeof measurementSchema>
