export interface CreateOrderInput {
  customerName: string
  email: string
  phone: string
  cityAge?: string
  packageId: string
  kvkkConsent: boolean
}

export type OrderStatus = 'PENDING' | 'MOCK_PAID' | 'FAILED'

export interface OrderResponse {
  orderNumber: string
  packageName: string
  priceMinor: number
  status: OrderStatus
  createdAt: string
}
