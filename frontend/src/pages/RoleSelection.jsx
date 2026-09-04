import { Navigate, Link } from 'react-router-dom'
import { homeFor, useAuth } from '../auth/AuthContext.jsx'

export default function RoleSelection() {
  const { user } = useAuth()

  if (user) return <Navigate to={homeFor(user.role)} replace />

  return (
    <main className="role-page">
      <section className="role-panel">
        <div className="role-intro">
          <div className="brand">NORTHWIND</div>
          <p className="eyebrow">Enterprise IT support</p>
          <h1>How would you like to enter?</h1>
          <p className="role-description">
            Choose the workspace that matches your role. Employees can ask questions and track tickets;
            administrators can manage users, tickets, and the knowledge base.
          </p>
        </div>
        <div className="role-actions">
          <Link className="role-option role-option-admin" to="/login/admin">
            <span className="role-icon" aria-hidden="true">A</span>
            <span>
              <strong>Admin</strong>
              <small>Manage users, tickets, and knowledge base</small>
            </span>
            <span className="role-arrow" aria-hidden="true">-&gt;</span>
          </Link>
          <Link className="role-option role-option-user" to="/login/user">
            <span className="role-icon" aria-hidden="true">U</span>
            <span>
              <strong>User</strong>
              <small>Ask IT questions and view your tickets</small>
            </span>
            <span className="role-arrow" aria-hidden="true">-&gt;</span>
          </Link>
        </div>
      </section>
    </main>
  )
}
