export interface CreateOrderInput {
  customerName: string
  email: string
  phone: string
  cityAge?: string
  packageId: string
  kvkkConsent: boolean
  /** Omitted means the first method the shop offers. */
  paymentMethod?: PaymentMethod
}

export type PaymentMethod = 'IBAN' | 'IYZICO' | 'MOCK'

export type OrderStatus =
  | 'PENDING'
  | 'AWAITING_TRANSFER'
  | 'AWAITING_PAYMENT'
  | 'PAID'
  | 'MOCK_PAID'
  | 'FAILED'
  | 'CANCELLED'

export interface BankDetails {
  iban: string
  accountHolder: string
  bankName: string
  /** What the buyer must write in the transfer description; null before an order exists. */
  reference: string | null
}

export interface PaymentMethodOption {
  method: PaymentMethod
  label: string
  bank: BankDetails | null
}

export interface OrderResponse {
  orderNumber: string
  packageName: string
  priceMinor: number
  status: OrderStatus
  createdAt: string
  /**
   * Where to send the buyer to pay, when the provider needs its own 3D Secure page. Null
   * with the development gateway, which settles the order inside the same request.
   */
  redirectUrl: string | null
  paymentMethod: PaymentMethod
  /** Set for an IBAN order: where to transfer, and the reference to write. */
  bankTransfer: BankDetails | null
}
