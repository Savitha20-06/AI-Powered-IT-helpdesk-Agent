import { useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import api from '../../api/client'

export default function ChatPage({ adminMode = false, routePrefix = adminMode ? '/admin' : '/app', title = adminMode ? 'Admin AI Assistant' : 'AI IT Assistant' }) {
  const [conversations, setConversations] = useState([])
  const [activeId, setActiveId] = useState(null)
  const [messages, setMessages] = useState([])
  const [question, setQuestion] = useState('')
  const [busy, setBusy] = useState(false)
  const [ticket, setTicket] = useState(null)
  const [ticketRecommended, setTicketRecommended] = useState(false)
  const [deleteError, setDeleteError] = useState('')
  const [historyLoading, setHistoryLoading] = useState(true)
  const [historyDeleting, setHistoryDeleting] = useState(false)
  const [historyError, setHistoryError] = useState('')

  async function loadConversations() {
    setHistoryLoading(true)
    setHistoryError('')
    try {
      const { data } = await api.get('/chat/conversations')
      setConversations(data)
    } catch (err) {
      setHistoryError(err.response?.data?.detail || err.response?.data?.message || 'Unable to load chat history.')
    } finally {
      setHistoryLoading(false)
    }
  }

  async function openConversation(id) {
    setActiveId(id)
    setTicket(null)
    setTicketRecommended(false)
    const { data } = await api.get(`/chat/conversations/${id}/messages`)
    setMessages(data)
  }

  async function deleteConversation(event, id) {
    event.stopPropagation()
    if (!window.confirm('Are you sure you want to delete this?')) return
    setDeleteError('')
    try {
      await api.delete(`/chat/conversations/${id}`)
      setConversations((prev) => prev.filter((conversation) => conversation.id !== id))
      if (activeId === id) {
        setActiveId(null)
        setMessages([])
        setTicket(null)
        setTicketRecommended(false)
      }
    } catch (err) {
      setDeleteError(err.response?.data?.detail || err.response?.data?.message || 'Unable to delete this conversation.')
    }
  }

  async function deleteAllConversations() {
    if (!conversations.length || !window.confirm('Delete all chat history? This cannot be undone.')) return
    setHistoryDeleting(true)
    setHistoryError('')
    try {
      await api.delete('/chat/conversations')
      setConversations([])
      setActiveId(null)
      setMessages([])
      setTicket(null)
      setTicketRecommended(false)
    } catch (err) {
      setHistoryError(err.response?.data?.detail || err.response?.data?.message || 'Unable to delete chat history.')
    } finally {
      setHistoryDeleting(false)
    }
  }

  useEffect(() => { loadConversations() }, [])

  async function send(e) {
    e.preventDefault()
    if (!question.trim()) return
    setBusy(true)
    try {
      const { data } = await api.post('/chat/ask', { conversationId: activeId, question })
      setActiveId(data.conversation.id)
      setMessages((prev) => activeId ? [...prev, data.userMessage, data.assistantMessage] : [data.userMessage, data.assistantMessage])
      setTicket(data.createdTicket)
      setTicketRecommended(data.ticketRecommended)
      setQuestion('')
      await loadConversations()
      if (activeId) {
        const res = await api.get(`/chat/conversations/${data.conversation.id}/messages`)
        setMessages(res.data)
      }
    } finally {
      setBusy(false)
    }
  }

  return (
    <>
      <div className="topbar">
        <div>
          <h1>{title}</h1>
          <div className="muted">Answers come only from approved company documents. Weak matches create a ticket.</div>
        </div>
      </div>
      <div className="chat-layout">
        <div className="card">
          <div className="history-heading">
            <h2>Chat history</h2>
          </div>
          <button className="btn secondary" onClick={() => { setActiveId(null); setMessages([]); setTicket(null); setTicketRecommended(false) }}>New conversation</button>
          {historyLoading && <p className="muted">Loading history...</p>}
          {!historyLoading && !conversations.length && <p className="muted">No previous questions yet.</p>}
          {historyError && <p className="error">{historyError}</p>}
          {deleteError && <p className="error">{deleteError}</p>}
          <div style={{ marginTop: 12 }}>
            {conversations.map((c) => (
              <div key={c.id} className={`conv ${c.id === activeId ? 'active' : ''}`}>
                <button className="conv-title" onClick={() => openConversation(c.id)}>{c.title}</button>
                <button
                  className="conv-delete"
                  aria-label={`Delete conversation: ${c.title}`}
                  title="Delete conversation"
                  onClick={(event) => deleteConversation(event, c.id)}
                >
                  🗑️
                </button>
              </div>
            ))}
          </div>
          <button className="btn danger history-delete-all" disabled={historyDeleting || !conversations.length} onClick={deleteAllConversations}>
            {historyDeleting ? 'Deleting...' : 'Delete all history'}
          </button>
        </div>
        <div className="card chat-thread">
          <div className="messages">
            {messages.length === 0 && <p className="muted">Ask about VPN, passwords, email, printers, Wi-Fi, hardware, software, or access.</p>}
            {messages.map((m) => (
              <div key={m.id} className={`bubble ${m.role}`}>
                {m.content}
                {m.role === 'assistant' && (
                  <div className="muted" style={{ marginTop: 8, fontSize: 12 }}>
                    {m.category && <span className="badge">{m.category}</span>} {m.priority && <span className={`badge ${m.priority}`}>{m.priority}</span>}
                    {m.usedRag ? ' · RAG used' : ' · no KB hit'}
                    {m.escalated ? ' · escalated' : ''}
                  </div>
                )}
              </div>
            ))}
          </div>
          {ticket && (
            <p>Escalated to <Link to={`${routePrefix}/tickets/${ticket.id}`}>{ticket.ticketNumber}</Link></p>
          )}
          {ticketRecommended && (
            <p><Link className="btn" to={`${routePrefix}/tickets`}>Create Ticket</Link></p>
          )}
          <form className="composer" onSubmit={send}>
            <textarea value={question} onChange={(e) => setQuestion(e.target.value)} placeholder="Describe the IT issue..." />
            <button className="btn" disabled={busy}>{busy ? 'Thinking...' : 'Ask'}</button>
          </form>
        </div>
      </div>
    </>
  )
}
