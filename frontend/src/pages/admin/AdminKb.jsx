import { useEffect, useState } from 'react'
import api from '../../api/client'

export default function AdminKb() {
  const [docs, setDocs] = useState([])
  const [file, setFile] = useState(null)
  const [busy, setBusy] = useState(false)
  const [message, setMessage] = useState('')

  async function load() {
    const { data } = await api.get('/admin/kb')
    setDocs(data)
  }
  useEffect(() => { load() }, [])

  async function upload(e) {
    e.preventDefault()
    if (!file) return
    setBusy(true)
    setMessage('')
    try {
      const form = new FormData()
      form.append('file', file)
      await api.post('/admin/kb', form, { headers: { 'Content-Type': 'multipart/form-data' } })
      setFile(null)
      setMessage('File indexed and ready for RAG search.')
      await load()
    } finally {
      setBusy(false)
    }
  }

  async function loadDefaults() {
    setBusy(true)
    setMessage('')
    try {
      const { data } = await api.post('/admin/kb/defaults')
      setMessage(`Default company files: added ${data.loaded}, already present ${data.skipped}.`)
      await load()
    } finally {
      setBusy(false)
    }
  }

  async function remove(id) {
    await api.delete(`/admin/kb/${id}`)
    load()
  }

  return (
    <>
      <div className="topbar">
        <div>
          <h1>Knowledge base</h1>
          <div className="muted">
            Default Northwind IT runbooks load when the backend starts. Use the button below if they are missing,
            or upload extra company PDFs / DOCX / TXT / MD.
          </div>
        </div>
      </div>
      <div className="card" style={{ marginBottom: 16 }}>
        <p style={{ marginTop: 0 }}>
          Built-in files live in <code>backend/src/main/resources/kb-samples</code> (VPN, password, email, printer, Wi-Fi, hardware, access, software).
        </p>
        <button className="btn secondary" type="button" disabled={busy} onClick={loadDefaults}>
          {busy ? 'Working...' : 'Load default company documents'}
        </button>
        {message && <p className="muted">{message}</p>}
      </div>
      <form className="card" onSubmit={upload} style={{ marginBottom: 16 }}>
        <h3 style={{ marginTop: 0 }}>Upload an extra company file</h3>
        <input type="file" onChange={(e) => setFile(e.target.files[0])} accept=".pdf,.docx,.txt,.md" />
        <button className="btn" disabled={busy || !file} style={{ marginLeft: 8 }}>{busy ? 'Indexing...' : 'Upload and index'}</button>
      </form>
      <div className="card">
        <table>
          <thead><tr><th>Document</th><th>Chunks</th><th>Status</th><th></th></tr></thead>
          <tbody>
            {docs.map((d) => (
              <tr key={d.id}>
                <td>{d.originalName}</td>
                <td>{d.chunkCount}</td>
                <td>{d.status}</td>
                <td><button className="btn danger" onClick={() => remove(d.id)}>Delete</button></td>
              </tr>
            ))}
          </tbody>
        </table>
        {docs.length === 0 && <p className="muted">No documents yet. Click “Load default company documents” or upload a file.</p>}
      </div>
    </>
  )
}
