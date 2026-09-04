import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'

export default function EmployeeLayout() {
  const { user, logout } = useAuth()
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">IT Helpdesk</div>
        <div className="brand-sub">{user.fullName} · Employee</div>
        <nav className="nav">
          <NavLink to="/app" end>AI Assistant</NavLink>
          <NavLink to="/app/tickets">My tickets</NavLink>
          <button onClick={logout}>Sign out</button>
        </nav>
      </aside>
      <main className="content">
        <Outlet />
      </main>
    </div>
  )
}
