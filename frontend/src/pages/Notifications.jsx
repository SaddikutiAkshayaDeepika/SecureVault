
import { useEffect, useState } from 'react'
import API_URL from '../api'

function Notifications() {

  const [notifications, setNotifications] = useState([])
  const [loading, setLoading] = useState(true)
  const [message, setMessage] = useState('')

  // =========================
  // FETCH NOTIFICATIONS
  // =========================

  const fetchNotifications = async () => {

    const token = localStorage.getItem('token')

    if (!token) {
      setMessage('Please login first')
      setLoading(false)
      return
    }

    try {

      const response = await fetch(
        `${API_URL}/api/notifications`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.json()

      console.log(
        'Notifications:',
        response.status,
        result
      )

      if (!response.ok) {
        setMessage(
          result.message ||
          `Failed to load notifications: ${response.status}`
        )
        setLoading(false)
        return
      }

      setNotifications(result)

    } catch (error) {

      console.error(error)

      setMessage('Cannot connect to backend')

    } finally {

      setLoading(false)
    }
  }

  // =========================
  // MARK ONE AS READ
  // =========================

  const markAsRead = async (id) => {

    const token = localStorage.getItem('token')

    try {

      const response = await fetch(
        `${API_URL}/api/notifications/${id}/read`,
        {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      if (!response.ok) {
        return
      }

      setNotifications((current) =>
        current.map((notification) =>
          notification.id === id
            ? { ...notification, read: true }
            : notification
        )
      )

    } catch (error) {

      console.error(error)
    }
  }

  // =========================
  // MARK ALL AS READ
  // =========================

  const markAllAsRead = async () => {

    const token = localStorage.getItem('token')

    try {

      const response = await fetch(
        `${API_URL}/api/notifications/read-all`,
        {
          method: 'PUT',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      if (!response.ok) {
        return
      }

      setNotifications((current) =>
        current.map((notification) => ({
          ...notification,
          read: true,
        }))
      )

    } catch (error) {

      console.error(error)
    }
  }

  // =========================
  // LOAD
  // =========================

  useEffect(() => {
    fetchNotifications()
  }, [])

  // =========================
  // FORMAT DATE
  // =========================

  const formatDate = (dateTime) => {

    if (!dateTime) {
      return 'Unknown'
    }

    return new Date(dateTime).toLocaleString()
  }

  // =========================
  // UNREAD COUNT
  // =========================

  const unreadCount =
    notifications.filter(
      (notification) => !notification.read
    ).length

  // =========================
  // UI
  // =========================

  return (

    <section className="dashboard-panel">

      <div
        className="panel-header"
        style={{
          display: 'flex',
          justifyContent: 'space-between',
          alignItems: 'center',
          gap: '15px',
          flexWrap: 'wrap',
        }}
      >

        <div>

          <h2>🔔 Notifications</h2>

          <p>
            View your SecureVault security alerts and notifications.
          </p>

        </div>

        {unreadCount > 0 && (

          <button
            onClick={markAllAsRead}
            style={{
              padding: '10px 16px',
              border: 'none',
              borderRadius: '8px',
              cursor: 'pointer',
              fontWeight: '600',
            }}
          >
            Mark All as Read
          </button>

        )}

      </div>

      {/* =========================
          MESSAGE
      ========================= */}

      {message && (

        <div className="vault-message">
          {message}
        </div>

      )}

      {/* =========================
          LOADING
      ========================= */}

      {loading && (
        <p>Loading notifications...</p>
      )}

      {/* =========================
          EMPTY
      ========================= */}

      {!loading &&
        !message &&
        notifications.length === 0 && (

          <div
            style={{
              marginTop: '25px',
              padding: '25px',
              borderRadius: '10px',
              background: '#f5f5f5',
              textAlign: 'center',
            }}
          >
            <h3>✅ No notifications</h3>

            <p>
              You don't have any security notifications.
            </p>
          </div>

        )}

      {/* =========================
          NOTIFICATIONS
      ========================= */}

      {!loading &&
        notifications.length > 0 && (

          <div
            style={{
              marginTop: '25px',
              display: 'flex',
              flexDirection: 'column',
              gap: '15px',
            }}
          >

            {notifications.map((notification) => (

              <div
                key={notification.id}
                style={{
                  padding: '18px',
                  borderRadius: '10px',
                  border: notification.read
                    ? '1px solid #ddd'
                    : '2px solid #ddd',
                  background: notification.read
                    ? '#fff'
                    : '#f8fbff',
                  boxShadow:
                    '0 2px 8px rgba(0,0,0,0.05)',
                }}
              >

                <div
                  style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    alignItems: 'flex-start',
                    gap: '15px',
                    flexWrap: 'wrap',
                  }}
                >

                  <div>

                    <h3
                      style={{
                        margin: '0 0 6px 0',
                      }}
                    >
                      {notification.title}
                    </h3>

                    <p
                      style={{
                        margin: 0,
                        color: '#555',
                      }}
                    >
                      {notification.message}
                    </p>

                  </div>

                  <span
                    style={{
                      padding: '6px 10px',
                      borderRadius: '20px',
                      fontSize: '12px',
                      fontWeight: '600',
                      background:
                        notification.severity === 'HIGH'
                          ? '#ffe5e5'
                          : notification.severity === 'MEDIUM'
                            ? '#fff3cd'
                            : '#e7f5e7',
                      color:
                        notification.severity === 'HIGH'
                          ? '#c62828'
                          : notification.severity === 'MEDIUM'
                            ? '#856404'
                            : '#2e7d32',
                    }}
                  >
                    {notification.severity}
                  </span>

                </div>

                <div
                  style={{
                    marginTop: '15px',
                    fontSize: '13px',
                    color: '#777',
                  }}
                >

                  {notification.type}

                  {' • '}

                  {formatDate(notification.createdAt)}

                </div>

                {!notification.read && (

                  <button
                    onClick={() =>
                      markAsRead(notification.id)
                    }
                    style={{
                      marginTop: '15px',
                      padding: '8px 12px',
                      border: 'none',
                      borderRadius: '7px',
                      cursor: 'pointer',
                      fontWeight: '600',
                    }}
                  >
                    Mark as Read
                  </button>

                )}

                {notification.read && (

                  <div
                    style={{
                      marginTop: '12px',
                      fontSize: '12px',
                      color: '#2e7d32',
                      fontWeight: '600',
                    }}
                  >
                    ✓ Read
                  </div>

                )}

              </div>

            ))}

          </div>

        )}

    </section>
  )
}

export default Notifications

