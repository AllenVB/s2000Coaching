import { usePaymentMethods } from '@/api/ordersApi'

export interface PaymentCopy {
  /** Short headline for the trust badge and the final call to action. */
  title: string
  caption: string
  /** One line under the cart total. */
  cartNote: string
}

/**
 * What the site says about paying, taken from what checkout really offers. The page used to
 * hard-code "test mode" and instalment promises that had nothing to do with the configured
 * methods, which would tell a real buyer the wrong thing.
 */
export function usePaymentCopy(): PaymentCopy {
  const { data: methods } = usePaymentMethods()
  const kinds = new Set(methods?.map((option) => option.method) ?? [])

  if (kinds.has('IYZICO') && kinds.has('IBAN')) {
    return {
      title: 'Kart veya Havale ile Ödeme',
      caption: 'Güvenli ödeme sayfası ya da IBAN',
      cartNote: 'Kart ya da havale/EFT ile ödenir',
    }
  }
  if (kinds.has('IYZICO')) {
    return {
      title: 'Kart ile Güvenli Ödeme',
      caption: 'Ödeme, bankanın güvenli sayfasında alınır',
      cartNote: 'Kart ile güvenli ödeme sayfasında ödenir',
    }
  }
  if (kinds.has('IBAN')) {
    return {
      title: 'Havale / EFT ile Ödeme',
      caption: 'Siparişten sonra IBAN bilgisi gösterilir',
      cartNote: 'Havale / EFT ile ödenir',
    }
  }
  return {
    title: 'Test Modu — Simülasyon',
    caption: 'Gerçek ödeme yöntemi tanımlanana kadar',
    cartNote: 'Test modu — simülasyon ile onaylanır',
  }
}
