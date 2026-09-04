import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'

export default function StaffLayout() {
  const { user, logout } = useAuth()
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">IT Operations</div>
        <div className="brand-sub">{user.fullName} · {user.role}</div>
        <nav className="nav">
          <NavLink to="/staff" end>Ticket queue</NavLink>
          <NavLink to="/staff/assistant">AI assistant</NavLink>
          <button onClick={logout}>Sign out</button>
        </nav>
      </aside>
      <main className="content"><Outlet /></main>
    </div>
  )
}
