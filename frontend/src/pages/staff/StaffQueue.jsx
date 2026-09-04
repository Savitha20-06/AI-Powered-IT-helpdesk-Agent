import { useEffect, useMemo, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/client'

export default function StaffQueue() {
  const [tickets, setTickets] = useState([])
  useEffect(() => {
    api.get('/staff/tickets').then((res) => setTickets(res.data))
  }, [])

  async function remove(id) {
    if (!window.confirm('Are you sure you want to delete this?')) return
    await api.delete(`/staff/tickets/${id}`)
    setTickets((current) => current.filter((ticket) => ticket.id !== id))
  }

  const stats = useMemo(() => {
    const open = tickets.filter((t) => ['OPEN', 'ASSIGNED', 'IN_PROGRESS'].includes(t.status)).length
    const critical = tickets.filter((t) => t.priority === 'CRITICAL' || t.priority === 'HIGH').length
    const ai = tickets.filter((t) => t.source === 'AI_ESCALATION').length
    return { open, critical, ai, total: tickets.length }
  }, [tickets])

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Support queue</h1>
          <div className="muted">Assign, update, resolve, and close employee tickets.</div>
        </div>
      </div>
      <div className="grid stats">
        <div className="card"><div className="muted">Visible tickets</div><div className="stat">{stats.total}</div></div>
        <div className="card"><div className="muted">Still open</div><div className="stat">{stats.open}</div></div>
        <div className="card"><div className="muted">High / Critical</div><div className="stat">{stats.critical}</div></div>
        <div className="card"><div className="muted">AI escalations</div><div className="stat">{stats.ai}</div></div>
      </div>
      <div className="card" style={{ marginTop: 16 }}>
        <table>
          <thead>
            <tr><th>Number</th><th>Employee</th><th>Category</th><th>Priority</th><th>Status</th><th>Source</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {tickets.map((t) => (
              <tr key={t.id}>
                <td><Link to={`/staff/tickets/${t.id}`}>{t.ticketNumber}</Link></td>
                <td>{t.employee?.fullName}</td>
                <td>{t.category}</td>
                <td><span className={`badge ${t.priority}`}>{t.priority}</span></td>
                <td><span className={`badge ${t.status}`}>{t.status}</span></td>
                <td>{t.source}</td>
                <td><button className="btn danger icon-btn" title="Delete ticket" aria-label="Delete ticket" onClick={() => remove(t.id)}>🗑️</button></td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </>
  )
}
