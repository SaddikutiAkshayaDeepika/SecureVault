import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import API_URL from '../api'

function SecureNotes() {
  const navigate = useNavigate()

  const [notes, setNotes] = useState([])
  const [title, setTitle] = useState('')
  const [content, setContent] = useState('')
  const [message, setMessage] = useState('')
  const [loading, setLoading] = useState(true)
  const [editingNote, setEditingNote] = useState(null)
  const [editTitle, setEditTitle] = useState('')
  const [editContent, setEditContent] = useState('')

  const token = localStorage.getItem('token')

  const fetchNotes = async () => {
    try {
      const response = await fetch(
        `${API_URL}/api/secure-notes`,
        {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.text()

      if (response.ok) {
        setNotes(JSON.parse(result))
      } else if (
        response.status === 401 ||
        response.status === 403
      ) {
        localStorage.removeItem('token')
        navigate('/login')
      } else {
        setMessage(`Load failed: ${response.status} ${result}`)
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchNotes()
  }, [])

  const handleAddNote = async (e) => {
    e.preventDefault()

    if (!title.trim() || !content.trim()) {
      setMessage('Title and content are required')
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/secure-notes`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            title,
            content,
          }),
        }
      )

      const result = await response.text()

      if (response.ok) {
        setMessage('Secure note added successfully')
        setTitle('')
        setContent('')
        fetchNotes()
      } else {
        setMessage(`Add failed: ${response.status} ${result}`)
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  const startEdit = (note) => {
    setEditingNote(note.id)
    setEditTitle(note.title)
    setEditContent(note.content)
    setMessage('')
  }

  const cancelEdit = () => {
    setEditingNote(null)
    setEditTitle('')
    setEditContent('')
  }

  const handleUpdateNote = async (id) => {
    if (!editTitle.trim() || !editContent.trim()) {
      setMessage('Title and content are required')
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/secure-notes/${id}`,
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify({
            title: editTitle,
            content: editContent,
          }),
        }
      )

      const result = await response.text()

      if (response.ok) {
        setMessage('Secure note updated successfully')
        cancelEdit()
        fetchNotes()
      } else {
        setMessage(`Update failed: ${response.status} ${result}`)
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  const handleDeleteNote = async (id) => {
    const confirmed = window.confirm(
      'Are you sure you want to delete this secure note?'
    )

    if (!confirmed) {
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/secure-notes/${id}`,
        {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.text()

      if (response.ok) {
        setMessage('Secure note deleted successfully')
        fetchNotes()
      } else {
        setMessage(`Delete failed: ${response.status} ${result}`)
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  return (
    <div className="vault-page">

      <aside className="vault-sidebar">
        <div className="sidebar-brand">
          <div className="sidebar-logo">
            🔐
          </div>
          <h2>SecureVault</h2>
        </div>

        <nav className="sidebar-menu">

          <button
            className="sidebar-item"
            onClick={() => navigate('/vault')}
          >
            🔑 Vault
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/team-vault')}
          >
            👥 Team Vault
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/secure-notes')}
          >
            📝 Secure Notes
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/password-generator')}
          >
            🔄 Password Generator
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/mfa')}
          >
            🛡️ MFA
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/login-history')}
          >
            📋 Login History
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/security-analytics')}
          >
            📊 Security Analytics
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/sessions')}
          >
            💻 Sessions
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/notifications')}
          >
            🔔 Notifications
          </button>

        </nav>

        <div className="sidebar-bottom">
          <button
            className="sidebar-logout"
            onClick={() => {
              localStorage.removeItem('token')
              navigate('/login')
            }}
          >
            Logout
          </button>
        </div>
      </aside>

      <main className="vault-main">

        <div className="vault-header">
          <h1>Secure Notes</h1>
          <p>Store your sensitive notes securely.</p>
        </div>

        {message && (
          <div className="vault-message">
            {message}
          </div>
        )}

        <div className="secure-note-form">

          <h2>Add Secure Note</h2>

          <form onSubmit={handleAddNote}>

            <label>Title</label>

            <input
              type="text"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="Note title"
            />

            <label>Content</label>

            <textarea
              value={content}
              onChange={(e) => setContent(e.target.value)}
              placeholder="Write your secure note..."
              rows="8"
            />

            <button
              type="submit"
              className="primary-action"
            >
              Add Note
            </button>

          </form>

        </div>

        <div className="secure-notes-list">

          <h2>Your Secure Notes</h2>

          {loading ? (
            <p>Loading notes...</p>
          ) : notes.length === 0 ? (
            <p>No secure notes yet.</p>
          ) : (
            notes.map((note) => (
              <div
                className="secure-note-card"
                key={note.id}
              >
                {editingNote === note.id ? (
                  <>
                    <input
                      type="text"
                      value={editTitle}
                      onChange={(e) => setEditTitle(e.target.value)}
                      placeholder="Note title"
                    />

                    <textarea
                      value={editContent}
                      onChange={(e) => setEditContent(e.target.value)}
                      rows="6"
                      placeholder="Write your secure note..."
                    />

                    <div className="secure-note-actions">
                      <button
                        type="button"
                        className="primary-action"
                        onClick={() => handleUpdateNote(note.id)}
                      >
                        Save
                      </button>

                      <button
                        type="button"
                        onClick={cancelEdit}
                      >
                        Cancel
                      </button>
                    </div>
                  </>
                ) : (
                  <>
                    <h3>{note.title}</h3>

                    <p>{note.content}</p>

                    <small>
                      Updated: {new Date(note.updatedAt).toLocaleString()}
                    </small>

                    <div className="secure-note-actions">
                      <button
                        type="button"
                        onClick={() => startEdit(note)}
                      >
                        ✏️ Edit
                      </button>

                      <button
                        type="button"
                        onClick={() => handleDeleteNote(note.id)}
                      >
                        🗑️ Delete
                      </button>
                    </div>
                  </>
                )}
              </div>
            ))
          )}

        </div>

      </main>

    </div>
  )
}

export default SecureNotes