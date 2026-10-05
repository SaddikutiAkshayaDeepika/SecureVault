import { useEffect, useState } from 'react'
import API_URL from '../api'

function SecurityAnalytics() {
  const [analytics, setAnalytics] = useState(null)
  const [anomalies, setAnomalies] = useState([])
  const [message, setMessage] = useState('')

  // =========================
  // FETCH SECURITY ANALYTICS
  // =========================

  const fetchAnalytics = async () => {
    const token = localStorage.getItem('token')

    if (!token) {
      setMessage('Please login first')
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/security/analytics`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.json()

      console.log(
        'Security Analytics:',
        response.status,
        result
      )

      if (!response.ok) {
        setMessage(
          result.message ||
          `Failed to load analytics: ${response.status}`
        )
        return
      }

      setAnalytics(result)
    } catch (error) {
      console.error(error)
      setMessage('Cannot connect to backend')
    }
  }

  // =========================
  // FETCH LOGIN ANOMALIES
  // =========================

  const fetchAnomalies = async () => {
    const token = localStorage.getItem('token')

    if (!token) {
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/security/anomalies`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const result = await response.json()

      console.log(
        'Login Anomalies:',
        response.status,
        result
      )

      if (!response.ok) {
        return
      }

      setAnomalies(result)
    } catch (error) {
      console.error(
        'Failed to fetch anomalies:',
        error
      )
    }
  }

  // =========================
  // EXPORT SECURITY EVENTS
  // =========================

  const exportSecurityEvents = async () => {
    const token = localStorage.getItem('token')

    if (!token) {
      setMessage('Please login first')
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/security/export`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      if (!response.ok) {
        setMessage(
          `Failed to export security events: ${response.status}`
        )
        return
      }

      const blob = await response.blob()

      const url = window.URL.createObjectURL(blob)

      const link = document.createElement('a')

      link.href = url

      link.download = 'securevault-security-events.csv'

      document.body.appendChild(link)

      link.click()

      link.remove()

      window.URL.revokeObjectURL(url)
    } catch (error) {
      console.error(
        'Failed to export security events:',
        error
      )

      setMessage('Cannot connect to backend')
    }
  }

  // =========================
  // LOAD DATA
  // =========================

  useEffect(() => {
    fetchAnalytics()
    fetchAnomalies()
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
  // GET RISK LEVEL
  // =========================

  const getRiskLevel = (status) => {
    if (status === 'ANOMALY_HIGH') {
      return 'HIGH'
    }

    if (status === 'ANOMALY_MEDIUM') {
      return 'MEDIUM'
    }

    return 'LOW'
  }

  // =========================
  // GET RISK MESSAGE
  // =========================

  const getRiskMessage = (status) => {
    if (status === 'ANOMALY_HIGH') {
      return 'High-risk login detected'
    }

    if (status === 'ANOMALY_MEDIUM') {
      return 'Unusual login detected'
    }

    return 'Low-risk anomaly detected'
  }

  return (
    <section className="dashboard-panel">

      {/* =========================
          HEADER
      ========================= */}

      <div className="panel-header">

        <div>

          <h2>📊 Security Analytics</h2>

          <p>
            Monitor your SecureVault security activity.
          </p>

        </div>

        <button
          type="button"
          onClick={exportSecurityEvents}
          className="primary-button"
        >
          Export Security Events
        </button>

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
          ANALYTICS
      ========================= */}

      {!analytics && !message && (
        <p>Loading analytics...</p>
      )}

      {analytics && (

        <div
          style={{
            display: 'grid',
            gridTemplateColumns:
              'repeat(auto-fit, minmax(200px, 1fr))',
            gap: '20px',
            marginTop: '20px',
          }}
        >

          <div className="stat-card">
            <h3>Successful Logins</h3>
            <p>{analytics.successfulLogins}</p>
          </div>

          <div className="stat-card">
            <h3>Failed Logins</h3>
            <p>{analytics.failedLogins}</p>
          </div>

          <div className="stat-card">
            <h3>Credentials Added</h3>
            <p>{analytics.credentialsAdded}</p>
          </div>

          <div className="stat-card">
            <h3>Credentials Updated</h3>
            <p>{analytics.credentialsUpdated}</p>
          </div>

          <div className="stat-card">
            <h3>Credentials Deleted</h3>
            <p>{analytics.credentialsDeleted}</p>
          </div>

          <div className="stat-card">
            <h3>Total Security Events</h3>
            <p>{analytics.totalEvents}</p>
          </div>

        </div>
      )}

      {/* =========================
          LOGIN ANOMALIES
      ========================= */}

      <div
        style={{
          marginTop: '40px',
        }}
      >

        <h2>🚨 Login Anomalies</h2>

        <p
          style={{
            color: '#666',
            marginTop: '5px',
          }}
        >
          Detect unusual login activity and security risks.
        </p>

        {anomalies.length === 0 && (

          <div
            style={{
              marginTop: '20px',
              padding: '20px',
              borderRadius: '10px',
              background: '#f5f5f5',
            }}
          >
            <strong>No login anomalies detected.</strong>
          </div>

        )}

        {anomalies.length > 0 && (

          <div
            style={{
              display: 'flex',
              flexDirection: 'column',
              gap: '15px',
              marginTop: '20px',
            }}
          >

            {anomalies.map((anomaly) => {

              const riskLevel =
                getRiskLevel(anomaly.status)

              return (

                <div
                  key={anomaly.id}
                  style={{
                    border: '1px solid #ddd',
                    borderRadius: '10px',
                    padding: '18px',
                    background: '#fff',
                    boxShadow:
                      '0 2px 8px rgba(0,0,0,0.06)',
                  }}
                >

                  <div
                    style={{
                      display: 'flex',
                      justifyContent: 'space-between',
                      alignItems: 'center',
                      flexWrap: 'wrap',
                      gap: '10px',
                    }}
                  >

                    <div>

                      <h3
                        style={{
                          margin: '0 0 6px 0',
                        }}
                      >
                        {getRiskMessage(anomaly.status)}
                      </h3>

                      <p
                        style={{
                          margin: 0,
                          color: '#666',
                        }}
                      >
                        Risk level: {riskLevel}
                      </p>

                    </div>

                    <span
                      style={{
                        padding: '6px 12px',
                        borderRadius: '20px',
                        fontSize: '12px',
                        fontWeight: '600',
                        background:
                          riskLevel === 'HIGH'
                            ? '#ffe5e5'
                            : riskLevel === 'MEDIUM'
                              ? '#fff3cd'
                              : '#e7f5e7',
                        color:
                          riskLevel === 'HIGH'
                            ? '#c62828'
                            : riskLevel === 'MEDIUM'
                              ? '#856404'
                              : '#2e7d32',
                      }}
                    >
                      {riskLevel}
                    </span>

                  </div>

                  <div
                    style={{
                      marginTop: '15px',
                      fontSize: '14px',
                      lineHeight: '1.8',
                    }}
                  >

                    <div>
                      <strong>Device:</strong>{' '}
                      {anomaly.device || 'Unknown'}
                    </div>

                    <div>
                      <strong>IP Address:</strong>{' '}
                      {anomaly.ipAddress || 'Unknown'}
                    </div>

                    <div>
                      <strong>Date & Time:</strong>{' '}
                      {formatDate(anomaly.dateTime)}
                    </div>

                  </div>

                </div>

              )

            })}

          </div>

        )}

      </div>

    </section>
  )
}

export default SecurityAnalytics