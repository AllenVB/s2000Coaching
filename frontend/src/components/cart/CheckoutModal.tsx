import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'

import { useEffect, useState } from 'react'

import { useCreateOrder, usePaymentMethods } from '@/api/ordersApi'
import { Button } from '@/components/ui/Button'
import { Field, inputClasses } from '@/components/ui/FormField'
import { CheckIcon, CloseIcon } from '@/components/ui/icons'
import { useCart } from '@/context/CartContext'
import { formatMinor } from '@/lib/money'
import { checkoutSchema, type CheckoutFormValues } from '@/schemas/checkoutSchema'
import type { PaymentMethod } from '@/types/order'

export function CheckoutModal() {
  const { selectedPackage, isCheckoutOpen, closeCheckout, removeFromCart } = useCart()
  const createOrder = useCreateOrder()
  const { data: methods } = usePaymentMethods()
  const [chosenMethod, setChosenMethod] = useState<PaymentMethod | null>(null)
  // The first offered method until the buyer picks another.
  const method = chosenMethod ?? methods?.[0]?.method ?? null
  const selected = methods?.find((option) => option.method === method) ?? null

  // The card provider's own page takes over: send the buyer there as soon as the order exists.
  const redirectUrl = createOrder.isSuccess ? createOrder.data.redirectUrl : null
  useEffect(() => {
    if (redirectUrl) {
      window.location.assign(redirectUrl)
    }
  }, [redirectUrl])

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
      paymentMethod: method ?? undefined,
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
          {createOrder.isSuccess && createOrder.data.status === 'FAILED' ? (
            <div className="flex flex-col items-center px-4 py-12 text-center">
              <h3 className="mb-2 text-2xl font-bold text-text-primary">Ödeme Başlatılamadı</h3>
              <p className="mb-6 max-w-md text-sm leading-relaxed text-text-secondary">
                Ödeme sayfası şu an açılamadı; kartınızdan herhangi bir çekim yapılmadı. Biraz sonra tekrar deneyin
                ya da havale / EFT ile ödemeyi seçin.
              </p>
              <Button onClick={() => createOrder.reset()}>Geri Dön</Button>
            </div>
          ) : createOrder.isSuccess && createOrder.data.redirectUrl ? (
            // The provider wants the buyer on its own 3D Secure page. Saying "order complete"
            // here would be a lie: nothing has been charged until they come back.
            <div className="flex flex-col items-center px-4 py-12 text-center">
              <h3 className="mb-2 text-2xl font-bold text-text-primary">Ödeme Adımına Geçiliyor</h3>
              <p className="mb-2 max-w-md text-sm leading-relaxed text-text-secondary">
                Sipariş numaranız: <span className="font-mono font-semibold text-text-primary">
                  {createOrder.data.orderNumber}
                </span>
              </p>
              <p className="mb-6 max-w-md text-xs leading-relaxed text-text-secondary">
                Ödemeyi bankanızın güvenli 3D Secure sayfasında tamamlayacaksınız. Sayfa açılmazsa aşağıdaki
                düğmeye basın.
              </p>
              <a
                href={createOrder.data.redirectUrl}
                className="inline-flex h-12 items-center justify-center rounded-full bg-primary px-7 text-sm font-semibold uppercase tracking-wide text-canvas transition-colors hover:bg-primary-hover"
              >
                Ödemeye Devam Et
              </a>
            </div>
          ) : createOrder.isSuccess && createOrder.data.bankTransfer ? (
            <div className="mx-auto max-w-lg px-2 py-8">
              <h3 className="mb-2 text-center text-2xl font-bold text-text-primary">Siparişiniz Alındı</h3>
              <p className="mb-5 text-center text-sm text-text-secondary">
                Ödemeyi aşağıdaki hesaba havale/EFT ile yapın. Açıklama kısmına{' '}
                <strong className="text-text-primary">sipariş numaranızı</strong> yazmayı unutmayın.
              </p>
              <dl className="space-y-3 rounded-xl border border-border bg-surface-elevated p-5 text-sm">
                <BankRow label="Alıcı" value={createOrder.data.bankTransfer.accountHolder} />
                {createOrder.data.bankTransfer.bankName ? (
                  <BankRow label="Banka" value={createOrder.data.bankTransfer.bankName} />
                ) : null}
                <BankRow label="IBAN" value={formatIban(createOrder.data.bankTransfer.iban)} mono />
                <BankRow label="Tutar" value={formatMinor(createOrder.data.priceMinor)} />
                <BankRow label="Açıklama" value={createOrder.data.orderNumber} mono />
              </dl>
              <p className="mt-4 text-xs leading-relaxed text-text-muted">
                Ödemeniz hesaba ulaşıp koç tarafından onaylandığında üyeliğiniz başlar ve giriş bilgileriniz
                e-postanıza gönderilir. Bu genellikle aynı gün içinde olur.
              </p>
              <Button className="mt-6 w-full" onClick={handleFinish}>
                Anladım, Kapat
              </Button>
            </div>
          ) : createOrder.isSuccess ? (
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

                  <fieldset className="space-y-2">
                    <legend className="mb-1.5 text-xs font-medium text-text-secondary">Ödeme Yöntemi</legend>
                    {methods?.map((option) => (
                      <label
                        key={option.method}
                        className={`flex cursor-pointer items-start gap-2.5 rounded-lg border p-3 text-xs ${
                          option.method === method
                            ? 'border-primary bg-primary/5 text-text-primary'
                            : 'border-border bg-surface-elevated text-text-secondary'
                        }`}
                      >
                        <input
                          type="radio"
                          name="payment-method"
                          checked={option.method === method}
                          onChange={() => setChosenMethod(option.method)}
                          className="mt-0.5"
                        />
                        <span>
                          <span className="block font-semibold text-text-primary">{option.label}</span>
                          {option.method === 'MOCK' ? (
                            <span className="block text-text-muted">
                              Gerçek ödeme sağlayıcısı tanımlanana kadar sipariş simülasyonla onaylanır. Kart
                              bilgisi istenmez.
                            </span>
                          ) : null}
                          {option.method === 'IBAN' ? (
                            <span className="block text-text-muted">
                              Siparişten sonra IBAN bilgisi gösterilir; ödemeniz koç tarafından onaylanınca
                              üyeliğiniz başlar.
                            </span>
                          ) : null}
                        </span>
                      </label>
                    ))}
                  </fieldset>

                  <div className="flex items-start gap-2 pt-1">
                    <input
                      {...register('kvkkConsent')}
                      id="kvkk-consent"
                      type="checkbox"
                      className="mt-0.5 rounded border-border-strong bg-surface text-primary focus:ring-0"
                    />
                    <label htmlFor="kvkk-consent" className="text-[11px] leading-tight text-text-muted">
                      <a href="/yasal/mesafeli-satis" target="_blank" rel="noopener noreferrer" className="underline hover:text-text-primary">
                        Mesafeli Satış Sözleşmesi
                      </a>{' '}
                      ve{' '}
                      <a href="/yasal/kvkk" target="_blank" rel="noopener noreferrer" className="underline hover:text-text-primary">
                        KVKK Aydınlatma Metnini
                      </a>{' '}
                      okudum, kabul ediyorum.
                    </label>
                  </div>
                  {errors.kvkkConsent ? <p className="text-[11px] text-error">{errors.kvkkConsent.message}</p> : null}

                  {createOrder.isError ? (
                    <p className="text-xs text-error">
                      Sipariş oluşturulamadı: {createOrder.error.message}
                    </p>
                  ) : null}

                  <Button type="submit" className="w-full" disabled={createOrder.isPending || !selected}>
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

function BankRow({ label, value, mono = false }: { label: string; value: string; mono?: boolean }) {
  return (
    <div className="flex flex-wrap items-baseline justify-between gap-2">
      <dt className="text-xs uppercase tracking-wider text-text-muted">{label}</dt>
      <dd className={`select-all break-all font-semibold text-text-primary ${mono ? 'font-mono' : ''}`}>{value}</dd>
    </div>
  )
}

/** TR33 0006 1005 … in groups of four, which is how people read and copy it. */
function formatIban(iban: string): string {
  return iban.replace(/(.{4})/g, '$1 ').trim()
}
