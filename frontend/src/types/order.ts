export interface CreateOrderInput {
  customerName: string
  email: string
  phone: string
  cityAge?: string
  packageId: string
  kvkkConsent: boolean
}

export type OrderStatus = 'PENDING' | 'AWAITING_PAYMENT' | 'PAID' | 'FAILED'

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
}
