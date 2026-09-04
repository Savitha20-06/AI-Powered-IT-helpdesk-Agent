import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext.jsx'

export default function AdminLayout() {
  const { user, logout } = useAuth()
  return (
    <div className="shell">
      <aside className="sidebar">
        <div className="brand">Admin Console</div>
        <div className="brand-sub">{user.fullName} · ADMIN</div>
        <nav className="nav">
          <NavLink to="/admin" end>Analytics</NavLink>
          <NavLink to="/admin/users">Users</NavLink>
          <NavLink to="/admin/kb">Knowledge base</NavLink>
          <NavLink to="/admin/tickets">All tickets</NavLink>
          <NavLink to="/admin/assistant">AI assistant</NavLink>
          <button onClick={logout}>Sign out</button>
        </nav>
      </aside>
      <main className="content"><Outlet /></main>
    </div>
  )
}
