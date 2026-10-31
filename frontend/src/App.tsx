import { Navigate, Route, Routes, useLocation } from 'react-router-dom'

import { AdminAreaLayout } from '@/components/layout/AdminAreaLayout'
import { MemberAreaLayout } from '@/components/layout/MemberAreaLayout'
import { AdminDashboardPage } from '@/pages/admin/AdminDashboardPage'
import { AdminMessagesPage } from '@/pages/admin/AdminMessagesPage'
import { AdminOrdersPage } from '@/pages/admin/AdminOrdersPage'
import { AdminSettingsPage } from '@/pages/admin/AdminSettingsPage'
import { ProgramsPage } from '@/pages/admin/ProgramsPage'
import { ExperiencePage } from '@/pages/admin/ExperiencePage'
import { StoriesPage } from '@/pages/admin/StoriesPage'
import { CaseStudiesPage } from '@/pages/admin/CaseStudiesPage'
import { ExerciseCatalogPage } from '@/pages/admin/ExerciseCatalogPage'
import { MemberDetailPage } from '@/pages/admin/MemberDetailPage'
import { MemberListPage } from '@/pages/admin/MemberListPage'
import { NewMemberPage } from '@/pages/admin/NewMemberPage'
import { PlanTemplatesPage } from '@/pages/admin/PlanTemplatesPage'
import { MemberNotesTab } from '@/pages/admin/tabs/MemberNotesTab'
import { MemberFormChecksTab } from '@/pages/admin/tabs/MemberFormChecksTab'
import { MemberAnalysisTab } from '@/pages/admin/tabs/MemberAnalysisTab'
import { MemberCheckInsTab } from '@/pages/admin/tabs/MemberCheckInsTab'
import { MemberMeasurementsTab } from '@/pages/admin/tabs/MemberMeasurementsTab'
import { MemberPhotosTab } from '@/pages/admin/tabs/MemberPhotosTab'
import { MemberMessagesTab } from '@/pages/admin/tabs/MemberMessagesTab'
import { MemberNutritionTab } from '@/pages/admin/tabs/MemberNutritionTab'
import { MemberTrainingTab } from '@/pages/admin/tabs/MemberTrainingTab'
import { MemberProfileTab } from '@/pages/admin/tabs/MemberProfileTab'
import { LandingPage } from '@/pages/LandingPage'
import { MemberHistoryPage } from '@/pages/member/MemberHistoryPage'
import { MemberQuestionnairePage } from '@/pages/member/MemberQuestionnairePage'
import { MemberMeasurementsPage } from '@/pages/member/MemberMeasurementsPage'
import { MemberMessagesPage } from '@/pages/member/MemberMessagesPage'
import { MemberWorkoutDayPage } from '@/pages/member/MemberWorkoutDayPage'
import { MemberProgramsPage } from '@/pages/member/MemberProgramsPage'
import { MemberProfilePage } from '@/pages/member/MemberProfilePage'
import { MemberPrintPage as AdminMemberPrintPage } from '@/pages/admin/MemberPrintPage'
import { MemberPrintPage } from '@/pages/member/MemberPrintPage'
import { MemberMonthlyRecordsTab } from '@/pages/admin/tabs/MemberMonthlyRecordsTab'
import { CalculatorsPage } from '@/pages/CalculatorsPage'
import { LegalPage } from '@/pages/LegalPage'
import { LoginPage } from '@/pages/LoginPage'
import { NotFoundPage } from '@/pages/NotFoundPage'

/** Redirects to a sibling route but keeps the query, which here holds the chosen student. */
function KeepSearchRedirect({ to }: { to: string }) {
  const { search } = useLocation()
  return <Navigate to={{ pathname: to, search }} replace />
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/giris" element={<LoginPage />} />
      <Route path="/hesaplayicilar" element={<CalculatorsPage />} />
      <Route path="/yasal/:slug" element={<LegalPage />} />

      <Route path="/uye" element={<MemberAreaLayout />}>
        <Route index element={<Navigate to="/uye/programlarim" replace />} />
        <Route path="programlarim" element={<MemberProgramsPage />} />
        <Route path="programlarim/antrenman/:dayId" element={<MemberWorkoutDayPage />} />
        <Route path="olcumlerim" element={<MemberMeasurementsPage />} />
        <Route path="anket" element={<MemberQuestionnairePage />} />
        <Route path="gecmis" element={<MemberHistoryPage />} />
        <Route path="fotograflar" element={<Navigate to="/uye/olcumlerim?sekme=fotograflar" replace />} />
        <Route path="mesajlar" element={<MemberMessagesPage />} />
        <Route path="profil" element={<MemberProfilePage />} />
        <Route path="cikti" element={<MemberPrintPage />} />
        {/* Addresses from before the member area was reorganised (bookmarks, old e-mails). */}
        <Route path="antrenman" element={<Navigate to="/uye/programlarim?sekme=antrenman" replace />} />
        <Route path="beslenme" element={<Navigate to="/uye/programlarim?sekme=beslenme" replace />} />
        <Route path="olcumler" element={<Navigate to="/uye/olcumlerim?sekme=haftalik" replace />} />
        <Route path="verilerim" element={<Navigate to="/uye/olcumlerim?sekme=aylik" replace />} />
        <Route path="haftalik" element={<Navigate to="/uye/olcumlerim?sekme=haftalik" replace />} />
        <Route path="form-kontrol" element={<Navigate to="/uye/mesajlar?sekme=form" replace />} />
        <Route path="hikayem" element={<Navigate to="/uye/programlarim" replace />} />
      </Route>

      <Route path="/yonetim" element={<AdminAreaLayout />}>
        <Route index element={<AdminDashboardPage />} />
        <Route path="uyeler" element={<MemberListPage />} />
        <Route path="uyeler/yeni" element={<NewMemberPage />} />
        <Route path="uyeler/:memberId/cikti" element={<AdminMemberPrintPage />} />
        <Route path="uyeler/:memberId" element={<MemberDetailPage />}>
          <Route index element={<MemberProfileTab />} />
          <Route path="antrenman" element={<MemberTrainingTab />} />
          <Route path="beslenme" element={<MemberNutritionTab />} />
          <Route path="olcumler" element={<MemberMeasurementsTab />} />
          <Route path="aylik-veriler" element={<MemberMonthlyRecordsTab />} />
          <Route path="haftalik" element={<MemberCheckInsTab />} />
          <Route path="analiz" element={<MemberAnalysisTab />} />
          <Route path="form-kontrol" element={<MemberFormChecksTab />} />
          <Route path="fotograflar" element={<MemberPhotosTab />} />
          <Route path="mesajlar" element={<MemberMessagesTab />} />
          <Route path="notlar" element={<MemberNotesTab />} />
        </Route>
        <Route path="mesajlar" element={<AdminMessagesPage />} />
        <Route path="hareketler" element={<ExerciseCatalogPage />} />
        <Route path="sablonlar" element={<PlanTemplatesPage />} />
        <Route path="vaka-calismalari" element={<CaseStudiesPage />} />
        <Route path="hikayeler" element={<StoriesPage />} />
        <Route path="tecrubelerim" element={<ExperiencePage />} />
        <Route path="programlar" element={<ProgramsPage />}>
          <Route index element={<KeepSearchRedirect to="antrenman" />} />
          <Route path="antrenman" element={<MemberTrainingTab />} />
          <Route path="beslenme" element={<MemberNutritionTab />} />
        </Route>
        <Route path="siparisler" element={<AdminOrdersPage />} />
        <Route path="ayarlar" element={<AdminSettingsPage />} />
      </Route>

      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
