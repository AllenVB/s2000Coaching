import { PageHeading } from '@/components/ui/PageHeading'
import { useAdminSignOut } from '@/hooks/useAdminSession'
import { Button } from '@/components/ui/Button'

export function AdminSettingsPage() {
  const signOut = useAdminSignOut()

  return (
    <>
      <PageHeading title="Ayarlar" description="Yönetim erişimi ve sistem yapılandırması." />

      <div className="space-y-6">
        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">Yönetim Erişimi</h2>
          <p className="mt-2 text-xs leading-relaxed text-text-secondary">
            Giriş koç hesabıyla yapılır; şifreler bcrypt ile özetlenerek saklanır. İlk hesap sunucuda{' '}
            <code className="text-text-primary">ADMIN_EMAIL</code> ve{' '}
            <code className="text-text-primary">ADMIN_PASSWORD</code> ile oluşturulur; mevcut bir hesap asla
            üzerine yazılmaz. Oturum bu sekmede saklanır ve çıkışta sunucu tarafında da iptal edilir.
          </p>
          <Button type="button" variant="secondary" className="mt-4" onClick={signOut}>
            Oturumu Kapat
          </Button>
        </section>

        <section className="rounded-2xl border border-border bg-surface p-6">
          <h2 className="text-sm font-bold uppercase tracking-wider text-text-primary">E-Posta Gönderimi</h2>
          <p className="mt-2 text-xs leading-relaxed text-text-secondary">
            Onay ve giriş kodları SMTP üzerinden gönderilir. Sunucuda{' '}
            <code className="text-text-primary">SPRING_MAIL_HOST</code> tanımlı değilse kodlar yalnızca
            uygulama loguna yazılır ve üyelere e-posta ulaşmaz.
          </p>
        </section>
      </div>
    </>
  )
}
