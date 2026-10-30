import { MIN_PASSWORD_LENGTH, PASSWORD_RULES } from '@/lib/password'

/** The password rules, each ticked off as the typed password meets it. */
export function PasswordChecklist({ value = '', minLength = MIN_PASSWORD_LENGTH }: { value?: string; minLength?: number }) {
  return (
    <ul aria-label="Şifre gereksinimleri" className="space-y-1 text-[11px] leading-relaxed">
      {PASSWORD_RULES.map((rule) => {
        const isLength = rule.id === 'length'
        const met = isLength ? value.length >= minLength : rule.test(value)
        return (
          <li key={rule.id} className={met ? 'text-success' : 'text-text-muted'}>
            <span aria-hidden="true" className="mr-1.5 inline-block w-3">
              {met ? '✓' : '•'}
            </span>
            {isLength ? `En az ${minLength} karakter` : rule.label}
            <span className="sr-only">{met ? ' (sağlandı)' : ' (eksik)'}</span>
          </li>
        )
      })}
    </ul>
  )
}
