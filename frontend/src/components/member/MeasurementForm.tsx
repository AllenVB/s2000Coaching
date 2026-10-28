import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { useSaveMyMeasurement } from '@/api/measurementsApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses, textareaClasses } from '@/components/ui/FormField'
import { CheckIcon } from '@/components/ui/icons'
import {
  measurementSchema,
  type MeasurementFormInput,
  type MeasurementFormValues,
} from '@/schemas/measurementSchema'
import { todayIso } from '@/lib/date'

const today = todayIso

/** Weekly check-in. Only weight is required so a quick scale reading still gets logged. */
export function MeasurementForm() {
  const saveMeasurement = useSaveMyMeasurement()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<MeasurementFormInput, unknown, MeasurementFormValues>({
    resolver: zodResolver(measurementSchema),
    defaultValues: { measuredOn: today(), note: '' },
  })

  const onSubmit = (values: MeasurementFormValues) => {
    saveMeasurement.mutate(
      { ...values, note: values.note.length > 0 ? values.note : null },
      { onSuccess: () => reset({ measuredOn: today(), note: '' }) },
    )
  }

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <h2 className="mb-2 text-sm font-bold uppercase tracking-wider text-text-primary">Ölçüm Ekle</h2>
      <p className="mb-5 text-xs leading-relaxed text-text-secondary">
        Yalnızca kilo zorunlu. Aynı tarihe tekrar kayıt girerseniz o günün ölçümü güncellenir.
      </p>

      <form className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-4">
          <Field label="Tarih *" error={errors.measuredOn?.message}>
            <input {...register('measuredOn')} type="date" max={today()} className={inputClasses} />
          </Field>
          <Field label="Kilo (kg) *" error={errors.weightKg?.message}>
            <input
              {...register('weightKg', { valueAsNumber: true })}
              type="number"
              step="0.1"
              min={30}
              max={300}
              placeholder="78.4"
              className={inputClasses}
            />
          </Field>
          <Field label="Bel (cm)" error={errors.waistCm?.message}>
            <input
              {...register('waistCm', { valueAsNumber: true })}
              type="number"
              step="0.5"
              placeholder="84"
              className={inputClasses}
            />
          </Field>
          <Field label="Göğüs (cm)" error={errors.chestCm?.message}>
            <input
              {...register('chestCm', { valueAsNumber: true })}
              type="number"
              step="0.5"
              placeholder="102"
              className={inputClasses}
            />
          </Field>
          <Field label="Kalça (cm)" error={errors.hipCm?.message}>
            <input
              {...register('hipCm', { valueAsNumber: true })}
              type="number"
              step="0.5"
              placeholder="96"
              className={inputClasses}
            />
          </Field>
          <Field label="Kol (cm)" error={errors.armCm?.message}>
            <input
              {...register('armCm', { valueAsNumber: true })}
              type="number"
              step="0.5"
              placeholder="36"
              className={inputClasses}
            />
          </Field>
          <Field label="Bacak (cm)" error={errors.thighCm?.message}>
            <input
              {...register('thighCm', { valueAsNumber: true })}
              type="number"
              step="0.5"
              placeholder="58"
              className={inputClasses}
            />
          </Field>
        </div>

        <Field label="Not" error={errors.note?.message}>
          <textarea
            {...register('note')}
            rows={2}
            placeholder="Uyku, stres, sakatlık, antrenman hissi…"
            className={textareaClasses}
          />
        </Field>

        {saveMeasurement.isError ? (
          <p className="text-xs text-error">Kaydedilemedi: {saveMeasurement.error.message}</p>
        ) : null}
        {saveMeasurement.isSuccess ? (
          <p className="flex items-center gap-1.5 text-xs text-success">
            <CheckIcon className="h-3.5 w-3.5" /> Ölçümünüz kaydedildi.
          </p>
        ) : null}

        <Button type="submit" disabled={saveMeasurement.isPending}>
          {saveMeasurement.isPending ? 'Kaydediliyor…' : 'Ölçümü Kaydet'}
        </Button>
      </form>
    </section>
  )
}
