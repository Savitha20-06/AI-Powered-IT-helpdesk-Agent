import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/client'

export default function EmployeeTickets() {
  const [tickets, setTickets] = useState([])
  const [title, setTitle] = useState('')
  const [summary, setSummary] = useState('')
  const [category, setCategory] = useState('SOFTWARE')
  const [priority, setPriority] = useState('MEDIUM')

  async function load() {
    const { data } = await api.get('/tickets')
    setTickets(data)
  }
  useEffect(() => { load() }, [])

  async function create(e) {
    e.preventDefault()
    await api.post('/tickets', { title, summary, symptoms: summary, category, priority })
    setTitle(''); setSummary('')
    load()
  }

  async function remove(id) {
    if (!window.confirm('Are you sure you want to delete this?')) return
    await api.delete(`/tickets/${id}`)
    setTickets((current) => current.filter((ticket) => ticket.id !== id))
  }

  return (
    <>
      <div className="topbar">
        <div>
          <h1>My tickets</h1>
          <div className="muted">AI-escalated and manually created tickets for your account.</div>
        </div>
      </div>
      <div className="grid two">
        <div className="card">
          <table>
            <thead>
              <tr><th>Number</th><th>Title</th><th>Priority</th><th>Status</th><th>Actions</th></tr>
            </thead>
            <tbody>
              {tickets.map((t) => (
                <tr key={t.id}>
                  <td><Link to={`/app/tickets/${t.id}`}>{t.ticketNumber}</Link></td>
                  <td>{t.title}</td>
                  <td><span className={`badge ${t.priority}`}>{t.priority}</span></td>
                  <td><span className={`badge ${t.status}`}>{t.status}</span></td>
                  <td><button className="btn danger icon-btn" title="Delete ticket" aria-label="Delete ticket" onClick={() => remove(t.id)}>🗑️</button></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
        <form className="card" onSubmit={create}>
          <h3>Create a ticket</h3>
          <label>Title</label>
          <input value={title} onChange={(e) => setTitle(e.target.value)} required />
          <label>Summary / symptoms</label>
          <textarea value={summary} onChange={(e) => setSummary(e.target.value)} required />
          <label>Category</label>
          <select value={category} onChange={(e) => setCategory(e.target.value)}>
            {['HARDWARE','SOFTWARE','NETWORK','VPN','EMAIL','PASSWORD','PRINTER','ACCESS','OTHER'].map((c) => <option key={c}>{c}</option>)}
          </select>
          <label>Priority</label>
          <select value={priority} onChange={(e) => setPriority(e.target.value)}>
            {['LOW','MEDIUM','HIGH','CRITICAL'].map((c) => <option key={c}>{c}</option>)}
          </select>
          <button className="btn" style={{ marginTop: 12 }}>Submit ticket</button>
        </form>
      </div>
    </>
  )
}
