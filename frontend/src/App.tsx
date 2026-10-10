import { Route, Routes } from 'react-router-dom'

import { AdminAreaLayout } from '@/components/layout/AdminAreaLayout'
import { MemberAreaLayout } from '@/components/layout/MemberAreaLayout'
import { AdminDashboardPage } from '@/pages/admin/AdminDashboardPage'
import { AdminOrdersPage } from '@/pages/admin/AdminOrdersPage'
import { AdminSettingsPage } from '@/pages/admin/AdminSettingsPage'
import { MemberDetailPage } from '@/pages/admin/MemberDetailPage'
import { MemberListPage } from '@/pages/admin/MemberListPage'
import { NewMemberPage } from '@/pages/admin/NewMemberPage'
import { MemberNotesTab } from '@/pages/admin/tabs/MemberNotesTab'
import { MemberMeasurementsTab } from '@/pages/admin/tabs/MemberMeasurementsTab'
import { MemberNutritionTab, MemberTrainingTab } from '@/pages/admin/tabs/MemberPlaceholderTabs'
import { MemberProfileTab } from '@/pages/admin/tabs/MemberProfileTab'
import { LandingPage } from '@/pages/LandingPage'
import { MemberMeasurementsPage } from '@/pages/member/MemberMeasurementsPage'
import {
  MemberMessagesPage,
  MemberNutritionPage,
  MemberTrainingPage,
} from '@/pages/member/MemberPlaceholderPages'
import { MemberProfilePage } from '@/pages/member/MemberProfilePage'
import { MemberTodayPage } from '@/pages/member/MemberTodayPage'
import { NotFoundPage } from '@/pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />

      <Route path="/uye" element={<MemberAreaLayout />}>
        <Route index element={<MemberTodayPage />} />
        <Route path="antrenman" element={<MemberTrainingPage />} />
        <Route path="beslenme" element={<MemberNutritionPage />} />
        <Route path="olcumler" element={<MemberMeasurementsPage />} />
        <Route path="mesajlar" element={<MemberMessagesPage />} />
        <Route path="profil" element={<MemberProfilePage />} />
      </Route>

      <Route path="/yonetim" element={<AdminAreaLayout />}>
        <Route index element={<AdminDashboardPage />} />
        <Route path="uyeler" element={<MemberListPage />} />
        <Route path="uyeler/yeni" element={<NewMemberPage />} />
        <Route path="uyeler/:memberId" element={<MemberDetailPage />}>
          <Route index element={<MemberProfileTab />} />
          <Route path="antrenman" element={<MemberTrainingTab />} />
          <Route path="beslenme" element={<MemberNutritionTab />} />
          <Route path="olcumler" element={<MemberMeasurementsTab />} />
          <Route path="notlar" element={<MemberNotesTab />} />
        </Route>
        <Route path="siparisler" element={<AdminOrdersPage />} />
        <Route path="ayarlar" element={<AdminSettingsPage />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
