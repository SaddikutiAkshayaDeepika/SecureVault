
import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import API_URL from '../api'

function Vault() {
  const navigate = useNavigate()
  const location = useLocation()

  const [credentials, setCredentials] = useState([])
  const [sharedCredentials, setSharedCredentials] = useState([])

  // Share states
  const [shareCredentialId, setShareCredentialId] = useState('')
  const [recipientEmail, setRecipientEmail] = useState('')
  const [expiresAt, setExpiresAt] = useState('')
  const [permission, setPermission] = useState('VIEW_ONLY')

  // Add credential states
  const [title, setTitle] = useState('')
  const [username, setUsername] = useState('')
  const [password, setPassword] = useState('')
  const [category, setCategory] = useState('')
  const [credentialType, setCredentialType] = useState('')
  const [favorite, setFavorite] = useState(false)
  const [message, setMessage] = useState('')
  const [strength, setStrength] = useState('')
  const [showPassword, setShowPassword] = useState({})

  // Add credential visibility
  const [showAddCredential, setShowAddCredential] = useState(true)

  const [search, setSearch] = useState('')
  const [categoryFilter, setCategoryFilter] = useState('')
  const [credentialTypeFilter, setCredentialTypeFilter] = useState('')
  const [favoriteFilter, setFavoriteFilter] = useState(false)

  // Edit states
  const [editingCredential, setEditingCredential] = useState(null)
  const [editTitle, setEditTitle] = useState('')
  const [editUsername, setEditUsername] = useState('')
  const [editPassword, setEditPassword] = useState('')
  const [editCategory, setEditCategory] = useState('')
  const [editCredentialType, setEditCredentialType] = useState('')
  const [editFavorite, setEditFavorite] = useState(false)
  const [editStrength, setEditStrength] = useState('')
  const [showEditPassword, setShowEditPassword] = useState(false)

  // Check active sidebar page
  const isActive = (path) => {
    return location.pathname === path
  }

  // Logout
  const handleLogout = () => {
    localStorage.removeItem('token')
    navigate('/login')
  }

  // Fetch my credentials
  const fetchCredentials = async () => {
    const token = localStorage.getItem('token')

    try {
      const response = await fetch(
        `${API_URL}/api/vault/credentials`,
        {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.text()

      if (response.ok) {
        setCredentials(JSON.parse(result))
      } else if (
        response.status === 401 ||
        response.status === 403
      ) {
        setMessage('Session expired. Please login again.')
        localStorage.removeItem('token')
        navigate('/login')
      } else {
        setMessage(
          `Load failed: ${response.status} ${result}`
        )
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  // Search credentials using backend
  const searchCredentials = async (query) => {
    const token = localStorage.getItem('token')

    if (!query.trim()) {
      fetchCredentials()
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/vault/credentials/search?query=${encodeURIComponent(query)}`,
        {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.text()

      if (response.ok) {
        setCredentials(JSON.parse(result))
      } else if (
        response.status === 401 ||
        response.status === 403
      ) {
        setMessage('Session expired. Please login again.')
        localStorage.removeItem('token')
        navigate('/login')
      } else {
        setMessage(
          `Search failed: ${response.status} ${result}`
        )
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  // Fetch credentials shared with me
  const fetchSharedCredentials = async () => {
    const token = localStorage.getItem('token')

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

      const result = await response.text()

      if (response.ok) {
        setSharedCredentials(JSON.parse(result))
      } else if (
        response.status === 401 ||
        response.status === 403
      ) {
        setMessage('Unable to load shared credentials')
      }
    } catch (error) {
      console.error(error)
    }
  }

  useEffect(() => {
    fetchCredentials()
    fetchSharedCredentials()
  }, [])

  // Password strength
  const checkPasswordStrength = (value) => {
    setPassword(value)

    if (value.length < 6) {
      setStrength('Weak')
    } else if (
      value.length >= 8 &&
      /[A-Z]/.test(value) &&
      /[a-z]/.test(value) &&
      /[0-9]/.test(value) &&
      /[^A-Za-z0-9]/.test(value)
    ) {
      setStrength('Strong')
    } else {
      setStrength('Medium')
    }
  }

  // Edit password strength
  const checkEditPasswordStrength = (value) => {
    setEditPassword(value)

    if (value.length < 6) {
      setEditStrength('Weak')
    } else if (
      value.length >= 8 &&
      /[A-Z]/.test(value) &&
      /[a-z]/.test(value) &&
      /[0-9]/.test(value) &&
      /[^A-Za-z0-9]/.test(value)
    ) {
      setEditStrength('Strong')
    } else {
      setEditStrength('Medium')
    }
  }

  // Generate password
  const generatePassword = async () => {
    try {
      const response = await fetch(
        `${API_URL}/api/password/generate?length=16&uppercase=true&lowercase=true&numbers=true&special=true`
      )

      if (response.ok) {
        const generatedPassword = await response.text()

        checkPasswordStrength(generatedPassword)

        setMessage('Password generated successfully')
      } else {
        const result = await response.text()

        setMessage(
          `Generation failed: ${response.status} ${result}`
        )
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  // Add credential
  const handleAddCredential = async (e) => {
    e.preventDefault()

    const token = localStorage.getItem('token')

    const credentialData = {
      title: title,
      username: username,
      password: password,
      category: category,
      credentialType: credentialType,
      favorite: favorite,
    }

    try {
      const response = await fetch(
        `${API_URL}/api/vault/credentials`,
        {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(credentialData),
        }
      )

      const result = await response.text()

      if (response.ok) {
        setMessage('Credential saved successfully')

        setTitle('')
        setUsername('')
        setPassword('')
        setCategory('')
        setCredentialType('')
        setFavorite(false)
        setStrength('')

        fetchCredentials()
      } else if (
        response.status === 401 ||
        response.status === 403
      ) {
        setMessage(
          'You are not authorized to save credentials.'
        )
      } else {
        setMessage(
          `Save failed: ${response.status} ${result}`
        )
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  // Show/hide password
  const togglePassword = (id) => {
    setShowPassword((previous) => ({
      ...previous,
      [id]: !previous[id],
    }))
  }

  // Copy password
  const copyPassword = async (password) => {
    if (
      password === null ||
      password === undefined
    ) {
      setMessage('Password is unavailable')
      return
    }

    try {
      await navigator.clipboard.writeText(password)
      setMessage('Password copied to clipboard')
    } catch (error) {
      console.error(error)
      setMessage('Unable to copy password')
    }
  }

  // Delete credential
  const deleteCredential = async (id) => {
    const token = localStorage.getItem('token')

    const confirmed = window.confirm(
      'Are you sure you want to delete this credential?'
    )

    if (!confirmed) {
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/vault/credentials/${id}`,
        {
          method: 'DELETE',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.text()

      if (response.ok) {
        setMessage('Credential deleted successfully')

        fetchCredentials()
      } else if (response.status === 403) {
        setMessage(
          'You do not have permission to delete this credential.'
        )
      } else {
        setMessage(
          `Delete failed: ${response.status} ${result}`
        )
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  // Open edit form
  const openEditCredential = (credential) => {
    if (credential.password === null) {
      setMessage(
        'Password is unavailable for this credential.'
      )
      return
    }

    setEditingCredential(credential)

    setEditTitle(credential.title)
    setEditUsername(credential.username)
    setEditPassword(credential.password)

    setEditStrength('')
    setShowEditPassword(false)

    setMessage('')
  }

  // Cancel edit
  const cancelEdit = () => {
    setEditingCredential(null)

    setEditTitle('')
    setEditUsername('')
    setEditPassword('')
    setEditCategory('')
    setEditCredentialType('')
    setEditFavorite(false)
    setEditStrength('')
    setShowEditPassword(false)
  }

  // Update credential
  const handleUpdateCredential = async (e) => {
    e.preventDefault()

    const token = localStorage.getItem('token')

    const credentialData = {
      title: editTitle,
      username: editUsername,
      password: editPassword,
      category: editCategory,
      credentialType: editCredentialType,
      favorite: editFavorite,
    }

    try {
      const response = await fetch(
        `${API_URL}/api/vault/credentials/${editingCredential.id}`,
        {
          method: 'PUT',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`,
          },
          body: JSON.stringify(credentialData),
        }
      )

      const result = await response.text()

      if (response.ok) {
        setMessage('Credential updated successfully')

        cancelEdit()

        fetchCredentials()
      } else if (response.status === 403) {
        setMessage(
          'You do not have permission to edit this credential.'
        )
      } else {
        setMessage(
          `Update failed: ${response.status} ${result}`
        )
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  // Share credential
  const handleShare = async (e) => {
    e.preventDefault()

    const token = localStorage.getItem('token')

    if (
      !shareCredentialId ||
      !recipientEmail ||
      !expiresAt ||
      !permission
    ) {
      setMessage('Please fill all share details')
      return
    }

    const selectedCredential = credentials.find(
      (credential) =>
        String(credential.id) ===
        String(shareCredentialId)
    )

    if (
      !selectedCredential ||
      selectedCredential.password === null
    ) {
      setMessage(
        'This credential cannot be shared because its password is unavailable.'
      )
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/vault/shares?credentialId=${shareCredentialId}&recipientEmail=${encodeURIComponent(
          recipientEmail
        )}&expiresAt=${encodeURIComponent(
          expiresAt
        )}&permission=${encodeURIComponent(
          permission
        )}`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.text()

      if (response.ok) {
        setMessage('Credential shared successfully')

        setShareCredentialId('')
        setRecipientEmail('')
        setExpiresAt('')
        setPermission('VIEW_ONLY')

        fetchSharedCredentials()
      } else {
        setMessage(
          `Share failed: ${response.status} ${result}`
        )
      }
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
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
            className={`sidebar-item ${
              isActive('/vault') ? 'active' : ''
            }`}
            onClick={() =>
              navigate('/vault')
            }
          >
            🗄️
            <span>My Vault</span>
          </button>

          <button
            className={`sidebar-item ${
              isActive('/team-vault')
                ? 'active'
                : ''
            }`}
            onClick={() =>
              navigate('/team-vault')
            }
          >
            👥
            <span>Team Vault</span>
          </button>

          <button
            className={`sidebar-item ${
              isActive('/secure-notes')
                ? 'active'
                : ''
            }`}
            onClick={() =>
              navigate('/secure-notes')
            }
          >
            📝
            <span>Secure Notes</span>
          </button>

          <button
            className={`sidebar-item ${
              isActive('/password-generator')
                ? 'active'
                : ''
            }`}
            onClick={() =>
              navigate('/password-generator')
            }
          >
            🔑
            <span>Password Tools</span>
          </button>

          <button
            className={`sidebar-item ${
              isActive('/mfa')
                ? 'active'
                : ''
            }`}
            onClick={() =>
              navigate('/mfa')
            }
          >
            🛡️
            <span>MFA Security</span>
          </button>

          <button
            className={`sidebar-item ${
              isActive('/login-history')
                ? 'active'
                : ''
            }`}
            onClick={() =>
              navigate('/login-history')
            }
          >
            🕘
            <span>Login History</span>
          </button>

          <button
            className={`sidebar-item ${
              isActive('/security-analytics')
                ? 'active'
                : ''
            }`}
            onClick={() =>
              navigate('/security-analytics')
            }
          >
            📊
            <span>Security Analytics</span>
          </button>

          <button
            className={`sidebar-item ${
              isActive('/sessions')
                ? 'active'
                : ''
            }`}
            onClick={() =>
              navigate('/sessions')
            }
          >
            💻
            <span>Security Sessions</span>
          </button>

          {/* NOTIFICATIONS */}

          <button
            className={`sidebar-item ${
              isActive('/notifications')
                ? 'active'
                : ''
            }`}
            onClick={() =>
              navigate('/notifications')
            }
          >
            🔔
            <span>Notifications</span>
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
              SECURITY DASHBOARD
            </p>

            <h1>
              My Vault
            </h1>

            <p className="page-description">
              Securely manage your passwords and credentials.
            </p>

          </div>

          <div className="security-status">

            <span className="status-dot"></span>

            Vault Protected

          </div>

        </div>

        {/* MESSAGE */}

        {message && (
          <div className="vault-message">
            {message}
          </div>
        )}

        {/* STATS */}

        <div className="vault-stats">

          <div className="stat-card">

            <div className="stat-icon blue">
              🔐
            </div>

            <div>
              <span>Total Credentials</span>
              <strong>
                {credentials.length}
              </strong>
            </div>

          </div>

          <div className="stat-card">

            <div className="stat-icon purple">
              🤝
            </div>

            <div>
              <span>Shared With Me</span>
              <strong>
                {sharedCredentials.length}
              </strong>
            </div>

          </div>

          <div className="stat-card">

            <div className="stat-icon green">
              🛡️
            </div>

            <div>
              <span>Security</span>
              <strong>
                Protected
              </strong>
            </div>

          </div>

        </div>

        {/* ADD + SHARE */}

        <div className="vault-grid">

          {/* ADD CREDENTIAL */}

          <section className="dashboard-panel">

            <div className="panel-header">

              <div>

                <h2>
                  Add Credential
                </h2>

                <p>
                  Save a new login securely.
                </p>

              </div>

              <button
                type="button"
                className="panel-icon"
                onClick={() =>
                  setShowAddCredential(
                    !showAddCredential
                  )
                }
                aria-label={
                  showAddCredential
                    ? 'Hide add credential form'
                    : 'Show add credential form'
                }
              >
                {showAddCredential
                  ? '−'
                  : '+'}
              </button>

            </div>

            {showAddCredential && (

              <form
                className="vault-form"
                onSubmit={
                  handleAddCredential
                }
              >

                <label>
                  Title
                </label>

                <input
                  type="text"
                  placeholder="e.g. Google"
                  value={title}
                  onChange={(e) =>
                    setTitle(
                      e.target.value
                    )
                  }
                  required
                />

                <label>
                  Username
                </label>

                <input
                  type="text"
                  placeholder="Username or email"
                  value={username}
                  onChange={(e) =>
                    setUsername(
                      e.target.value
                    )
                  }
                  required
                />

                <label>
                  Password
                </label>

                <input
                  type="password"
                  placeholder="Enter password"
                  value={password}
                  onChange={(e) =>
                    checkPasswordStrength(
                      e.target.value
                    )
                  }
                  required
                />

                <label>
                  Category
                </label>
                <select
                  value={category}
                  onChange={(e) =>
                    setCategory(e.target.value)
                  }
                >
                  <option value="">Select category</option>
                  <option value="Personal">Personal</option>
                  <option value="Work">Work</option>
                  <option value="Finance">Finance</option>
                  <option value="Social">Social</option>
                  <option value="Shopping">Shopping</option>
                  <option value="Other">Other</option>
                </select>

                <label>
                  Credential Type
                </label>
                <select
                  value={credentialType}
                  onChange={(e) =>
                    setCredentialType(e.target.value)
                  }
                >
                  <option value="">Select type</option>
                  <option value="Login">Login</option>
                  <option value="Credit Card">Credit Card</option>
                  <option value="API Key">API Key</option>
                  <option value="Database">Database</option>
                  <option value="Other">Other</option>
                </select>

                <label className="favorite-checkbox">
                  <input
                    type="checkbox"
                    checked={favorite}
                    onChange={(e) =>
                      setFavorite(e.target.checked)
                    }
                  />
                  Favorite
                </label>

                {strength && (
                  <div
                    className={`strength strength-${strength.toLowerCase()}`}
                  >
                    Password strength: {strength}
                  </div>
                )}

                <button
                  type="button"
                  className="secondary-action"
                  onClick={
                    generatePassword
                  }
                >
                  ✨ Generate Strong Password
                </button>

                <button
                  type="submit"
                  className="primary-action"
                >
                  + Save Credential
                </button>

              </form>

            )}

          </section>

          {/* SHARE CREDENTIAL */}

          <section className="dashboard-panel">

            <div className="panel-header">

              <div>

                <h2>
                  Share Credential
                </h2>

                <p>
                  Give temporary secure access.
                </p>

              </div>

              <span className="panel-icon">
                ↗
              </span>

            </div>

            {credentials.filter(
              (credential) =>
                credential.password !==
                null
            ).length === 0 ? (

              <div className="empty-state">

                <div>
                  🔐
                </div>

                <h3>
                  No shareable credentials
                </h3>

                <p>
                  Add or create a credential
                  with an available password
                  before sharing.
                </p>

              </div>

            ) : (

              <form
                className="vault-form"
                onSubmit={handleShare}
              >

                <label>
                  Credential
                </label>

                <select
                  value={shareCredentialId}
                  onChange={(e) =>
                    setShareCredentialId(
                      e.target.value
                    )
                  }
                  required
                >

                  <option value="">
                    Select credential
                  </option>

                  {credentials
                    .filter(
                      (credential) =>
                        credential.password !==
                        null
                    )
                    .map(
                      (credential) => (
                        <option
                          key={
                            credential.id
                          }
                          value={
                            credential.id
                          }
                        >
                          {
                            credential.title
                          }
                        </option>
                      )
                    )}

                </select>

                <label>
                  Recipient Email
                </label>

                <input
                  type="email"
                  placeholder="recipient@example.com"
                  value={recipientEmail}
                  onChange={(e) =>
                    setRecipientEmail(
                      e.target.value
                    )
                  }
                  required
                />

                <label>
                  Permission
                </label>

                <select
                  value={permission}
                  onChange={(e) =>
                    setPermission(
                      e.target.value
                    )
                  }
                  required
                >

                  <option value="VIEW_ONLY">
                    View Only
                  </option>

                  <option value="EDIT">
                    Edit
                  </option>

                  <option value="FULL_MANAGEMENT">
                    Full Management
                  </option>

                </select>

                <label>
                  Expires At
                </label>

                <input
                  type="datetime-local"
                  value={expiresAt}
                  onChange={(e) =>
                    setExpiresAt(
                      e.target.value
                    )
                  }
                  required
                />

                <div className="share-info">
                  🔒 Access automatically
                  expires at the selected
                  time.
                </div>

                <button
                  type="submit"
                  className="primary-action"
                >
                  🤝 Share Securely
                </button>

              </form>

            )}

          </section>

        </div>

        {/* SAVED CREDENTIALS */}

        <section className="dashboard-panel credentials-panel">

          <div className="panel-header">

            <div>

              <h2>
                Saved Credentials
              </h2>

              <p>
                Your encrypted login credentials.
              </p>

            </div>

            <span className="credential-count">
              {credentials.length} saved
            </span>

          </div>

          <input
            type="text"
            className="credential-search"
            placeholder="🔍 Search credentials..."
            value={search}
            onChange={(e) => {
              const value = e.target.value
              setSearch(value)
              searchCredentials(value)
            }}
          />

          <div className="credential-filters">
            <select
              value={categoryFilter}
              onChange={(e) =>
                setCategoryFilter(e.target.value)
              }
            >
              <option value="">All Categories</option>
              <option value="Personal">Personal</option>
              <option value="Work">Work</option>
              <option value="Finance">Finance</option>
              <option value="Social">Social</option>
              <option value="Shopping">Shopping</option>
              <option value="Other">Other</option>
            </select>

            <select
              value={credentialTypeFilter}
              onChange={(e) =>
                setCredentialTypeFilter(e.target.value)
              }
            >
              <option value="">All Types</option>
              <option value="Login">Login</option>
              <option value="Credit Card">Credit Card</option>
              <option value="API Key">API Key</option>
              <option value="Database">Database</option>
              <option value="Other">Other</option>
            </select>

            <label className="favorite-filter">
              <input
                type="checkbox"
                checked={favoriteFilter}
                onChange={(e) =>
                  setFavoriteFilter(e.target.checked)
                }
              />
              Favorites only
            </label>
          </div>

          {credentials.length ===
          0 ? (

            <div className="empty-state">

              <div>
                🔐
              </div>

              <h3>
                {credentials.length === 0
                  ? 'No credentials yet'
                  : 'No matching credentials'}
              </h3>

              <p>
                {credentials.length === 0
                  ? 'Add your first credential above.'
                  : 'Try searching with a different title or username.'}
              </p>

            </div>

          ) : (

            <div className="credential-list">

              {credentials
                .filter(
                  (credential) =>
                    (!categoryFilter ||
                      credential.category === categoryFilter) &&
                    (!credentialTypeFilter ||
                      credential.credentialType === credentialTypeFilter) &&
                    (!favoriteFilter ||
                      credential.favorite === true)
                )
                .map(
                  (credential) => (

                  <div
                    className="credential-row"
                    key={credential.id}
                  >

                    <div className="credential-logo">
                      🔑
                    </div>

                    <div className="credential-info">

                      <h3>
                        {credential.title}
                      </h3>

                      <p>
                        {credential.username}
                      </p>

                    </div>

                    <div className="credential-meta">
                      {credential.category && (
                        <span className="credential-tag">
                          {credential.category}
                        </span>
                      )}
                      {credential.credentialType && (
                        <span className="credential-tag">
                          {credential.credentialType}
                        </span>
                      )}
                      {credential.favorite && (
                        <span className="credential-favorite">
                          ★ Favorite
                        </span>
                      )}
                    </div>

                    <div className="credential-password">

                      {credential.password ===
                      null ? (

                        <span>
                          Password unavailable
                        </span>

                      ) : (

                        <>

                          <span>
                            {showPassword[
                              credential.id
                            ]
                              ? credential.password
                              : '••••••••••••'}
                          </span>

                          <button
                            type="button"
                            className="password-toggle"
                            onClick={() =>
                              togglePassword(
                                credential.id
                              )
                            }
                          >
                            {showPassword[
                              credential.id
                            ]
                              ? 'Hide'
                              : 'Show'}
                          </button>

                          <button
                            type="button"
                            className="password-toggle"
                            onClick={() =>
                              copyPassword(
                                credential.password
                              )
                            }
                          >
                            Copy
                          </button>

                          <button
                            type="button"
                            className="password-toggle"
                            onClick={() =>
                              openEditCredential(
                                credential
                              )
                            }
                          >
                            Edit
                          </button>

                        </>

                      )}

                      <button
                        type="button"
                        className="password-toggle"
                        onClick={() =>
                          deleteCredential(
                            credential.id
                          )
                        }
                      >
                        Delete
                      </button>

                    </div>

                  </div>
                )
              )}

            </div>

          )}

        </section>

        {/* SHARED WITH ME */}

        <section className="dashboard-panel credentials-panel">

          <div className="panel-header">

            <div>

              <h2>
                Shared With Me
              </h2>

              <p>
                Credentials temporarily shared
                with your account.
              </p>

            </div>

            <span className="credential-count">
              {sharedCredentials.length}
              {' '}
              shared
            </span>

          </div>

          {sharedCredentials.length ===
          0 ? (

            <div className="empty-state">

              <div>
                🤝
              </div>

              <h3>
                No shared credentials
              </h3>

              <p>
                Credentials shared with you
                will appear here.
              </p>

            </div>

          ) : (

            <div className="credential-list">

              {sharedCredentials.map(
                (share) => (

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
                        {share.permission ||
                          'Not specified'}
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
                        share.active
                          ? 'active-badge'
                          : 'expired-badge'
                      }
                    >
                      {share.active
                        ? 'Active'
                        : 'Expired'}
                    </span>

                  </div>
                )
              )}

            </div>

          )}

        </section>

      </main>

      {/* EDIT CREDENTIAL MODAL */}

      {editingCredential && (

        <div className="edit-modal-overlay">

          <div className="edit-modal">

            <div className="edit-modal-header">

              <div>

                <h2>
                  Edit Credential
                </h2>

                <p>
                  Update your saved login details.
                </p>

              </div>

              <button
                type="button"
                className="edit-close-button"
                onClick={cancelEdit}
              >
                ×
              </button>

            </div>

            <form
              className="vault-form"
              onSubmit={
                handleUpdateCredential
              }
            >

              <label>
                Title
              </label>

              <input
                type="text"
                value={editTitle}
                onChange={(e) =>
                  setEditTitle(
                    e.target.value
                  )
                }
                required
              />

              <label>
                Username
              </label>

              <input
                type="text"
                value={editUsername}
                onChange={(e) =>
                  setEditUsername(
                    e.target.value
                  )
                }
                required
              />

              <label>
                Password
              </label>

              <div className="edit-password-wrapper">

                <input
                  type={
                    showEditPassword
                      ? 'text'
                      : 'password'
                  }
                  value={editPassword}
                  onChange={(e) =>
                    checkEditPasswordStrength(
                      e.target.value
                    )
                  }
                  required
                />

                <button
                  type="button"
                  className="edit-password-toggle"
                  onClick={() =>
                    setShowEditPassword(
                      !showEditPassword
                    )
                  }
                >
                  {showEditPassword
                    ? 'Hide'
                    : 'Show'}
                </button>

              </div>

              {editStrength && (
                <div
                  className={`strength strength-${editStrength.toLowerCase()}`}
                >
                  Password strength: {editStrength}
                </div>
              )}

              <label>
                Category
              </label>
              <select
                value={editCategory}
                onChange={(e) =>
                  setEditCategory(e.target.value)
                }
              >
                <option value="">Select category</option>
                <option value="Personal">Personal</option>
                <option value="Work">Work</option>
                <option value="Finance">Finance</option>
                <option value="Social">Social</option>
                <option value="Shopping">Shopping</option>
                <option value="Other">Other</option>
              </select>

              <label>
                Credential Type
              </label>
              <select
                value={editCredentialType}
                onChange={(e) =>
                  setEditCredentialType(e.target.value)
                }
              >
                <option value="">Select type</option>
                <option value="Login">Login</option>
                <option value="Credit Card">Credit Card</option>
                <option value="API Key">API Key</option>
                <option value="Database">Database</option>
                <option value="Other">Other</option>
              </select>

              <label className="favorite-checkbox">
                <input
                  type="checkbox"
                  checked={editFavorite}
                  onChange={(e) =>
                    setEditFavorite(e.target.checked)
                  }
                />
                Favorite
              </label>

              <div className="edit-modal-actions">

                <button
                  type="button"
                  className="secondary-action"
                  onClick={cancelEdit}
                >
                  Cancel
                </button>

                <button
                  type="submit"
                  className="primary-action"
                >
                  Save Changes
                </button>

              </div>

            </form>

          </div>

        </div>

      )}

    </div>
  )
}

export default Vault

