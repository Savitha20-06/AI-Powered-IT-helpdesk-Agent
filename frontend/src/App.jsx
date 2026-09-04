import { Navigate, Route, Routes } from 'react-router-dom'
import { homeFor, useAuth } from './auth/AuthContext.jsx'
import Login from './pages/Login.jsx'
import RoleSelection from './pages/RoleSelection.jsx'
import AdminAssistant from './pages/admin/AdminAssistant.jsx'
import EmployeeLayout from './pages/employee/EmployeeLayout.jsx'
import ChatPage from './pages/employee/ChatPage.jsx'
import EmployeeTickets from './pages/employee/EmployeeTickets.jsx'
import TicketPage from './pages/TicketPage.jsx'
import StaffLayout from './pages/staff/StaffLayout.jsx'
import StaffQueue from './pages/staff/StaffQueue.jsx'
import StaffAssistant from './pages/staff/StaffAssistant.jsx'
import AdminLayout from './pages/admin/AdminLayout.jsx'
import AdminHome from './pages/admin/AdminHome.jsx'
import AdminUsers from './pages/admin/AdminUsers.jsx'
import AdminKb from './pages/admin/AdminKb.jsx'
import AdminTickets from './pages/admin/AdminTickets.jsx'

function Guard({ roles, children }) {
  const { user, authLoading } = useAuth()
  if (authLoading) return <p>Checking your access...</p>
  if (!user) return <Navigate to="/login" replace />
  if (roles && !roles.includes(user.role)) return <Navigate to={homeFor(user.role)} replace />
  return children
}

export default function App() {
  const { user } = useAuth()
  return (
    <Routes>
      <Route path="/" element={<RoleSelection />} />
      <Route path="/login" element={<RoleSelection />} />
      <Route path="/login/user" element={user ? <Navigate to={homeFor(user.role)} replace /> : <Login expectedRole="USER" />} />
      <Route path="/login/admin" element={user ? <Navigate to={homeFor(user.role)} replace /> : <Login expectedRole="ADMIN" />} />
      <Route path="/app" element={<Guard roles={['EMPLOYEE']}><EmployeeLayout /></Guard>}>
        <Route index element={<ChatPage />} />
        <Route path="tickets" element={<EmployeeTickets />} />
        <Route path="tickets/:id" element={<TicketPage />} />
      </Route>
      <Route path="/staff" element={<Guard roles={['IT_STAFF', 'ADMIN']}><StaffLayout /></Guard>}>
        <Route index element={<StaffQueue />} />
        <Route path="assistant" element={<StaffAssistant />} />
        <Route path="tickets/:id" element={<TicketPage staff />} />
      </Route>
      <Route path="/admin" element={<Guard roles={['ADMIN']}><AdminLayout /></Guard>}>
        <Route index element={<AdminHome />} />
        <Route path="assistant" element={<AdminAssistant />} />
        <Route path="users" element={<AdminUsers />} />
        <Route path="kb" element={<AdminKb />} />
        <Route path="tickets" element={<AdminTickets />} />
        <Route path="tickets/:id" element={<TicketPage staff />} />
      </Route>
      <Route path="*" element={<Navigate to={user ? homeFor(user.role) : '/'} replace />} />
    </Routes>
  )
}
