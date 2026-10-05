
import { useEffect, useState } from 'react'
import API_URL from './api'

function LoginHistory() {

  const [history, setHistory] = useState([])
  const [message, setMessage] = useState('')

  const fetchLoginHistory = async () => {

    const token = localStorage.getItem('token')

    if (!token) {
      setMessage('Please login first')
      return
    }

    try {

      const response = await fetch(
        `${API_URL}/api/security/login-history`,
        {
          method: 'GET',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.text()

      console.log(
        'Login history:',
        response.status,
        result
      )

      if (response.ok) {

        setHistory(JSON.parse(result))

      } else {

        setMessage(
          `Failed to load login history: ${response.status}`
        )
      }

    } catch (error) {

      console.error(error)

      setMessage('Cannot connect to backend')
    }
  }

  useEffect(() => {
    fetchLoginHistory()
  }, [])

  return (

    <div style={{ padding: '30px' }}>

      <h1>🔐 SecureVault</h1>

      <h2>Login History</h2>

      {message && (
        <p>{message}</p>
      )}

      {history.length === 0 && !message ? (

        <p>No login history found.</p>

      ) : (

        history.map((event) => (

          <div
            key={event.id}
            style={{
              border: '1px solid #ccc',
              padding: '15px',
              marginBottom: '15px',
              borderRadius: '8px',
            }}
          >

            <p>
              <strong>Email:</strong>{' '}
              {event.email}
            </p>

            <p>
              <strong>Date & Time:</strong>{' '}
              {event.dateTime}
            </p>

            <p>
              <strong>Status:</strong>{' '}
              {event.status}
            </p>

            <p>
              <strong>IP Address:</strong>{' '}
              {event.ipAddress}
            </p>

            <p>
              <strong>Device:</strong>{' '}
              {event.device}
            </p>

          </div>

        ))
      )}

    </div>
  )
}

export default LoginHistory

