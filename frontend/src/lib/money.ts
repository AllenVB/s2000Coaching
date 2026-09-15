const formatter = new Intl.NumberFormat('tr-TR', {
  style: 'currency',
  currency: 'TRY',
  maximumFractionDigits: 0,
})

export function formatMinor(priceMinor: number): string {
  return formatter.format(priceMinor / 100)
}
