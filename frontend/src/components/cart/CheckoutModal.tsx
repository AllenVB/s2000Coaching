import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { useCreateOrder } from '@/api/ordersApi'
import { Button } from '@/components/ui/Button'
import { CheckIcon, CloseIcon, ShieldIcon } from '@/components/ui/icons'
import { useCart } from '@/context/CartContext'
import { formatMinor } from '@/lib/money'
import { checkoutSchema, type CheckoutFormValues } from '@/schemas/checkoutSchema'

export function CheckoutModal() {
  const { selectedPackage, isCheckoutOpen, closeCheckout, removeFromCart } = useCart()
  const createOrder = useCreateOrder()

  const {
    register,
    handleSubmit,
    reset,
    formState: { errors },
  } = useForm<CheckoutFormValues>({
    resolver: zodResolver(checkoutSchema),
    defaultValues: { customerName: '', email: '', phone: '', cityAge: '', kvkkConsent: false },
  })

  if (!isCheckoutOpen || !selectedPackage) {
    return null
  }

  const handleClose = () => {
    closeCheckout()
    createOrder.reset()
    reset()
  }

  const handleFinish = () => {
    removeFromCart()
    handleClose()
  }

  const onSubmit = (values: CheckoutFormValues) => {
    createOrder.mutate({
      customerName: values.customerName,
      email: values.email,
      phone: values.phone,
      cityAge: values.cityAge || undefined,
      packageId: selectedPackage.id,
      kvkkConsent: values.kvkkConsent,
    })
  }

  const discountAmount = selectedPackage.originalPriceMinor - selectedPackage.priceMinor

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/80 p-4 backdrop-blur-sm sm:p-6">
      <div className="relative flex max-h-[90vh] w-full max-w-4xl flex-col overflow-hidden rounded-2xl border border-border bg-surface shadow-2xl">
        <div className="flex h-16 shrink-0 items-center justify-between border-b border-border bg-surface-elevated px-6">
          <div className="flex items-center gap-2">
            <span className="h-2 w-2 rounded-full bg-primary" />
            <h3 className="text-sm font-bold uppercase tracking-wider text-text-primary">
              Siparişi Tamamla & Kayıt
            </h3>
          </div>
          <button
            onClick={handleClose}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-text-secondary hover:text-text-primary"
          >
            <CloseIcon className="h-4 w-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {createOrder.isSuccess ? (
            <div className="flex flex-col items-center px-4 py-12 text-center">
              <div className="mb-4 flex h-14 w-14 items-center justify-center rounded-full bg-success/15 text-success">
                <CheckIcon className="h-7 w-7" />
              </div>
              <h3 className="mb-2 text-2xl font-bold text-text-primary">Siparişiniz Başarıyla Tamamlandı!</h3>
              <p className="mb-2 max-w-md text-sm leading-relaxed text-text-secondary">
                Sipariş numaranız: <span className="font-mono font-semibold text-text-primary">
                  {createOrder.data.orderNumber}
                </span>
              </p>
              <p className="mb-6 max-w-md text-xs leading-relaxed text-text-secondary">
                Kaydınız oluşturuldu. E-posta adresinize sporcu analiz formunuz iletilecek. Koçunuz en kısa sürede
                sizinle iletişime geçecektir.
              </p>
              <Button onClick={handleFinish}>Kapat ve Ana Sayfaya Dön</Button>
            </div>
          ) : (
            <div className="grid grid-cols-1 gap-8 lg:grid-cols-12">
              <div className="lg:col-span-7">
                <h4 className="mb-4 border-b border-border pb-2 text-sm font-bold uppercase tracking-wider text-text-primary">
                  1. Sporcu Bilgileri
                </h4>
                <form id="checkout-form" className="space-y-4" onSubmit={handleSubmit(onSubmit)}>
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <Field label="Ad Soyad *" error={errors.customerName?.message}>
                      <input
                        {...register('customerName')}
                        type="text"
                        placeholder="Adınız ve Soyadınız"
                        className={inputClasses}
                      />
                    </Field>
                    <Field label="E-Posta Adresi *" error={errors.email?.message}>
                      <input
                        {...register('email')}
                        type="email"
                        placeholder="ornek@s2000coaching.com"
                        className={inputClasses}
                      />
                    </Field>
                  </div>
                  <div className="grid grid-cols-1 gap-4 sm:grid-cols-2">
                    <Field label="WhatsApp / Telefon *" error={errors.phone?.message}>
                      <input
                        {...register('phone')}
                        type="tel"
                        placeholder="+90 5XX XXX XX XX"
                        className={inputClasses}
                      />
                    </Field>
                    <Field label="Şehir & Yaş" error={errors.cityAge?.message}>
                      <input
                        {...register('cityAge')}
                        type="text"
                        placeholder="Örn: 28, İstanbul"
                        className={inputClasses}
                      />
                    </Field>
                  </div>

                  <div className="flex items-start gap-2 rounded-lg border border-border bg-surface-elevated p-3.5 text-xs text-text-secondary">
                    <ShieldIcon className="mt-0.5 h-4 w-4 shrink-0 text-primary" />
                    <span>
                      <strong className="text-text-primary">Test modu:</strong> Gerçek ödeme sağlayıcısı entegre
                      edilene kadar siparişiniz simülasyon ile anında onaylanır. Kart bilgisi istenmez.
                    </span>
                  </div>

                  <div className="flex items-start gap-2 pt-1">
                    <input
                      {...register('kvkkConsent')}
                      id="kvkk-consent"
                      type="checkbox"
                      className="mt-0.5 rounded border-border-strong bg-surface text-primary focus:ring-0"
                    />
                    <label htmlFor="kvkk-consent" className="text-[11px] leading-tight text-text-muted">
                      Mesafeli Satış Sözleşmesi ve KVKK Onam Metnini okudum, kabul ediyorum.
                    </label>
                  </div>
                  {errors.kvkkConsent ? <p className="text-[11px] text-error">{errors.kvkkConsent.message}</p> : null}

                  {createOrder.isError ? (
                    <p className="text-xs text-error">
                      Sipariş oluşturulamadı: {createOrder.error.message}
                    </p>
                  ) : null}

                  <Button type="submit" className="w-full" disabled={createOrder.isPending}>
                    {createOrder.isPending ? 'Gönderiliyor…' : 'Siparişi Onayla'}
                  </Button>
                </form>
              </div>

              <div className="flex flex-col justify-between rounded-xl border border-border bg-surface-elevated p-6 lg:col-span-5">
                <div>
                  <h4 className="mb-4 border-b border-border pb-2 text-sm font-bold uppercase tracking-wider text-text-primary">
                    2. Sipariş Özeti
                  </h4>
                  <div className="space-y-3">
                    <div className="text-xs font-bold uppercase text-primary">{selectedPackage.badge}</div>
                    <div className="text-base font-bold text-text-primary">{selectedPackage.name}</div>
                    <div className="text-xs text-text-secondary">{selectedPackage.tagline}</div>
                  </div>
                  <div className="mt-6 space-y-2 border-t border-border pt-4 text-xs">
                    <div className="flex items-center justify-between text-text-secondary">
                      <span>Liste Fiyatı</span>
                      <span className="text-text-muted line-through">
                        {formatMinor(selectedPackage.originalPriceMinor)}
                      </span>
                    </div>
                    <div className="flex items-center justify-between text-primary">
                      <span>İndirim</span>
                      <span>-{formatMinor(discountAmount)}</span>
                    </div>
                    <div className="flex items-center justify-between border-t border-border pt-3 text-sm font-bold">
                      <span className="text-text-primary">Toplam Ödenecek</span>
                      <span className="text-lg text-primary">{formatMinor(selectedPackage.priceMinor)}</span>
                    </div>
                  </div>
                </div>
                <div className="mt-6 space-y-2 border-t border-border pt-6 text-[11px] text-text-muted">
                  <p className="flex items-center gap-1.5">
                    <CheckIcon className="h-3.5 w-3.5 text-success" /> Anında Sporcu Analiz Formu Gönderimi
                  </p>
                  <p className="flex items-center gap-1.5">
                    <CheckIcon className="h-3.5 w-3.5 text-success" /> Koç Tanışma Mesajı
                  </p>
                </div>
              </div>
            </div>
          )}
        </div>
      </div>
    </div>
  )
}

const inputClasses =
  'h-11 w-full rounded-lg border border-border bg-surface px-3.5 text-xs text-text-primary focus:border-primary focus:outline-none'

function Field({
  label,
  error,
  children,
}: {
  label: string
  error?: string
  children: React.ReactNode
}) {
  return (
    <div>
      <label className="mb-1.5 block text-xs font-medium text-text-secondary">{label}</label>
      {children}
      {error ? <p className="mt-1 text-[11px] text-error">{error}</p> : null}
    </div>
  )
}
