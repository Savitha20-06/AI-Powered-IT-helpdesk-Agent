import { useEffect, useState } from 'react'
import api from '../../api/client'

function Bars({ data }) {
  const entries = Object.entries(data || {})
  const max = Math.max(1, ...entries.map(([, v]) => v))
  return (
    <div>
      {entries.map(([k, v]) => (
        <div key={k} style={{ margin: '8px 0' }}>
          <div className="muted">{k} · {v}</div>
          <div style={{ background: '#e8eef6', borderRadius: 99, height: 10 }}>
            <div style={{ width: `${(v / max) * 100}%`, background: '#0f8f86', height: 10, borderRadius: 99 }} />
          </div>
        </div>
      ))}
      {entries.length === 0 && <p className="muted">No data yet. Ask the assistant a question that should escalate.</p>}
    </div>
  )
}

export default function AdminHome() {
  const [data, setData] = useState(null)
  useEffect(() => {
    api.get('/admin/analytics').then((res) => setData(res.data))
  }, [])
  if (!data) return <p>Loading analytics...</p>
  return (
    <>
      <div className="topbar">
        <div>
          <h1>System analytics</h1>
          <div className="muted">Users, tickets, AI volume, and knowledge-base coverage.</div>
        </div>
      </div>
      <div className="grid stats">
        <div className="card"><div className="muted">Users</div><div className="stat">{data.totalUsers}</div></div>
        <div className="card"><div className="muted">Tickets</div><div className="stat">{data.totalTickets}</div></div>
        <div className="card"><div className="muted">Open work</div><div className="stat">{data.openTickets}</div></div>
        <div className="card"><div className="muted">Resolved / closed</div><div className="stat">{data.resolvedTickets}</div></div>
      </div>
      <div className="grid stats" style={{ marginTop: 16 }}>
        <div className="card"><div className="muted">AI escalations</div><div className="stat">{data.aiEscalations}</div></div>
        <div className="card"><div className="muted">AI answers</div><div className="stat">{data.assistantMessages}</div></div>
        <div className="card"><div className="muted">KB documents</div><div className="stat">{data.kbDocuments}</div></div>
        <div className="card"><div className="muted">KB chunks</div><div className="stat">{data.kbChunks}</div></div>
      </div>
      <div className="grid two" style={{ marginTop: 16 }}>
        <div className="card"><h3>By status</h3><Bars data={data.ticketsByStatus} /></div>
        <div className="card"><h3>By priority</h3><Bars data={data.ticketsByPriority} /></div>
      </div>
      <div className="card" style={{ marginTop: 16 }}>
        <h3>By category</h3>
        <Bars data={data.ticketsByCategory} />
      </div>
    </>
  )
}
