import { Route, Routes } from 'react-router-dom'

import { AdminPage } from '@/pages/AdminPage'
import { LandingPage } from '@/pages/LandingPage'
import { MemberPage } from '@/pages/MemberPage'
import { NotFoundPage } from '@/pages/NotFoundPage'

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/uye" element={<MemberPage />} />
      <Route path="/yonetim" element={<AdminPage />} />
      <Route path="*" element={<NotFoundPage />} />
    </Routes>
  )
}
