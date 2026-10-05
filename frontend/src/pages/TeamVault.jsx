
import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import API_URL from '../api'

function TeamVault() {
  const navigate = useNavigate()

  const [receivedShares, setReceivedShares] = useState([])
  const [ownedShares, setOwnedShares] = useState([])
  const [message, setMessage] = useState('')

  const token = localStorage.getItem('token')

  const fetchReceivedShares = async () => {
    try {
      const response = await fetch(
        `${API_URL}/api/vault/shares`,
        {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      if (response.ok) {
        const data = await response.json()
        setReceivedShares(data)
      }
    } catch (error) {
      console.error(error)
    }
  }

  const fetchOwnedShares = async () => {
    try {
      const response = await fetch(
        `${API_URL}/api/vault/shares/owned`,
        {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      if (response.ok) {
        const data = await response.json()
        setOwnedShares(data)
      }
    } catch (error) {
      console.error(error)
    }
  }

  useEffect(() => {
    fetchReceivedShares()
    fetchOwnedShares()
  }, [])

  const getShareStatus = (share) => {
    const now = new Date()
    const expiry = share.expiresAt
      ? new Date(share.expiresAt)
      : null

    if (expiry && expiry <= now) {
      return 'Expired'
    }

    if (share.active) {
      return 'Active'
    }

    return 'Revoked'
  }

  const revokeShare = async (shareId) => {
    const confirmed = window.confirm(
      'Are you sure you want to revoke this access?'
    )

    if (!confirmed) {
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/vault/shares/${shareId}`,
        {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.text()

      if (response.ok) {
        setMessage('Access revoked successfully')

        fetchOwnedShares()
        fetchReceivedShares()
      } else {
        setMessage(
          `Revoke failed: ${response.status} ${result}`
        )
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  const handleLogout = () => {
    localStorage.removeItem('token')
    navigate('/login')
  }

  return (
    <div className="vault-page">

      {/* SIDEBAR */}

      <aside className="vault-sidebar">

        <div className="sidebar-brand">

          <div className="sidebar-logo">
            🔐
          </div>

          <div>
            <h2>SecureVault</h2>
            <span>Security Dashboard</span>
          </div>

        </div>

        <nav className="sidebar-menu">

          <button
            className="sidebar-item"
            onClick={() => navigate('/vault')}
          >
            🗄️
            <span>My Vault</span>
          </button>

          <button
            className="sidebar-item active"
            onClick={() => navigate('/team-vault')}
          >
            👥
            <span>Team Vault</span>
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/password-generator')}
          >
            🔑
            <span>Password Tools</span>
          </button>

          <button
            className="sidebar-item"
            onClick={() => navigate('/login-history')}
          >
            🕘
            <span>Login History</span>
          </button>

        </nav>

        <div className="sidebar-bottom">

          <button
            className="sidebar-logout"
            onClick={handleLogout}
          >
            ↪
            <span>Logout</span>
          </button>

        </div>

      </aside>

      {/* MAIN */}

      <main className="vault-main">

        <div className="vault-header">

          <div>

            <p className="page-label">
              TEAM SECURITY
            </p>

            <h1>
              Team Vault
            </h1>

            <p className="page-description">
              Manage credentials shared with you and credentials you have shared.
            </p>

          </div>

          <div className="security-status">

            <span className="status-dot"></span>

            Protected

          </div>

        </div>

        {/* MESSAGE */}

        {message && (
          <div className="vault-message">
            {message}
          </div>
        )}

        {/* SHARED WITH ME */}

        <section className="dashboard-panel credentials-panel">

          <div className="panel-header">

            <div>

              <h2>
                Shared With Me
              </h2>

              <p>
                Credentials other users have shared with you.
              </p>

            </div>

            <span className="credential-count">
              {receivedShares.length} shared
            </span>

          </div>

          {receivedShares.length === 0 ? (

            <div className="empty-state">

              <div>
                🤝
              </div>

              <h3>
                No shared credentials
              </h3>

              <p>
                Credentials shared with you will appear here.
              </p>

            </div>

          ) : (

            <div className="credential-list">

              {receivedShares.map((share) => {

                const status = getShareStatus(share)

                return (
                  <div
                    className="credential-row"
                    key={share.id}
                  >

                    <div className="credential-logo shared-icon">
                      🤝
                    </div>

                    <div className="credential-info">

                      <h3>
                        {share.title}
                      </h3>

                      <p>
                        {share.username}
                      </p>

                    </div>

                    <div className="share-expiry">

                      <span>
                        Permission
                      </span>

                      <strong>
                        {share.permission || 'Not specified'}
                      </strong>

                    </div>

                    <div className="share-expiry">

                      <span>
                        Expires
                      </span>

                      <strong>
                        {share.expiresAt}
                      </strong>

                    </div>

                    <span
                      className={
                        status === 'Active'
                          ? 'active-badge'
                          : 'expired-badge'
                      }
                    >
                      {status}
                    </span>

                  </div>
                )
              })}

            </div>

          )}

        </section>

        {/* SHARED BY ME */}

        <section className="dashboard-panel credentials-panel">

          <div className="panel-header">

            <div>

              <h2>
                Shared By Me
              </h2>

              <p>
                Credentials you have shared with other users.
              </p>

            </div>

            <span className="credential-count">
              {ownedShares.length} shared
            </span>

          </div>

          {ownedShares.length === 0 ? (

            <div className="empty-state">

              <div>
                📤
              </div>

              <h3>
                No outgoing shares
              </h3>

              <p>
                Credentials you share with others will appear here.
              </p>

            </div>

          ) : (

            <div className="credential-list">

              {ownedShares.map((share) => {

                const status = getShareStatus(share)

                return (
                  <div
                    className="credential-row"
                    key={share.id}
                  >

                    <div className="credential-logo">
                      📤
                    </div>

                    <div className="credential-info">

                      <h3>
                        {share.title}
                      </h3>

                      <p>
                        {share.username}
                      </p>

                    </div>

                    <div className="share-expiry">

                      <span>
                        Permission
                      </span>

                      <strong>
                        {share.permission || 'Not specified'}
                      </strong>

                    </div>

                    <div className="share-expiry">

                      <span>
                        Expires
                      </span>

                      <strong>
                        {share.expiresAt}
                      </strong>

                    </div>

                    <span
                      className={
                        status === 'Active'
                          ? 'active-badge'
                          : 'expired-badge'
                      }
                    >
                      {status}
                    </span>

                    {status === 'Active' && (

                      <button
                        className="password-toggle"
                        onClick={() =>
                          revokeShare(share.id)
                        }
                      >
                        Revoke
                      </button>

                    )}

                  </div>
                )
              })}

            </div>

          )}

        </section>

      </main>

    </div>
  )
}

export default TeamVault

