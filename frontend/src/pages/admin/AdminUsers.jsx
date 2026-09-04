import { useEffect, useState } from 'react'
import api from '../../api/client'
import { useAuth } from '../../auth/AuthContext.jsx'

export default function AdminUsers() {
  const { user: currentUser } = useAuth()
  const [users, setUsers] = useState([])
  const [form, setForm] = useState({ email: '', password: 'Password@123', fullName: '', department: '', role: 'EMPLOYEE' })
  const [error, setError] = useState('')
  const [savingId, setSavingId] = useState(null)

  async function load() {
    const { data } = await api.get('/admin/users')
    setUsers(data)
  }
  useEffect(() => { load() }, [])

  async function create(e) {
    e.preventDefault()
    await api.post('/admin/users', form)
    setForm({ ...form, email: '', fullName: '' })
    load()
  }

  async function toggle(user) {
    setError('')
    if (currentUser?.userId === user.id && user.enabled) {
      setError('You cannot disable your own admin account.')
      return
    }
    setSavingId(user.id)
    try {
      await api.put(`/admin/users/${user.id}`, { enabled: !user.enabled })
      await load()
    } catch (err) {
      setError(err.response?.data?.detail || err.response?.data?.message || 'Unable to update user status.')
    } finally {
      setSavingId(null)
    }
  }

  return (
    <>
      <div className="topbar"><h1>Users and roles</h1></div>
      {error && <p className="error">{error}</p>}
      <div className="grid two">
        <div className="card">
          <table>
            <thead><tr><th>Name</th><th>Email</th><th>Role</th><th>Status</th><th></th></tr></thead>
            <tbody>
              {users.map((u) => (
                <tr key={u.id}>
                  <td>{u.fullName}</td>
                  <td>{u.email}</td>
                  <td>{u.role}</td>
                  <td>{u.enabled ? 'Active' : 'Disabled'}</td>
                  <td><button className="btn ghost" disabled={savingId === u.id} onClick={() => toggle(u)}>{savingId === u.id ? 'Saving...' : u.enabled ? 'Disable' : 'Enable'}</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <form className="card" onSubmit={create}>
          <h3>Create user</h3>
          <label>Full name</label>
          <input value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} required />
          <label>Email</label>
          <input type="email" value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} required />
          <label>Department</label>
          <input value={form.department} onChange={(e) => setForm({ ...form, department: e.target.value })} />
          <label>Role</label>
          <select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value })}>
            <option>EMPLOYEE</option>
            <option>IT_STAFF</option>
            <option>ADMIN</option>
          </select>
          <label>Password</label>
          <input value={form.password} onChange={(e) => setForm({ ...form, password: e.target.value })} required />
          <button className="btn" style={{ marginTop: 12 }}>Add user</button>
        </form>
      </div>
    </>
  )
}
