import { useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { homeFor, useAuth } from '../auth/AuthContext.jsx'

export default function Login({ expectedRole }) {
  const { login, logout } = useAuth()
  const navigate = useNavigate()
  const [email, setEmail] = useState('employee@helpdesk.local')
  const [password, setPassword] = useState('Password@123')
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  async function onSubmit(e) {
    e.preventDefault()
    setLoading(true)
    setError('')
    try {
      const user = await login(email, password)
      if (expectedRole === 'ADMIN' && user.role !== 'ADMIN') {
        logout()
        throw new Error('This login is for administrators only.')
      }
      if (expectedRole === 'USER' && user.role === 'ADMIN') {
        logout()
        throw new Error('Please use the Admin button for administrator access.')
      }
      navigate(homeFor(user.role))
    } catch (err) {
      const apiError = err.response?.data?.error
      setError(apiError || (err.response ? `Login failed (HTTP ${err.response.status})` : 'Helpdesk API is unavailable. Start the backend and try again.'))
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="login-wrap">
      <div className="login-card">
        <div className="login-copy">
          <div className="brand">NORTHWIND</div>
          <h1>AI-Powered Enterprise IT Helpdesk</h1>
          <p>
            Employees ask IT questions against the approved knowledge base. The assistant uses RAG
            (retrieve, then generate). If the documents are not enough, it escalates and opens a ticket instead of guessing.
          </p>
          <p>
            Demo users:<br />
            employee@helpdesk.local<br />
            itstaff@helpdesk.local<br />
            admin@helpdesk.local<br />
            Password for all: Password@123
          </p>
        </div>
        <form className="login-form" onSubmit={onSubmit}>
          <h2>{expectedRole === 'ADMIN' ? 'Admin sign in' : 'User sign in'}</h2>
          <Link className="login-back" to="/">&lt;- Choose another entry</Link>
          <label>Work email</label>
          <input value={email} onChange={(e) => setEmail(e.target.value)} type="email" required />
          <label>Password</label>
          <input value={password} onChange={(e) => setPassword(e.target.value)} type="password" required />
          {error && <p className="error">{error}</p>}
          <button className="btn" style={{ marginTop: 16 }} disabled={loading}>
            {loading ? 'Signing in...' : 'Enter helpdesk'}
          </button>
        </form>
      </div>
    </div>
  )
}
