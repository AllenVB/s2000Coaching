export interface Membership {
  id: string
  label: string
  startsOn: string
  endsOn: string
  /** Days until the period ends, counting today; negative once it has ended. */
  daysLeft: number
  active: boolean
  /** The order it came from; null when the coach added it by hand. */
  orderNumber: string | null
}
