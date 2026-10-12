import { Route, Routes } from 'react-router-dom'

import { AdminAreaLayout } from '@/components/layout/AdminAreaLayout'
import { MemberAreaLayout } from '@/components/layout/MemberAreaLayout'
import { AdminDashboardPage } from '@/pages/admin/AdminDashboardPage'
import { AdminOrdersPage } from '@/pages/admin/AdminOrdersPage'
import { AdminSettingsPage } from '@/pages/admin/AdminSettingsPage'
import { ExerciseCatalogPage } from '@/pages/admin/ExerciseCatalogPage'
import { MemberDetailPage } from '@/pages/admin/MemberDetailPage'
import { MemberListPage } from '@/pages/admin/MemberListPage'
import { NewMemberPage } from '@/pages/admin/NewMemberPage'
import { PlanTemplatesPage } from '@/pages/admin/PlanTemplatesPage'
import { MemberNotesTab } from '@/pages/admin/tabs/MemberNotesTab'
import { MemberMeasurementsTab } from '@/pages/admin/tabs/MemberMeasurementsTab'
import { MemberMessagesTab } from '@/pages/admin/tabs/MemberMessagesTab'
import { MemberNutritionTab } from '@/pages/admin/tabs/MemberNutritionTab'
import { MemberTrainingTab } from '@/pages/admin/tabs/MemberTrainingTab'
import { MemberProfileTab } from '@/pages/admin/tabs/MemberProfileTab'
import { LandingPage } from '@/pages/LandingPage'
import { MemberHistoryPage } from '@/pages/member/MemberHistoryPage'
import { MemberMeasurementsPage } from '@/pages/member/MemberMeasurementsPage'
import { MemberMessagesPage } from '@/pages/member/MemberMessagesPage'
import { MemberNutritionPage } from '@/pages/member/MemberNutritionPage'
import { MemberTrainingPage } from '@/pages/member/MemberTrainingPage'
import { MemberWorkoutDayPage } from '@/pages/member/MemberWorkoutDayPage'
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
        <Route path="antrenman/:dayId" element={<MemberWorkoutDayPage />} />
        <Route path="beslenme" element={<MemberNutritionPage />} />
        <Route path="olcumler" element={<MemberMeasurementsPage />} />
        <Route path="gecmis" element={<MemberHistoryPage />} />
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
          <Route path="mesajlar" element={<MemberMessagesTab />} />
          <Route path="notlar" element={<MemberNotesTab />} />
        </Route>
        <Route path="hareketler" element={<ExerciseCatalogPage />} />
        <Route path="sablonlar" element={<PlanTemplatesPage />} />
        <Route path="siparisler" element={<AdminOrdersPage />} />
        <Route path="ayarlar" element={<AdminSettingsPage />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
