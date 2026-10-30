import { useState, type ComponentPropsWithRef } from 'react'

import { inputClasses } from '@/components/ui/FormField'
import { EyeIcon, EyeOffIcon } from '@/components/ui/icons'

/** A password field with an eye button that shows what was typed while it is pressed in. */
export function PasswordInput({ className = '', ...props }: Omit<ComponentPropsWithRef<'input'>, 'type'>) {
  const [visible, setVisible] = useState(false)

  return (
    <div className="relative">
      <input {...props} type={visible ? 'text' : 'password'} className={`${className || inputClasses} pr-11`} />
      <button
        type="button"
        onClick={() => setVisible((current) => !current)}
        aria-label={visible ? 'Şifreyi gizle' : 'Şifreyi göster'}
        aria-pressed={visible}
        className="absolute inset-y-0 right-0 flex w-11 items-center justify-center text-text-muted transition-colors hover:text-text-primary"
      >
        {visible ? <EyeOffIcon className="h-4 w-4" /> : <EyeIcon className="h-4 w-4" />}
      </button>
    </div>
  )
}
