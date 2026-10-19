import type { ComponentPropsWithRef, ReactNode } from 'react'

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger'
type Size = 'sm' | 'md' | 'lg'

interface ButtonProps extends ComponentPropsWithRef<'button'> {
  variant?: Variant
  size?: Size
  children: ReactNode
}

// The ember glow is written out rather than taken from the token, because a colour token
// cannot be used inside a shadow's alpha; the two are kept in step by hand.
const variantClasses: Record<Variant, string> = {
  primary:
    'bg-primary text-canvas hover:bg-primary-hover shadow-[0_6px_20px_-6px_rgba(255,107,44,0.65)] hover:shadow-[0_10px_28px_-6px_rgba(255,107,44,0.8)]',
  secondary:
    'bg-surface-elevated text-text-primary border border-border-strong hover:border-primary/60 hover:bg-surface-high',
  ghost: 'bg-transparent text-text-secondary hover:text-text-primary hover:bg-text-primary/5',
  danger: 'bg-error text-white hover:bg-error/85',
}

const sizeClasses: Record<Size, string> = {
  sm: 'h-9 px-3.5 text-xs',
  md: 'h-11 px-5 text-sm',
  lg: 'h-12 px-7 text-sm',
}

export function Button({ variant = 'primary', size = 'md', className = '', children, ...props }: ButtonProps) {
  return (
    <button
      className={`inline-flex items-center justify-center gap-2 rounded-full font-semibold uppercase tracking-wide transition-all active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-50 disabled:active:scale-100 ${variantClasses[variant]} ${sizeClasses[size]} ${className}`}
      {...props}
    >
      {children}
    </button>
  )
}
