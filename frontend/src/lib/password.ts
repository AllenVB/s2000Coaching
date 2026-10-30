export const MIN_PASSWORD_LENGTH = 8
export const MAX_PASSWORD_LENGTH = 64

/** What a chosen password must contain. The server checks the same rules (`StrongPassword`). */
export const PASSWORD_RULES = [
  { id: 'length', label: 'En az 8 karakter', test: (value: string) => value.length >= MIN_PASSWORD_LENGTH },
  { id: 'upper', label: 'En az bir büyük harf', test: (value: string) => /\p{Lu}/u.test(value) },
  { id: 'lower', label: 'En az bir küçük harf', test: (value: string) => /\p{Ll}/u.test(value) },
  { id: 'digit', label: 'En az bir rakam', test: (value: string) => /\p{Nd}/u.test(value) },
  {
    id: 'special',
    label: 'En az bir özel karakter (örn. . ! ? * -)',
    test: (value: string) => /[^\p{L}\p{Nd}\s]/u.test(value),
  },
] as const

export const PASSWORD_RULES_MESSAGE =
  'Şifre en az 8 karakter olmalı; büyük harf, küçük harf, rakam ve özel karakter içermeli.'

export function isStrongPassword(value: string, minLength = MIN_PASSWORD_LENGTH): boolean {
  return (
    value.length >= minLength &&
    value.length <= MAX_PASSWORD_LENGTH &&
    PASSWORD_RULES.every((rule) => rule.id === 'length' || rule.test(value))
  )
}
