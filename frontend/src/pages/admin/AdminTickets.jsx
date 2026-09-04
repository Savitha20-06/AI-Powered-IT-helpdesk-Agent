import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/client'

export default function AdminTickets() {
  const [tickets, setTickets] = useState([])
  useEffect(() => {
    api.get('/staff/tickets').then((res) => setTickets(res.data))
  }, [])

  async function remove(id) {
    if (!window.confirm('Are you sure you want to delete this?')) return
    await api.delete(`/staff/tickets/${id}`)
    setTickets((current) => current.filter((ticket) => ticket.id !== id))
  }
  return (
    <>
      <div className="topbar"><h1>All tickets</h1></div>
      <div className="card">
        <table>
          <thead>
            <tr><th>Number</th><th>Employee</th><th>Assignee</th><th>Priority</th><th>Status</th><th>Source</th><th>Actions</th></tr>
          </thead>
          <tbody>
            {tickets.map((t) => (
              <tr key={t.id}>
                <td><Link to={`/admin/tickets/${t.id}`}>{t.ticketNumber}</Link></td>
                <td>{t.employee?.fullName}</td>
                <td>{t.assignedTo?.fullName || 'Unassigned'}</td>
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
