import { Button } from '@/components/ui/Button'
import { CartIcon, CloseIcon, TrashIcon } from '@/components/ui/icons'
import { useCart } from '@/context/CartContext'
import { formatMinor } from '@/lib/money'

export function CartDrawer() {
  const { selectedPackage, isDrawerOpen, closeDrawer, removeFromCart, openCheckout } = useCart()

  return (
    <>
      <div
        onClick={closeDrawer}
        className={`fixed inset-0 z-50 bg-black/60 backdrop-blur-sm transition-opacity duration-200 ${
          isDrawerOpen ? 'opacity-100' : 'pointer-events-none opacity-0'
        }`}
      />
      <aside
        className={`fixed right-0 top-0 z-50 flex h-full w-full flex-col justify-between border-l border-border bg-surface shadow-2xl transition-transform duration-300 ease-out sm:w-[400px] ${
          isDrawerOpen ? 'translate-x-0' : 'translate-x-full'
        }`}
      >
        <div className="flex h-[72px] items-center justify-between border-b border-border bg-surface-elevated px-6">
          <div className="flex items-center gap-2">
            <CartIcon className="h-5 w-5 text-primary" />
            <h3 className="text-sm font-bold uppercase tracking-wider text-text-primary">Sepetiniz</h3>
          </div>
          <button
            onClick={closeDrawer}
            className="flex h-8 w-8 items-center justify-center rounded-lg border border-border bg-surface text-text-secondary hover:text-text-primary"
          >
            <CloseIcon className="h-4 w-4" />
          </button>
        </div>

        <div className="flex-1 overflow-y-auto p-6">
          {selectedPackage ? (
            <div className="flex flex-col justify-between rounded-xl border border-border bg-surface-elevated p-4">
              <div className="mb-3 flex items-start justify-between gap-3">
                <div>
                  <span className="text-[10px] font-bold uppercase tracking-wider text-primary">
                    {selectedPackage.durationMonths} Ay Protokol
                  </span>
                  <h4 className="mt-0.5 text-sm font-bold leading-snug text-text-primary">{selectedPackage.name}</h4>
                  <p className="mt-0.5 text-[11px] text-text-secondary">{selectedPackage.tagline}</p>
                </div>
                <button
                  onClick={removeFromCart}
                  title="Kaldır"
                  className="p-1 text-text-muted transition-colors hover:text-error"
                >
                  <TrashIcon className="h-[18px] w-[18px]" />
                </button>
              </div>
              <div className="flex items-center justify-between border-t border-border pt-3">
                <span className="text-xs text-text-muted">Tutar:</span>
                <div className="text-right">
                  <span className="text-sm font-bold text-primary">{formatMinor(selectedPackage.priceMinor)}</span>
                  <span className="ml-1.5 text-xs text-text-muted line-through">
                    {formatMinor(selectedPackage.originalPriceMinor)}
                  </span>
                </div>
              </div>
            </div>
          ) : (
            <div className="flex h-full flex-col items-center justify-center py-16 text-center">
              <CartIcon className="mb-3 h-10 w-10 text-text-muted" />
              <p className="text-sm font-medium text-text-secondary">Sepetiniz boş.</p>
              <p className="mt-1 text-xs text-text-muted">Lütfen bir koçluk veya beslenme paketi seçin.</p>
            </div>
          )}
        </div>

        {selectedPackage ? (
          <div className="space-y-4 border-t border-border bg-surface-elevated p-6">
            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between text-text-secondary">
                <span>Ara Toplam</span>
                <span className="font-medium text-text-primary">{formatMinor(selectedPackage.originalPriceMinor)}</span>
              </div>
              <div className="flex items-center justify-between text-primary">
                <span>İndirim</span>
                <span>
                  -{formatMinor(selectedPackage.originalPriceMinor - selectedPackage.priceMinor)}
                </span>
              </div>
              <div className="flex items-center justify-between border-t border-border pt-2 text-sm font-bold">
                <span className="text-text-primary">Toplam Tutar</span>
                <span className="text-base text-primary">{formatMinor(selectedPackage.priceMinor)}</span>
              </div>
            </div>
            <Button className="w-full" onClick={openCheckout}>
              Ödemeye Geç
            </Button>
            <div className="text-center text-[11px] text-text-muted">Test modu — simülasyon ile onaylanır</div>
          </div>
        ) : null}
      </aside>
    </>
  )
}
