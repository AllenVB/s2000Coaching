import { Link } from 'react-router-dom'

import { Button } from '@/components/ui/Button'
import { ArrowLeftIcon } from '@/components/ui/icons'

/**
 * The strip above the sheet: a way back and the print button. It is not part of the page that
 * prints (`print:hidden`), which is why the sheet is its own component.
 */
export function PrintToolbar({ backTo, backLabel }: { backTo: string; backLabel: string }) {
  return (
    <div className="mb-5 flex flex-wrap items-center justify-between gap-3 print:hidden">
      <Link
        to={backTo}
        className="inline-flex items-center gap-1.5 text-xs font-semibold uppercase tracking-wide text-text-muted transition-colors hover:text-text-primary"
      >
        <ArrowLeftIcon className="h-3.5 w-3.5" />
        {backLabel}
      </Link>
      <div className="flex flex-wrap items-center gap-3">
        <p className="max-w-md text-xs leading-relaxed text-text-secondary">
          Açılan pencerede yazıcı olarak <strong>PDF olarak kaydet</strong>&apos;i seçerseniz dosya olarak
          indirebilirsiniz. Sayfa yatay (A4) basılır.
        </p>
        <Button type="button" onClick={() => window.print()}>
          Yazdır / PDF
        </Button>
      </div>
    </div>
  )
}
