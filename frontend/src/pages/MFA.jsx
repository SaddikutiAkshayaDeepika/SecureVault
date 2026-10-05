import { useEffect, useState } from 'react'
import { QRCodeSVG } from 'qrcode.react'
import API_URL from '../api'

function MFA() {
  const [enabled, setEnabled] = useState(false)
  const [qrCodeUrl, setQrCodeUrl] = useState('')
  const [code, setCode] = useState('')
  const [message, setMessage] = useState('')

  const getToken = () => localStorage.getItem('token')

  const fetchStatus = async () => {
    const token = getToken()

    if (!token) {
      setMessage('Please login again')
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/mfa/status`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const text = await response.text()

      if (!response.ok) {
        console.log('MFA status error:', response.status, text)
        setMessage(`MFA status failed: ${response.status}`)
        return
      }

      const result = text ? JSON.parse(text) : {}
      setEnabled(result.enabled === true)
    } catch (error) {
      console.error('MFA status error:', error)
      setMessage('Cannot connect to backend')
    }
  }

  const enableMfa = async () => {
    const token = getToken()

    if (!token) {
      setMessage('Please login again')
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/mfa/enable`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const text = await response.text()

      console.log('Enable MFA:', response.status, text)

      if (!response.ok) {
        setMessage(
          text || `Failed to enable MFA (${response.status})`
        )
        return
      }

      const result = text ? JSON.parse(text) : {}

      setEnabled(result.enabled === true)
      setMessage('MFA enabled successfully')

      fetchQrCode()
    } catch (error) {
      console.error('Enable MFA error:', error)
      setMessage('Cannot connect to backend')
    }
  }

  const fetchQrCode = async () => {
    const token = getToken()

    if (!token) {
      setMessage('Please login again')
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/mfa/qr`,
        {
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const text = await response.text()

      console.log('QR response:', response.status, text)

      if (!response.ok) {
        setMessage(
          text || `Failed to load QR code (${response.status})`
        )
        return
      }

      const result = text ? JSON.parse(text) : {}

      setQrCodeUrl(result.qrCodeUrl || '')
    } catch (error) {
      console.error('QR code error:', error)
      setMessage('Cannot connect to backend')
    }
  }

  const verifyMfa = async () => {
    const token = getToken()

    if (!token) {
      setMessage('Please login again')
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/mfa/verify?code=${code}`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const text = await response.text()

      console.log('Verify MFA:', response.status, text)

      if (response.ok) {
        setMessage('MFA verification successful')
        setCode('')
      } else {
        setMessage(text || `Invalid MFA code (${response.status})`)
      }
    } catch (error) {
      console.error('Verify MFA error:', error)
      setMessage('Cannot connect to backend')
    }
  }

  const disableMfa = async () => {
    const token = getToken()

    if (!token) {
      setMessage('Please login again')
      return
    }

    try {
      const response = await fetch(
        `${API_URL}/api/mfa/disable`,
        {
          method: 'POST',
          headers: {
            Authorization: `Bearer ${token}`,
          },
        }
      )

      const text = await response.text()

      console.log('Disable MFA:', response.status, text)

      if (response.ok) {
        setEnabled(false)
        setQrCodeUrl('')
        setMessage('MFA disabled successfully')
      } else {
        setMessage(
          text || `Failed to disable MFA (${response.status})`
        )
      }
    } catch (error) {
      console.error('Disable MFA error:', error)
      setMessage('Cannot connect to backend')
    }
  }

  useEffect(() => {
    fetchStatus()
  }, [])

  return (
    <section className="dashboard-panel">
      <div className="panel-header">
        <div>
          <h2>🔐 Multi-Factor Authentication</h2>
          <p>
            Add an extra layer of security to your SecureVault account.
          </p>
        </div>
      </div>

      {message && (
        <div className="vault-message">
          {message}
        </div>
      )}

      <p>
        Status:{' '}
        <strong>
          {enabled ? 'Enabled' : 'Disabled'}
        </strong>
      </p>

      {!enabled ? (
        <button
          className="primary-action"
          onClick={enableMfa}
        >
          Enable MFA
        </button>
      ) : (
        <>
          {!qrCodeUrl && (
            <button
              className="secondary-action"
              onClick={fetchQrCode}
            >
              Show QR Code
            </button>
          )}

          {qrCodeUrl && (
            <div style={{ marginTop: '20px' }}>
              <p>
                Scan this QR code with your authenticator app:
              </p>

              <QRCodeSVG
                value={qrCodeUrl}
                size={220}
              />
            </div>
          )}

          <div style={{ marginTop: '20px' }}>
            <input
              type="text"
              inputMode="numeric"
              maxLength="6"
              placeholder="Enter 6-digit code"
              value={code}
              onChange={(e) =>
                setCode(
                  e.target.value.replace(/\D/g, '')
                )
              }
            />

            <button
              className="primary-action"
              onClick={verifyMfa}
              disabled={code.length !== 6}
            >
              Verify Code
            </button>
          </div>

          <button
            className="secondary-action"
            onClick={disableMfa}
            style={{ marginTop: '15px' }}
          >
            Disable MFA
          </button>
        </>
      )}
    </section>
  )
}

export default MFA