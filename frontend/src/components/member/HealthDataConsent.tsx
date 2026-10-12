import { useGrantHealthDataConsent } from '@/api/measurementsApi'
import { Button } from '@/components/ui/Button'
import { ShieldIcon } from '@/components/ui/icons'

/**
 * KVKK gate for the measurement flow. Measurements are special-category health data, so they
 * need their own explicit consent — the checkout KVKK box does not cover them.
 *
 * NOTE: the wording below is placeholder copy. Replace it with the aydınlatma metni your own
 * legal advisor approves before this goes live.
 */
export function HealthDataConsent() {
  const grantConsent = useGrantHealthDataConsent()

  return (
    <section className="rounded-2xl border border-border bg-surface p-6">
      <div className="flex items-start gap-3">
        <ShieldIcon className="mt-0.5 h-5 w-5 shrink-0 text-primary" />
        <div>
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">
            Sağlık Verisi İşleme Onayı
          </h2>
          <div className="mt-3 space-y-2.5 text-xs leading-relaxed text-text-secondary">
            <p>
              Ölçümleriniz (kilo, çevre ölçüleri ve eklediğiniz notlar) KVKK kapsamında{' '}
              <strong className="text-text-primary">özel nitelikli kişisel veri</strong> sayılır. Bu verileri
              yalnızca size özel antrenman ve beslenme planınızın hazırlanması ve gelişiminizin takibi amacıyla
              işliyoruz.
            </p>
            <p>
              Verilerinize yalnızca siz ve koçunuz erişebilir; üçüncü kişilerle paylaşılmaz. Girdiğiniz her
              ölçümü dilediğiniz zaman kendiniz silebilir, üyeliğinizi sonlandırdığınızda tüm ölçüm
              kayıtlarınızın silinmesini isteyebilirsiniz.
            </p>
            <p>
              Onay vermeden de üyeliğinizi kullanabilirsiniz; yalnızca ölçüm takibi özelliği kapalı kalır.
            </p>
          </div>

          {grantConsent.isError ? (
            <p className="mt-3 text-xs text-error">Onay kaydedilemedi: {grantConsent.error.message}</p>
          ) : null}

          <Button
            type="button"
            className="mt-5"
            onClick={() => grantConsent.mutate()}
            disabled={grantConsent.isPending}
          >
            {grantConsent.isPending ? 'Kaydediliyor…' : 'Okudum, Onaylıyorum'}
          </Button>
        </div>
      </div>
    </section>
  )
}
