import { useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import api from '../api/client'
import { useAuth } from '../auth/AuthContext.jsx'

export default function TicketPage({ staff = false }) {
  const { id } = useParams()
  const navigate = useNavigate()
  const { user } = useAuth()
  const [ticket, setTicket] = useState(null)
  const [comment, setComment] = useState('')
  const [assignees, setAssignees] = useState([])
  const [assigneeId, setAssigneeId] = useState('')
  const [status, setStatus] = useState('OPEN')

  async function load() {
    const { data } = await api.get(`/tickets/${id}`)
    setTicket(data)
    setStatus(data.status)
    if (data.assignedTo) setAssigneeId(String(data.assignedTo.id))
  }

  useEffect(() => {
    load()
    if (staff) {
      api.get('/staff/tickets/assignees').then((res) => setAssignees(res.data)).catch(() => {})
    }
  }, [id, staff])

  async function addComment(e) {
    e.preventDefault()
    await api.post(`/tickets/${id}/comments`, { body: comment })
    setComment('')
    load()
  }

  async function assign(e) {
    e.preventDefault()
    await api.patch(`/staff/tickets/${id}/assign`, { assigneeId: Number(assigneeId) })
    load()
  }

  async function changeStatus(e) {
    e.preventDefault()
    await api.patch(`/staff/tickets/${id}/status`, { status })
    load()
  }

  async function remove() {
    if (!window.confirm('Are you sure you want to delete this?')) return
    await api.delete(`${staff ? '/staff/tickets' : '/tickets'}/${id}`)
    navigate(staff ? (user.role === 'ADMIN' ? '/admin/tickets' : '/staff') : '/app/tickets')
  }

  if (!ticket) return <p>Loading ticket...</p>
  const canStaff = staff && (user.role === 'IT_STAFF' || user.role === 'ADMIN')

  return (
    <>
      <div className="topbar">
        <div>
          <h1>{ticket.ticketNumber}</h1>
          <div className="muted">{ticket.title}</div>
        </div>
        <div className="row">
          <span className={`badge ${ticket.priority}`}>{ticket.priority}</span>
          <span className={`badge ${ticket.status}`}>{ticket.status}</span>
          <span className="badge">{ticket.category}</span>
          <span className="badge">{ticket.source}</span>
          <button className="btn danger icon-btn" title="Delete ticket" aria-label="Delete ticket" onClick={remove}>🗑️</button>
        </div>
      </div>
      <div className="grid two">
        <div className="card">
          <h3>Problem summary</h3>
          <p>{ticket.summary}</p>
          <h3>Symptoms</h3>
          <p>{ticket.symptoms}</p>
          <h3>Troubleshooting already attempted</h3>
          <p>{ticket.troubleshootingAttempted}</p>
          <h3>Relevant knowledge base</h3>
          <p style={{ whiteSpace: 'pre-wrap' }}>{ticket.kbContext || 'None captured'}</p>
        </div>
        <div>
          {canStaff && (
            <div className="card" style={{ marginBottom: 16 }}>
              <h3>IT workspace</h3>
              <form onSubmit={assign}>
                <label>Assign to</label>
                <select value={assigneeId} onChange={(e) => setAssigneeId(e.target.value)}>
                  <option value="">Select staff</option>
                  {assignees.map((a) => <option key={a.id} value={a.id}>{a.fullName}</option>)}
                </select>
                <button className="btn" style={{ marginTop: 8 }}>Assign</button>
              </form>
              <form onSubmit={changeStatus} style={{ marginTop: 12 }}>
                <label>Status</label>
                <select value={status} onChange={(e) => setStatus(e.target.value)}>
                  {['OPEN','ASSIGNED','IN_PROGRESS','RESOLVED','CLOSED'].map((s) => <option key={s}>{s}</option>)}
                </select>
                <button className="btn secondary" style={{ marginTop: 8 }}>Update status</button>
              </form>
            </div>
          )}
          <div className="card">
            <h3>Comments</h3>
            {ticket.comments.map((c) => (
              <p key={c.id}><strong>{c.author.fullName}:</strong> {c.body}</p>
            ))}
            <form onSubmit={addComment}>
              <textarea value={comment} onChange={(e) => setComment(e.target.value)} required />
              <button className="btn" style={{ marginTop: 8 }}>Add comment</button>
            </form>
          </div>
        </div>
      </div>
    </>
  )
}
